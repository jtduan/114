package code.jtduan.okhttp;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static code.jtduan.Config.confirmUrl;
import static code.jtduan.Config.detailUrl;
import static code.jtduan.Config.doctorId;
import static code.jtduan.Config.dutySourceId;
import static code.jtduan.Config.getDoctorURL;
import static code.jtduan.Config.loginURL;
import static code.jtduan.Config.verifyCodeURL;

import code.jtduan.Config;
import okhttp3.Interceptor;
import okhttp3.JavaNetCookieJar;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * @author jtduan
 * @date 2016/12/8
 */
@Service
public class OService {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private OkHttpClient client;

    private final Config config;

    @Autowired
    public OService(Config config) {
        this.config = config;
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        client = new OkHttpClient().newBuilder()
//                .proxy(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", 1080)))
                .cookieJar(new JavaNetCookieJar(cookieManager))
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request request = chain.request()
                                .newBuilder()
                                .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                                .addHeader("Host", "www.bjguahao.gov.cn")
                                .addHeader("Origin", "http://www.bjguahao.gov.cn")
                                .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36")
                                .addHeader("Accept", "*/*")
                                .addHeader("X-Requested-With", "XMLHttpRequest")
                                .build();
                        return chain.proceed(request);
                    }
                })
                .build();
    }

    private boolean doLogin() {
        String params = "mobileNo=" + config.userName + "&password=" + config.userPwd + "&yzm=&isAjax=true";
        String res = sendPost(loginURL, params, null);
        logger.info("login result：{}", res);
        return res.matches(".*\"code\":200\\b.*");
    }

    private boolean loadDoctor() {
        String params = "hospitalId=" + config.hospitalId + "&departmentId=" + config.departmentId + "&dutyCode=" + config.duty + "&dutyDate=" + config.date + "&isAjax=true";
        String res = sendPost(getDoctorURL, params, null);
        return !res.isEmpty() && buildOrderUrl(res);
    }

    private boolean buildOrderUrl(String response) {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node;
        try {
            node = objectMapper.readTree(response);
        } catch (IOException e) {
            return false;
        }
        JsonNode data = node.get("data");
        if (data == null) {
            return false;
        }
        for (int i = data.size() - 1; i >= 0; i--) {
            if (data.get(i).get("remainAvailableNumber").asInt() > 0) {
                doctorId = data.get(i).get("doctorId").asText();
                dutySourceId = data.get(i).get("dutySourceId").asText();
                return true;
            }
        }
        return false;
    }

    public void run() {
        if (!doLogin()) return;

        System.out.print("running...");
        while (true) {
            if (loadDoctor()) {
                break;
            }
            System.out.print(".");
        }
        doPreOrder();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (StringUtils.isEmpty(config.verifyCode)) {
            if (!doSendVerfiCode()) return;
            System.out.println("cin the verficode:");
            Scanner scanner = new Scanner(System.in);
            config.verifyCode = scanner.nextLine();
        }
        doOrder();
    }

    private void doPreOrder() {
        String url = detailUrl.replace("${hospitalId}", config.hospitalId)
                .replace("${departmentId}", config.departmentId)
                .replace("${doctorId}", doctorId)
                .replace("${dutySourceId}", dutySourceId);
        String res = sendGet(url);
        System.out.println("doPreOrder finish!");
    }

    private void doOrder() {
        Map<String, String> headers = new TreeMap<>();
        String url = detailUrl.replace("${hospitalId}", config.hospitalId)
                .replace("${departmentId}", config.departmentId)
                .replace("${doctorId}", doctorId)
                .replace("${dutySourceId}", dutySourceId);
        headers.put("Referer", url);

        String[] params = new String[11];
        params[0] = "dutySourceId=" + dutySourceId;
        params[1] = "hospitalId=" + config.hospitalId;
        params[2] = "departmentId=" + config.departmentId;
        params[3] = "doctorId=" + doctorId;
        params[4] = "patientId=" + config.patientId;
        params[5] = "hospitalCardId=";
        params[6] = "medicareCardId=";
        params[7] = "reimbursementType=-1";
        params[8] = "smsVerifyCode=" + config.verifyCode;
        params[9] = "childrenBirthday=";
        params[10] = "isAjax=true";
        String res = sendPost(confirmUrl, String.join("&", params), headers);
        logger.info("order result：{}", res);
    }

    private boolean doSendVerfiCode() {
        Map<String, String> headers = new TreeMap<>();
        String url = detailUrl.replace("${hospitalId}", config.hospitalId)
                .replace("${departmentId}", config.departmentId)
                .replace("${doctorId}", doctorId)
                .replace("${dutySourceId}", dutySourceId);
        headers.put("Referer", url);

        String res = sendPost(verifyCodeURL, "", headers);
        logger.info("verficode send result：{}", res);
        return res.matches(".*\"code\":200\\b.*");
    }

    private String sendPost(String url, String params, Map<String, String> headers) {
        RequestBody body = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), params);
        Request.Builder builder = new Request.Builder()
                .url(url).post(body);
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        Request request = builder.build();

        try {
            Response response = client.newCall(request).execute();
            return response.body().string();
        } catch (IOException e) {
            logger.warn(e.getMessage());
            return "";
        }
    }

    private String sendGet(String url) {
        Request.Builder builder = new Request.Builder()
                .url(url).get();
        Request request = builder.build();

        try {
            Response response = client.newCall(request).execute();
            return response.body().string();
        } catch (IOException e) {
            logger.warn(e.getMessage());
            return "";
        }
    }
}