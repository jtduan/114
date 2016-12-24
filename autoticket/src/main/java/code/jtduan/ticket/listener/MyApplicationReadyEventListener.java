package code.jtduan.ticket.listener;

import code.jtduan.ticket.util.SpringUtil;
import code.jtduan.ticket.back.PicThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

public class MyApplicationReadyEventListener implements ApplicationListener<ApplicationReadyEvent> {

    private Logger logger = LoggerFactory.getLogger(MyApplicationReadyEventListener.class);

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        new Thread(SpringUtil.getBean(PicThread.class)).start();
    }
}