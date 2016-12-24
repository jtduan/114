package code.jtduan.ticket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.locks.LockSupport;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static code.jtduan.ticket.TVariables.*;
import static code.jtduan.ticket.util.OKHttpUtil.sendGet;
import static code.jtduan.ticket.util.OKHttpUtil.sendPost;

/**
 * @author jtduan
 * @date 2016/12/8
 */
@Service
public class TService {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${start}")
    public String start;
    @Value("${end}")
    public String end;
    @Value("${train}")
    public String train;
    @Value("${seat}")
    public String siteType;
    @Value("${date}")
    public String date;

    @Value("${name}")
    public String userName;
    @Value("${password}")
    public String pwd;

    @Value("${sync}")
    public boolean sync;

    /**
     * 程序运行中需要获取的参数
     */
    private String globalRepeatSubmitToken = "";
    private String key_check_isChange = "";
    private String leftTicketStr = "";

    private List<Train> find_trains = new ArrayList<>();
    private Train cur_train;

//    public OkHttpClient client;
//    public MyCookieJar cookieJar;

    public TService() {
//        find_trains = new ArrayList<>();
//        cookieJar = new MyCookieJar();
//        client = HttpsUtil.getUnsafeOkHttpClient().newBuilder()
//                .cookieJar(cookieJar)
//                .build();
    }

    /**
     * 抢票 主函数
     */
    public void run() {
        LocalTime now = LocalTime.now();
        logger.info("[Task start]");
        /**
         * 初始化
         */
        if (!init()) return;
        autoThread = Thread.currentThread();
        if (cardId == null || cardId.isEmpty()) return;

        /**
         * 循环3分钟
         */
        while (LocalTime.now().compareTo(now.plusMinutes(3)) < 0) {
            try {
                System.out.print(".");
                if (!queryTicket()) {
                    Thread.sleep(400);
                    continue;
                }
                System.out.print("[find Tickets]");
                for (Train temp : find_trains) {
                    cur_train = temp;
                    if (!submitOrder(cur_train.secret)) {
                        System.out.print("[submitOrder:failed]");
                        continue;
                    }
                    for (int j = 0; j < 2; j++) {
                        if (!initDc()) {
                            System.out.print("[initDc:failed]");
                            continue;
                        }
                        if (!checkOrderInfo()) {
                            continue;
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("[System Error]");
            }
        }
    }

    /**
     * 加速程序执行，初始化变量
     */
    private boolean init() {
        try {
            passengerTicketStr = URLEncoder.encode(siteType + ",0,1," + realName + ",1," + cardId + ",,N", "UTF-8");
            oldPassengerStr = URLEncoder.encode(realName + ",1," + cardId + ",1_", "UTF-8");
            seatTypeStr = convert(siteType);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private String convert(String siteType) {
        switch (siteType) {
            case "O":
                return "ze_num";
            case "3":
                return "yw_num";
            default:
                throw new UnsupportedOperationException("not valid siteType");
        }
    }

    private void getPassangers() {
        sendPost("https://kyfw.12306.cn/otn/confirmPassenger/getPassengerDTOs", "_json_att=&REPEAT_SUBMIT_TOKEN=" + globalRepeatSubmitToken);
    }

    private void checkUser() {
        sendPost("https://kyfw.12306.cn/otn/login/checkUser", "_json_att=");
    }

    private boolean queryTicket() {
        find_trains.clear();

        Map<String, String> params = new LinkedHashMap<>();
        params.put("leftTicketDTO.train_date", date);
        params.put("leftTicketDTO.from_station", start);
        params.put("leftTicketDTO.to_station", end);
        params.put("purpose_codes", "ADULT");

        String url = "https://kyfw.12306.cn/otn/leftTicket/queryA";
        String res = sendGet(url, buildParams(params));
        if (res.contains("拒绝访问")) {
            System.out.print("[denied]");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return false;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node;
        try {
            node = objectMapper.readTree(res);
        } catch (IOException e) {
            System.out.print("[" + res + "]");
            return false;
        }
        if (node.get("data") == null) {
            System.out.println("[" + res + "]");
            return false;
        }
        for (JsonNode temp : node.get("data")) {
            JsonNode n = temp.get("queryLeftNewDTO");
            if (train.contains(n.get("station_train_code").asText()) &&
                    ("有".equals(n.get(seatTypeStr).asText()) || "[0-9]+".matches(n.get(seatTypeStr).asText()))) {
                Train t = new Train();
                t.secret = temp.get("secretStr").asText();
                if (t.secret == null || t.secret.isEmpty()) {
                    continue;
                }
                t.train_location = n.get("location_code").asText();
                t.train_no = n.get("train_no").asText();
                t.from_station_telecode = n.get("from_station_telecode").asText();
                t.to_station_telecode = n.get("to_station_telecode").asText();
                t.from_station_name = n.get("from_station_name").asText();
                t.to_station_name = n.get("to_station_name").asText();
                t.station_train_code = n.get("station_train_code").asText();
                find_trains.add(t);
            }
        }
        return !find_trains.isEmpty();
    }

    private boolean submitOrder(String secret) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("secretStr", secret);
        params.put("train_date", date);
        params.put("tour_flag", "dc");
        params.put("purpose_codes", "ADULT");
        params.put("query_from_station_name", cur_train.from_station_name);
        params.put("query_to_station_name", cur_train.to_station_name);
        String temp = sendPost("https://kyfw.12306.cn/otn/leftTicket/submitOrderRequest", buildParams(params));

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node;
        try {
            node = objectMapper.readTree(temp);
            if (node.get("status").asText().equals("false")) {
                System.out.print(node.get("messages").get(0).asText());
                return false;
            }
        } catch (IOException e) {
            System.out.print("[submitOrder]:" + temp);
            return false;
        }
        return true;
    }

    public boolean initDc() {
        key_check_isChange = "";
        globalRepeatSubmitToken = "";
        leftTicketStr = "";
        String res = sendPost("https://kyfw.12306.cn/otn/confirmPassenger/initDc", "_json_att=", 1);
        if (res == null || res.isEmpty()) return false;
        Pattern p = Pattern.compile("globalRepeatSubmitToken {1,3}= {1,3}'(.+?)'");
        Matcher matcher = p.matcher(res);
        if (matcher.find()) {
            globalRepeatSubmitToken = matcher.group(1);
        }

        Pattern p2 = Pattern.compile("'key_check_isChange':'(.+?)'");
        Matcher matcher2 = p2.matcher(res);
        if (matcher2.find()) {
            key_check_isChange = matcher2.group(1);
        }

        Pattern p3 = Pattern.compile("'leftTicketStr':'(.+?)'");
        Matcher matcher3 = p3.matcher(res);
        if (matcher3.find()) {
            leftTicketStr = matcher3.group(1);
        }

        return !(key_check_isChange.isEmpty() || globalRepeatSubmitToken.isEmpty() || leftTicketStr.isEmpty());
    }

    private boolean checkOrderInfo() {
        String url = "https://kyfw.12306.cn/otn/confirmPassenger/checkOrderInfo";
        Map<String, String> params = new LinkedHashMap<>();
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
            System.out.print("[checkOrderInfo]:" + str);
            return false;
        }
        JsonNode data = node.get("data");
        if (data.get("submitStatus").asText().equals("true")) {
            System.out.print("[checkOrderInfo:success]");
            getQueueCount();
            /**
             * 判断是否需要验证码
             */
            if ("N".equals(data.get("ifShowPassCode").asText())) {
                return confirmTicket(false);
            } else {
                return confirmTicketSync();
            }
        } else {
            System.out.print("[checkOrderInfo]:" + data.get("errMsg"));
            return false;
        }
    }


    @Deprecated
    private void getQueueCount() {
        SimpleDateFormat sdf = new SimpleDateFormat("E MMM dd yyyy HH:mm:ss 'GMT'Z", Locale.US);
        String url = "https://kyfw.12306.cn/otn/confirmPassenger/getQueueCount";
        Map<String, String> params = new LinkedHashMap<>();

        try {
            params.put("train_date", URLEncoder.encode(sdf.format(new SimpleDateFormat("yyyy-MM-dd").parse(date)), "UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        params.put("train_no", cur_train.train_no);
        params.put("stationTrainCode", cur_train.station_train_code);
        params.put("seatType", siteType);
        params.put("fromStationTelecode", cur_train.from_station_telecode);
        params.put("toStationTelecode", cur_train.to_station_telecode);
        params.put("leftTicket", leftTicketStr);
        params.put("purpose_codes", "00");
        params.put("train_location", cur_train.train_location);
        params.put("_json_att", "");
        params.put("REPEAT_SUBMIT_TOKEN", globalRepeatSubmitToken);
        String res = sendPost(url, buildParams(params), 1);
        logger.debug("[getQueueCount]" + res);
    }

    private boolean confirmTicketSync() {
        /**
         * 检测是否允许执行，在Controller里用户输入验证码后释放许可
         */
        LockSupport.park();
        boolean res = confirmTicket(true);
        /**
         * 允许获取验证码线程执行
         */
        LockSupport.unpark(picThread);
        return res;
    }

    private boolean confirmTicket(boolean needverifyCode) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("passengerTicketStr", passengerTicketStr);
        params.put("oldPassengerStr", oldPassengerStr);
        params.put("randCode", needverifyCode ? passangerCode : "");
        params.put("purpose_codes", "00");
        params.put("key_check_isChange", key_check_isChange);
        params.put("leftTicketStr", leftTicketStr);
        params.put("train_location", cur_train.train_location);
        params.put("choose_seats", "");
        params.put("seatDetailType", "000");
        params.put("roomType", "00");
        params.put("dwAll", "N");
        params.put("_json_att", "");
        params.put("REPEAT_SUBMIT_TOKEN", globalRepeatSubmitToken);
        String res = sendPost("https://kyfw.12306.cn/otn/confirmPassenger/confirmSingleForQueue", buildParams(params));
        logger.info("[results]:" + res);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node;
        try {
            node = objectMapper.readTree(res);
            return node.get("status").asText().equals("true") && node.get("data").get("submitStatus").asText().equals("true");
        } catch (Exception e) {
            return false;
        }
    }

    public void login() {
        Map<String, String> params1 = new LinkedHashMap<>();
        params1.put("randCode", getverifyCodeString(verifyPic));
        params1.put("rand", "sjrand");
        String res1 = sendPost("https://kyfw.12306.cn/otn/passcodeNew/checkRandCodeAnsyn", buildParams(params1));
        logger.info(res1);
        Map<String, String> params = new LinkedHashMap<>();
        params.put("loginUserDTO.user_name", userName);
        params.put("userDTO.password", pwd);
        params.put("randCode", getverifyCodeString(verifyPic));
        sendPost("https://kyfw.12306.cn/otn/login/loginAysnSuggest", buildParams(params), 3);
    }

    private String buildParams(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        for (String str : map.keySet()) {
            sb.append(str).append("=").append(map.get(str)).append("&");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    @Deprecated
    public String getverifyCodeString(String code) {
        if (code.isEmpty()) return "";
        String[] pics = code.split("[ ,]");
        StringBuilder sb = new StringBuilder();
        for (String pic : pics) {
            int x = 70 * (pic.charAt(0) - '0' - 1) + 35;
            int y = 70 * (pic.charAt(1) - '0' - 1) + 35;
            sb.append(y).append(",").append(x).append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public boolean keepSession() {
        String res = sendGet("https://kyfw.12306.cn/otn/modifyUser/initQueryUserInfo", "");
        if (res.contains("退出")) {
            logger.info("===session valid===");
            return true;
        } else {
            logger.info("===session Invalid===");
            return false;
        }
    }

//    private String sendPost(String url, String params) {
//        return sendPost(url, params, 2);
//    }
//
//    private String sendPost(String url, String params, int num) {
//        logger.info(url + ":" + params);
//        RequestBody body = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded; charset=utf-8"), params);
//        Request request = new Request.Builder()
//                .url(url).post(body)
//                .build();
//
//        return send(request, num);
//    }
//
//    private String sendGet(String url, String params) {
//        if (!params.isEmpty()) {
//            url = url + "?" + params;
//        }
//        Request request = new Request.Builder()
//                .url(url).get()
//                .build();
//        return send(request, 2);
//    }
//
//    private String send(Request request, int num) {
//        try {
//            Response response = client.newCall(request).execute();
//            return response.body().string();
//        } catch (IOException e) {
//            if (num > 1) {
//                return send(request, num - 1);
//            } else {
//                logger.debug(e.getMessage());
//                return "";
//            }
//        }
//    }
//

//    public void clearCookie() {
//        client = client.newBuilder().cookieJar(new MyCookieJar()).build();
//    }
//
//    private class MyCookieJar implements CookieJar {
//
//        private List<Cookie> cookies;
//
//        MyCookieJar() {
//            cookies = new ArrayList<>();
//        }
//
//        @Override
//        public void saveFromResponse(HttpUrl httpUrl, List<Cookie> list) {
//            this.cookies.addAll(list);
//        }
//
//        @Override
//        public List<Cookie> loadForRequest(HttpUrl httpUrl) {
//            if (cookies == null) return Collections.emptyList();
//            return cookies;
//        }
//
//    }

    class Train {
        public String train_no;
        public String from_station_telecode;
        public String to_station_telecode;
        public String train_location;
        public String secret;
        public String from_station_name;
        public String to_station_name;
        public String station_train_code;
    }
}