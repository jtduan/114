package code.jtduan.okhttp;

import code.jtduan.Config;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import static code.jtduan.Config.*;

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
        MyCookieJar cookieJar = new MyCookieJar();
        client = new OkHttpClient().newBuilder().cookieJar(cookieJar).build();
    }

    private boolean doLogin() {
        String params = "mobileNo=" + config.userName + "&password=" + config.userPwd + "&yzm=&isAjax=true";
        String res = send(loginURL, params);
        logger.info("login result：{}", res);
        return res.matches(".*\"code\":200\\b.*");
    }

    private boolean loadDoctor() {
        String params = "hospitalId=" + config.hospitalId + "&departmentId=" + config.departmentId + "&dutyCode=" + config.duty + "&dutyDate=" + config.date + "&isAjax=true";
        String res = send(getDoctorURL, params);
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
        if (config.verifyCode == 0) {
            if (!doSendVerfiCode()) return;
            System.out.println("cin the verficode:");
            Scanner scanner = new Scanner(System.in);
            config.verifyCode = scanner.nextInt();
        }
        System.out.print("running...");
        while (true) {
            if (loadDoctor()) {
                break;
            }
            System.out.print(".");
        }
        doPreOrder();
    }

    private void doPreOrder() {
        String[] params = new String[15];
        params[0] = "dutySourceId=" + dutySourceId;
        params[1] = "hospitalId=" + config.hospitalId;
        params[2] = "departmentId=" + config.departmentId;
        params[3] = "doctorId=" + doctorId;
        params[4] = "patientId=" + config.patientId;
        params[5] = "hospitalCardId=";
        params[6] = "medicareCardId=";
        params[7] = "reimbursementType=-1";
        params[8] = "smsVerifyCode=" + config.verifyCode;
        params[9] = "isFirstTime=2";
        params[10] = "hasPowerHospitalCard=2";
        params[11] = "cidType=1";
        params[12] = "childrenBirthday=";
        params[13] = "childrenGender=2";
        params[14] = "isAjax=true";
        String res = send(confirmUrl, String.join("&", params));
        logger.info("order result：{}", res);
    }

    private boolean doSendVerfiCode() {
        String res = send(verifyCodeURL, "");
        logger.info("verficode send result：{}", res);
        return res.matches(".*\"code\":200\\b.*");
    }

    private String send(String url, String params) {
        RequestBody body = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), params);
        Request request = new Request.Builder()
                .url(url).post(body)
                .build();

        try {
            Response response = client.newCall(request).execute();
            return response.body().string();
        } catch (IOException e) {
            logger.warn(e.getMessage());
            return "";
        }
    }


    private class MyCookieJar implements CookieJar {

        private List<Cookie> cookies;

        @Override
        public void saveFromResponse(HttpUrl httpUrl, List<Cookie> list) {
            cookies = list;
        }

        /**
         * 该函数不能返回null(否则会报异常)
         *
         * @param httpUrl
         * @return
         */
        @Override
        public List<Cookie> loadForRequest(HttpUrl httpUrl) {
            if (cookies == null) return Collections.emptyList();
            return cookies;
        }
    }
}