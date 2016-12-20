package code.jtduan.retrofit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static code.jtduan.TConfig.*;

/**
 * @author jtduan
 * @date 2016/12/8
 */
@Service
public class TService {

    private String sessionid="";

    private String bIGipServerotn="";
    /**
     * 程序运行中需要获取的参数
     */
    private String globalRepeatSubmitToken="";
    private String key_check_isChange ="";
    private String leftTicketStr ="";

    private String train_location;
//    private String train_no;
//    private String from_station_telecode;
//    private String to_station_telecode;
    private String passengerTicketStr="";
    private String oldPassengerStr="";

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private OkHttpClient client;

    //
//    final X509TrustManager trustAllCerts = new X509TrustManager() {
//        @Override
//        public void checkClientTrusted(
//                java.security.cert.X509Certificate[] chain,
//                String authType) throws CertificateException {
//        }
//
//        @Override
//        public void checkServerTrusted(
//                java.security.cert.X509Certificate[] chain,
//                String authType) throws CertificateException {
//        }
//
//        @Override
//        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
//            X509Certificate[] x509Certificates = new X509Certificate[0];
//            return x509Certificates;
//        }
//    };
//    private final SSLContext sslContext;
//    private final SSLSocketFactory sslSocketFactory;

    public TService() throws NoSuchAlgorithmException, KeyManagementException {
//        sslContext = SSLContext.getInstance("TLS");
//        sslContext.init(null, new TrustManager[]{trustAllCerts}, null);
//        sslSocketFactory = sslContext.getSocketFactory();
        MyCookieJar cookieJar = new MyCookieJar(sessionid,bIGipServerotn);
        client = HttpsUtil.getValidOkHttpClient().newBuilder()
                .cookieJar(cookieJar)
                .build();
        passengerTicketStr ="O,0,1,"+realName+",1,"+cardId+",,N";
        oldPassengerStr = realName+",1,"+cardId+",1_";
    }

    private String buildParams(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        for (String str : map.keySet()) {
            sb.append(str).append("=").append(map.get(str)).append("&");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public void run() {
        while(true) {
            String secret = queryTicket();
            if(secret==null || secret.isEmpty()) continue;
            if(!submitOrder(secret)){
                continue;
            }
            confirm();
            confirmTicket();
        }
    }

    private String queryTicket() {
        String secret="";
        String url = "https://kyfw.12306.cn/otn/leftTicket/queryA";
        String params = "leftTicketDTO.train_date="+date+"&leftTicketDTO.from_station=BJP&leftTicketDTO.to_station=ZDN&purpose_codes=ADULT";
        String res = sendGet(url, params);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node;
        try {
            node = objectMapper.readTree(res);
        } catch (IOException e) {
            return null;
        }
        Iterator<JsonNode> it = node.get("data").iterator();
        while(it.hasNext()){
            JsonNode temp = it.next();
            JsonNode n = temp.get("queryLeftNewDTO");
            if(n.get("station_train_code").asText().equals(train)){
                secret = temp.get("secretStr").asText();
                train_location = n.get("location_code").asText();
//                train_no = temp.get("queryLeftNewDTO").get("train_no").asText();
//                from_station_telecode = temp.get("queryLeftNewDTO").get("from_station_telecode").asText();
//                to_station_telecode = temp.get("queryLeftNewDTO").get("to_station_telecode").asText();
                break;
            }
        };
        return secret;
    }

    private boolean submitOrder(String secret) {
        Map<String, String> params = new TreeMap<>();
        params.put("secretStr",secret);
        params.put("train_date", date);
        params.put("tour_flag", "dc");
        params.put("purpose_codes","ADULT");
        params.put("query_from_station_name", startCity);
        params.put("query_to_station_name", endCity);
        sendPost("https://kyfw.12306.cn/otn/login/checkUser","_json_att=");
        sendPost("https://kyfw.12306.cn/otn/leftTicket/submitOrderRequest",buildParams(params));
        String res = sendPost("https://kyfw.12306.cn/otn/confirmPassenger/initDc","_json_att=");
        Pattern p = Pattern.compile("globalRepeatSubmitToken *= *'([0-9A-Za-z]+)'");
        Matcher matcher = p.matcher(res);
        if(matcher.find()) {
            globalRepeatSubmitToken = matcher.group(1);
        }

        Pattern p2 = Pattern.compile("'key_check_isChange':'([0-9A-Za-z]+)'");
        Matcher matcher2 = p2.matcher(res);
        if(matcher2.find()) {
            key_check_isChange = matcher2.group(1);
        }

        Pattern p3 = Pattern.compile("'leftTicketStr':'([0-9A-Za-z]+)'");
        Matcher matcher3 = p3.matcher(res);
        if(matcher3.find()) {
            leftTicketStr = matcher3.group(1);
        }

        if(globalRepeatSubmitToken.isEmpty() || key_check_isChange.isEmpty() || leftTicketStr.isEmpty()) return false;
        return true;
    }

    private void confirmTicket() {
        Map<String, String> params = new TreeMap<>();
        params.put("passengerTicketStr",passengerTicketStr);
        params.put("oldPassengerStr",oldPassengerStr);
        params.put("randCode", "");
        params.put("purpose_codes","00");
        params.put("key_check_isChange", key_check_isChange);
        params.put("leftTicketStr", leftTicketStr);
        params.put("train_location", train_location);
        params.put("choose_seats", "");
        params.put("seatDetailType", "000");
        params.put("roomType", "00");
        params.put("dwAll", "N");
        params.put("_json_att", "");
        params.put("REPEAT_SUBMIT_TOKEN", globalRepeatSubmitToken);
        String res = sendPost("https://kyfw.12306.cn/otn/confirmPassenger/confirmSingleForQueue",buildParams(params));
    }

    public void confirm() {
        String url = "https://kyfw.12306.cn/otn/confirmPassenger/checkOrderInfo";
        Map<String, String> params = new TreeMap<>();
        params.put("cancel_flag", "2");
        params.put("bed_level_order_num", "000000000000000000000000000000");
        params.put("passengerTicketStr", passengerTicketStr);
        params.put("oldPassengerStr", oldPassengerStr);
        params.put("tour_flag", "dc");
        params.put("randCode", "");
        params.put("_json_att", "");
        params.put("REPEAT_SUBMIT_TOKEN", globalRepeatSubmitToken);
        String res = sendPost(url, buildParams(params));
    }

    private String sendPost(String url, String params) {
        RequestBody body = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded; charset=utf-8"), params);
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

    private String sendGet(String url, String params) {
        if (!params.isEmpty()) {
            url = url + "?" + params;
        }
        Request request = new Request.Builder()
                .url(url).get()
                .build();

        try {
            Response response = client.newCall(request).execute();
            return response.body().string();
        } catch (IOException e) {
            logger.warn(e.getMessage());
            return "";
        }
    }

//    public void getCount() {
//        String url = "https://kyfw.12306.cn/otn/confirmPassenger/getQueueCount";
//        Map<String, String> params = new TreeMap<>();
//        params.put("train_date", "Fri Jan 06 2017 00:00:00 GMT+0800");
//        params.put("train_no", train_no);
//        params.put("stationTrainCode", config.train);
//        params.put("seatType", "O");
//        params.put("fromStationTelecode", from_station_telecode);
//        params.put("toStationTelecode", to_station_telecode);
//        params.put("leftTicket", leftTicketStr);
//        params.put("purpose_codes", "00");
//        params.put("train_location", train_location);
//        params.put("_json_att", "");
//        params.put("REPEAT_SUBMIT_TOKEN", globalRepeatSubmitToken);
//        String res = sendPost(url, buildParams(params));
//        System.out.println(res);
//    }
}

@Component
class MyCookieJar implements CookieJar {

    private List<Cookie> cookies;

    private List<Cookie> preCookies;

    public MyCookieJar() {
    }

    public MyCookieJar(String sessionid, String bigip) {
        preCookies = new ArrayList<>();
        cookies = new ArrayList<>();
        Cookie cookie = new Cookie.Builder()
                .name("current_captcha_type").value("Z").domain("kyfw.12306.cn")
                .build();
        Cookie cookie2 = new Cookie.Builder()
                .name("JSESSIONID").value(sessionid).domain("kyfw.12306.cn").path("/otn")
                .build();

        Cookie cookie3 = new Cookie.Builder()
                .name("BIGipServerotn").value(bigip).domain("kyfw.12306.cn")
                .build();

//        Cookie cookie4 = new Cookie.Builder()
//                .name("_jc_save_fromDate").value("2017-01-09").domain("kyfw.12306.cn")
//                .build();
//
//        Cookie cookie5 = new Cookie.Builder()
//                .name("_jc_save_fromStation").value("%u5317%u4EAC%2CBJP").domain("kyfw.12306.cn")
//                .build();
//
//        Cookie cookie6 = new Cookie.Builder()
//                .name("_jc_save_showIns").value("true").domain("kyfw.12306.cn")
//                .build();
//        Cookie cookie7 = new Cookie.Builder()
//                .name("_jc_save_toDate").value("2017-01-09").domain("kyfw.12306.cn")
//                .build();
//        Cookie cookie8 = new Cookie.Builder()
//                .name("_jc_save_toStation").value("%u9A7B%u9A6C%u5E97%u897F%2CZLN").domain("kyfw.12306.cn")
//                .build();
//        Cookie cookie9 = new Cookie.Builder()
//                .name("_jc_save_wfdc_flag").value("dc").domain("kyfw.12306.cn")
//                .build();

        preCookies.add(cookie);
        preCookies.add(cookie2);
        preCookies.add(cookie3);
//        preCookies.add(cookie4);
//        preCookies.add(cookie5);
//        preCookies.add(cookie6);
//        preCookies.add(cookie7);
//        preCookies.add(cookie8);
//        preCookies.add(cookie9);
        this.cookies.addAll(preCookies);
    }

    @Override
    public void saveFromResponse(HttpUrl httpUrl, List<Cookie> list) {
        cookies.addAll(list);
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