package code.jtduan;

import code.jtduan.ticket.TService;
import code.jtduan.ticket.listener.MyApplicationReadyEventListener;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

/**
 * 主类
 * 开启定时任务支持和WebSocket支持
 */
@SpringBootApplication
@EnableWebSocket
@EnableScheduling
public class Application {

    public static void main(String[] args) throws InterruptedException {
        SpringApplication app = new SpringApplication(Application.class);
        app.addListeners(new MyApplicationReadyEventListener());
        app.run(args);
    }
}
