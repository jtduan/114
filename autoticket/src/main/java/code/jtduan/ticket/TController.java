package code.jtduan.ticket;

import code.jtduan.ticket.util.OKHttpUtil;
import code.jtduan.ticket.websocket.NotificationService;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.locks.LockSupport;

import static code.jtduan.ticket.TVariables.*;

/**
 * Created by djt on 12/22/16.
 */
@Controller
public class TController {

    @Autowired
    private TService service;

    @Autowired
    private NotificationService notificationService;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String index(Model model) {
        model.addAttribute("logined", service.keepSession());
        model.addAttribute("passangerCode", passangerCode);
        model.addAttribute("realName", realName);
        model.addAttribute("cardId", cardId);
        return "home";
    }

    @RequestMapping(value = "/verify", method = RequestMethod.POST)
    public String update(HttpServletRequest request) {
        String temp = request.getParameter("verifyCode");
        if (temp != null && !temp.isEmpty()) {
            verifyPic = temp;
        }
        service.login();
        return "redirect:/";
    }

    @RequestMapping(value = "/user", method = RequestMethod.POST)
    public String updateUser(HttpServletRequest request) {
        String temp = request.getParameter("realName");
        if (temp != null && !temp.isEmpty()) {
            realName = temp;
        }
        temp = request.getParameter("cardId");
        if (temp != null && !temp.isEmpty()) {
            cardId = temp;
        }
        return "redirect:/";
    }


    @RequestMapping(value = "/getPic", method = RequestMethod.GET)
    @ResponseBody
    public void getPic(HttpServletResponse response) {
        try {
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
            response.getOutputStream().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequestMapping(value = "/getPicConfirm", method = RequestMethod.GET)
    @ResponseBody
    public void getPicConfirm(HttpServletResponse response) {
        try {
            response.getOutputStream().write(bytesConfirm);
            response.getOutputStream().flush();
            response.getOutputStream().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String resfresh(Model model) {
        model.addAttribute("realName", realName);
        model.addAttribute("cardId", cardId);

        bytes = OKHttpUtil.getAndResponse("https://kyfw.12306.cn/otn/passcodeNew/getPassCodeNew?module=login&rand=sjrand&" + Math.random(), 3);
        return "login";
    }

    @RequestMapping(value = "/new", method = RequestMethod.GET)
    public String newCookie() {
        OKHttpUtil.clearCookie();
        bytes = new byte[0];
        return "forward:/";
    }

    @RequestMapping(value = "/start", method = RequestMethod.GET)
    public String start(HttpServletResponse response) {
        service.run();
        return "forward:/";
    }

//    @RequestMapping(value = "/passenger", method = RequestMethod.GET)
//    @ResponseBody
//    public boolean getPassangerPic(HttpServletResponse response) {
//        try {
//            if (passangerCode.isEmpty() && picCommited) {
//                Request httpRequest = new Request.Builder()
//                        .url("https://kyfw.12306.cn/otn/passcodeNew/getPassCodeNew?module=passenger&rand=randp&" + Math.random()).get()
//                        .build();
//
//                Response httpResresponse = service.client.newCall(httpRequest).execute();
//                bytesConfirm = httpResresponse.body().bytes();
//                picCommited = false;
//                return true;
//            } else {
//                return false;
//            }
//        } catch (IOException e) {
//            return false;
//        }
//    }

    @RequestMapping(value = "/passenger", method = RequestMethod.POST)
    @ResponseBody
    public String postPassangerPic(HttpServletRequest request) {
        String temp = request.getParameter("passangerCode");
        passangerCode = service.getverifyCodeString(temp);
        bytesConfirm = new byte[0];
        notificationService.notifyAll("2");
        /**
         * 允许抢票线程执行确认订单操作
         */
        LockSupport.unpark(autoThread);
        return passangerCode;
    }

    @RequestMapping(value = "/code", method = RequestMethod.GET)
    public String code(HttpServletRequest request, Model model) {
        model.addAttribute("code", passangerCode);
        return "passanger";
    }

    @RequestMapping(value = "/startQueryPic", method = RequestMethod.GET)
    @ResponseBody
    public String startQueryPic(HttpServletRequest request, Model model) {
        /**
         * 释放许可，允许获取新验证码
         */
        LockSupport.unpark(picThread);
        return "true";
    }
}
