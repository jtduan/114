package code.jtduan;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Config {
    public static final String loginURL = "http://www.bjguahao.gov.cn/quicklogin.htm";
    public static final String getDoctorURL = "http://www.bjguahao.gov.cn/dpt/partduty.htm";
    public static final String confirmUrl = "http://www.bjguahao.gov.cn/order/confirm.htm";
    public static final String verifyCodeURL = "http://www.bjguahao.gov.cn/v/sendorder.htm";

    public static String doctorId = "";
    public static String dutySourceId = "";

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
