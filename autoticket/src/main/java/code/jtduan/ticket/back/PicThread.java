package code.jtduan.ticket.back;


import code.jtduan.ticket.TVariables;
import code.jtduan.ticket.util.OKHttpUtil;
import code.jtduan.ticket.websocket.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.LockSupport;

@Component("PicThread")
public class PicThread implements Runnable {

    @Autowired
    private NotificationService notificationService;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void run() {
        TVariables.picThread = Thread.currentThread();
        logger.info("pic-thread begin running.....");
        while (true) {
            LockSupport.park();

            TVariables.bytesConfirm = OKHttpUtil.getAndResponse(
                    "https://kyfw.12306.cn/otn/passcodeNew/getPassCodeNew?module=passenger&rand=randp&" + Math.random(), 20);

            /**
             * 通知前端有新图片需要验证
             */
            notificationService.notifyAll("1");
            logger.info("pic-thread request a new picture.....");
        }
    }
}
