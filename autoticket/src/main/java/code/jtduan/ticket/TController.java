package code.jtduan.ticket;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Created by djt on 12/22/16.
 */
@Controller
public class TController {

    @RequestMapping("/")
    public String index(){
        return "index";
    }
}
