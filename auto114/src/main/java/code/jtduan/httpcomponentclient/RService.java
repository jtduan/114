package code.jtduan.httpcomponentclient;

import code.jtduan.Config;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Scanner;

import static code.jtduan.Config.*;

/**
 * 使用须知：
 * 1. 需要首先创建病人，并获取patientId(通过查看源代码：<input type="radio" name="hzr" value="***" checked="checked"> )
 * value即为patinentId
 * 2 存在多个就诊人时，第一个为待预约就诊人
 * 3 需要找到待预约的医院Id和科室Id(通过进入医院详情页面查看源代码：<a class="kfyuks_islogin" href="/dpt/appoint/142-200039598.htm">眼科门诊</a>)
 * 142-200039598 即为医院Id-科室Id
 * <p>
 * 4 开始放号前2分钟启动，即可
 */

@Service
public class RService {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    static CookieStore cookieStore = new BasicCookieStore();

    @Autowired
    private Config config;

    private HttpClientContext localContext;
    private CloseableHttpClient httpclient;

    private void init() {

        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setCookieSpec(CookieSpecs.DEFAULT)
                .setExpectContinueEnabled(true)
                .setSocketTimeout(3000)
                .setConnectTimeout(1000)
                .setConnectionRequestTimeout(1000)
                .build();
        httpclient = HttpClients.createDefault();
        localContext = HttpClientContext.create();
        localContext.setRequestConfig(defaultRequestConfig);
        localContext.setCookieStore(cookieStore);
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
        init();
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
        HttpPost post = new HttpPost(url);
        StringEntity stringEntity = new StringEntity(params, "UTF-8");
        stringEntity.setContentType("application/x-www-form-urlencoded");
        post.setEntity(stringEntity);

        try (CloseableHttpResponse response = httpclient.execute(post, localContext)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            EntityUtils.consume(response.getEntity());
            return responseBody;
        } catch (Exception e) {
            logger.warn(e.getMessage());
            return "";
        }
    }
}