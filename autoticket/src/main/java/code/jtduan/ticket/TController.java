package code.jtduan.ticket;

import okhttp3.Request;
import okhttp3.Response;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by djt on 12/22/16.
 */
@Controller
public class TController {

    @Autowired
    private TService service;

    private byte[] bytes = new byte[0];

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String index(Model model) {
        model.addAttribute("logined", service.keepSession());
        model.addAttribute("realName", TConfig.realName);
        model.addAttribute("cardId", TConfig.cardId);
        return "home";
    }

    @RequestMapping(value = "/verify", method = RequestMethod.POST)
    public String update(HttpServletRequest request) {
        String verifyPic = request.getParameter("verifyCode");
        if (verifyPic != null && !verifyPic.isEmpty()) {
            TConfig.verifyPic = verifyPic;
        }
        service.login();
        return "redirect:/";
    }

    @RequestMapping(value = "/user", method = RequestMethod.POST)
    public String updateUser(HttpServletRequest request) {
        String realName = request.getParameter("realName");
        if (realName != null && !realName.isEmpty()) {
            TConfig.realName = realName;
        }
        String cardId = request.getParameter("cardId");
        if (cardId != null && !cardId.isEmpty()) {
            TConfig.cardId = cardId;
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

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String resfresh(HttpServletResponse response, Model model) {
        model.addAttribute("logined", service.keepSession());
        model.addAttribute("realName", TConfig.realName);
        model.addAttribute("cardId", TConfig.cardId);
        Request httpRequest = new Request.Builder()
                .url("https://kyfw.12306.cn/otn/passcodeNew/getPassCodeNew?module=login&rand=sjrand&" + Math.random()).get()
                .build();

        try {
            Response httpResresponse = service.client.newCall(httpRequest).execute();
            bytes = httpResresponse.body().bytes();
        } catch (IOException e) {
        }
        return "login";
    }

    @RequestMapping(value = "/new", method = RequestMethod.GET)
    public String newCookie(HttpServletResponse response) {
        service.clearCookie();
        bytes = new byte[0];
        return "forward:/";
    }

    @RequestMapping(value = "/start", method = RequestMethod.GET)
    public String start(HttpServletResponse response) {
        service.run();
        return "forward:/";
    }
}
