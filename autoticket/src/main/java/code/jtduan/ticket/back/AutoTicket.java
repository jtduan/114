package code.jtduan.ticket.back;

import code.jtduan.ticket.TService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 定时任务配置
 */
@Service
public class AutoTicket {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private TService service;

    @Scheduled(cron = "${cron}")
    public void demoSchedule() {
        service.run();
    }

    @Scheduled(cron = "0 2/4 * * * ?")
    public void keepSession() {
        service.keepSession();
    }
}
