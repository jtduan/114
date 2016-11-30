package code.jtduan.main;

import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.BasicCookieStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class Config {
    static final String loginURL = "http://www.bjguahao.gov.cn/quicklogin.htm";
    static final String getDoctorURL = "http://www.bjguahao.gov.cn/dpt/partduty.htm";
    static final String confirmUrl = "http://www.bjguahao.gov.cn/order/confirm.htm";
    static final String verifyCodeURL = "http://www.bjguahao.gov.cn/v/sendorder.htm";

    static CookieStore cookieStore = new BasicCookieStore();
    static String doctorId = "";
    static String dutySourceId = "";

    @Value("${name}")
    public String userName;

    @Value("${pwd}")
    public String userPwd;

    @Value("${date}")
    public String date;

    @Value("${duty}")
    public String duty;

    @Value("${hospitalId}")
    public String hospitalId;

    @Value("${departmentId}")
    public String departmentId;

    @Value("${patientId}")
    public String patientId;

    @Value("${verifyCode}")
    public Integer verifyCode;
}
