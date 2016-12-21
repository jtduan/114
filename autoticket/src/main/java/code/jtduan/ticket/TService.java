package code.jtduan.ticket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static code.jtduan.ticket.TConfig.*;

/**
 * @author jtduan
 * @date 2016/12/8
 */
@Service
public class TService {

    @Value("${sessionid}")
    public String sessionid;

    @Value("${bigip}")
    public String bIGipServerotn;

    @Value("${date}")
    public String date;
    /**
     * 程序运行中需要获取的参数
     */
    private String globalRepeatSubmitToken = "";
    private String key_check_isChange = "";
    private String leftTicketStr = "";

    private String train_location;
    private String passengerTicketStr = "";
    private String oldPassengerStr = "";

    private String siteTypeStr = "";

    private OkHttpClient client;

    public boolean inited = false;

    public void run() {
        if (!inited && !init()) {
            System.out.println("params has errors,program will exit...");
            return;
        }
        String str = sendGet("https://kyfw.12306.cn/otn/modifyUser/initQueryUserInfo", "");
        if (!str.contains(realName)) {
            System.out.println("Login FAILED ,program will exit...");
            return;
        }

        while (true) {
            try {
                System.out.print(".");
                String secret = queryTicket();
                if (secret == null || secret.isEmpty()) continue;
                System.out.print("find Tickets...");
                if (!submitOrder(secret)) {
                    System.out.print("submitOrder failed...");
                    continue;
                }
                if (!checkOrderInfo()) {
                    System.out.print("checkOrderInfo failed...");
                    continue;
                }
                if (confirmTicket()) {
                    System.out.println("order success...");
                    return;
                }
            } catch (Exception e) {
                System.out.println("System error...continue");
            }
        }
    }

    public boolean init() {
        System.out.println("===========");
        System.out.println("date:" + date);
        System.out.println("train:" + train);
        System.out.println("start:" + start);
        System.out.println("end:" + end);
        System.out.println("cardId:" + cardId);
        System.out.println("siteType:" + siteType);
        System.out.println("===========");

        Scanner cin = new Scanner(System.in);
        if (sessionid.isEmpty()) {
            System.out.println("cin the SessionId:");
            sessionid = cin.nextLine();
        }
        if (bIGipServerotn.isEmpty()) {
            System.out.println("cin the bIGipServerotn:");
            bIGipServerotn = cin.nextLine();
        }
        MyCookieJar cookieJar = new MyCookieJar(sessionid, bIGipServerotn);
        client = HttpsUtil.getUnsafeOkHttpClient().newBuilder()
                .cookieJar(cookieJar)
                .build();
        passengerTicketStr = siteType + ",0,1," + realName + ",1," + cardId + ",,N";
        oldPassengerStr = realName + ",1," + cardId + ",1_";

        if (siteType.equals("O")) {
            siteTypeStr = "ze_num";
        } else if (siteType.equals("3")) {
            siteTypeStr = "yw_num";
        } else {
            System.out.println(" invalid siteType ");
            return false;
        }
        inited = true;
        return true;
    }

    private String queryTicket() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("leftTicketDTO.train_date", date);
        params.put("leftTicketDTO.from_station", start);
        params.put("leftTicketDTO.to_station", end);
        params.put("purpose_codes", "ADULT");

        String url = "https://kyfw.12306.cn/otn/leftTicket/queryA";
        String res = sendGet(url, buildParams(params));
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node;
        try {
            node = objectMapper.readTree(res);
        } catch (IOException e) {
            if (res.contains("拒绝访问")) System.out.print("denied.");
            return "";
        }
        if (node.get("data") == null) {
            return "";
        }
        Iterator<JsonNode> it = node.get("data").iterator();
        while (it.hasNext()) {
            JsonNode temp = it.next();
            JsonNode n = temp.get("queryLeftNewDTO");
            if (train.contains(n.get("station_train_code").asText()) &&
                    !(("--").equals(n.get(siteTypeStr).asText()) || ("无").equals(n.get(siteTypeStr).asText()))) {
                train_location = n.get("location_code").asText();
                return temp.get("secretStr").asText();
            }
        }
        return "";
    }

    private boolean submitOrder(String secret) {
        Map<String, String> params = new TreeMap<>();
        params.put("secretStr", secret);
        params.put("train_date", date);
        params.put("tour_flag", "dc");
        params.put("purpose_codes", "ADULT");
        params.put("query_from_station_name", startCity);
        params.put("query_to_station_name", endCity);
        String temp = sendPost("https://kyfw.12306.cn/otn/leftTicket/submitOrderRequest", buildParams(params));

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node;
        try {
            node = objectMapper.readTree(temp);
            if (node.get("status").asText().equals("false")) {
                System.out.println(node.get("messages").get(0).asText());
                return false;
            }
        } catch (IOException e) {
            return false;
        }

        String res = sendPost("https://kyfw.12306.cn/otn/confirmPassenger/initDc", "_json_att=");
        Pattern p = Pattern.compile("globalRepeatSubmitToken *= *'([0-9%A-Za-z]+)'");
        Matcher matcher = p.matcher(res);
        if (matcher.find()) {
            globalRepeatSubmitToken = matcher.group(1);
        }

        Pattern p2 = Pattern.compile("'key_check_isChange':'([0-9%A-Za-z]+)'");
        Matcher matcher2 = p2.matcher(res);
        if (matcher2.find()) {
            key_check_isChange = matcher2.group(1);
        }

        Pattern p3 = Pattern.compile("'leftTicketStr':'([0-9%A-Za-z]+)'");
        Matcher matcher3 = p3.matcher(res);
        if (matcher3.find()) {
            leftTicketStr = matcher3.group(1);
        }

        if (globalRepeatSubmitToken.isEmpty() || key_check_isChange.isEmpty() || leftTicketStr.isEmpty()) return false;
        return true;
    }

    private boolean confirmTicket() {
        Map<String, String> params = new TreeMap<>();
        params.put("passengerTicketStr", passengerTicketStr);
        params.put("oldPassengerStr", oldPassengerStr);
        params.put("randCode", "");
        params.put("purpose_codes", "00");
        params.put("key_check_isChange", key_check_isChange);
        params.put("leftTicketStr", leftTicketStr);
        params.put("train_location", train_location);
        params.put("choose_seats", "");
        params.put("seatDetailType", "000");
        params.put("roomType", "00");
        params.put("dwAll", "N");
        params.put("_json_att", "");
        params.put("REPEAT_SUBMIT_TOKEN", globalRepeatSubmitToken);
        String res = sendPost("https://kyfw.12306.cn/otn/confirmPassenger/confirmSingleForQueue", buildParams(params));
        System.out.println("results:" + res);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node;
        try {
            node = objectMapper.readTree(res);
        } catch (IOException e) {
            return false;
        }
        return node.get("status").asText().equals("true") && node.get("data").get("submitStatus").asText().equals("true");
    }

    private boolean checkOrderInfo() {
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
        String str = sendPost(url, buildParams(params));

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node;
        try {
            node = objectMapper.readTree(str);
        } catch (IOException e) {
            return false;
        }
        if (node.get("data").get("submitStatus").asText().equals("true")) {
            return true;
        } else {
            System.out.println(node.get("data").get("errMsg"));
            return false;
        }
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
            System.out.println(e.getMessage());
            return "";
        }
    }

    public String sendGet(String url, String params) {
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
            System.out.println(e.getMessage());
            return "";
        }
    }

    private String buildParams(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        for (String str : map.keySet()) {
            sb.append(str).append("=").append(map.get(str)).append("&");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public void keepSession() throws InterruptedException {
        if (!inited) {
            return;
        }
        String res = sendGet("https://kyfw.12306.cn/otn/modifyUser/initQueryUserInfo", "");
        if (res.contains(realName)) {
            System.out.println("===session valid===");
        } else {
            System.out.println("===session Invalid===");
        }
    }

    private class MyCookieJar implements CookieJar {

        private List<Cookie> cookies;


        public MyCookieJar(String sessionid, String bigip) {
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

            this.cookies.add(cookie);
            this.cookies.add(cookie2);
            this.cookies.add(cookie3);
        }

        @Override
        public void saveFromResponse(HttpUrl httpUrl, List<Cookie> list) {
        }

        @Override
        public List<Cookie> loadForRequest(HttpUrl httpUrl) {
            if (cookies == null) return Collections.emptyList();
            return cookies;
        }
    }
}