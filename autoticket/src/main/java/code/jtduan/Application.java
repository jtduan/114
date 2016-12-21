package code.jtduan;

import code.jtduan.ticket.TService;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

/**
 * 开发环境运行需要将配置文件application.bak.properties改名为application.properties
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) throws InterruptedException {
        ApplicationContext ctx;
        try {
            ctx = SpringApplication.run(Application.class, args);
        } catch (Exception e) {
            LoggerFactory.getLogger(Application.class).error("no avaliable configuare files...");
            return;
        }
        ((TService) ctx.getBean(TService.class)).init();
    }
}
