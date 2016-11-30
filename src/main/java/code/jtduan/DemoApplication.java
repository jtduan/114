package code.jtduan;

import code.jtduan.main.RService;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

/**
 * 开发环境运行需要将配置文件application.bak.properties改名为application.properties
 */
@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args){
		ApplicationContext ctx;
		try {
			ctx = SpringApplication.run(DemoApplication.class, args);
		}catch (Exception e){
			LoggerFactory.getLogger(DemoApplication.class).error("找不到配置文件或者配置文件格式错误...");
			return;
		}
		((RService)ctx.getBean(RService.class)).run();
	}
}
