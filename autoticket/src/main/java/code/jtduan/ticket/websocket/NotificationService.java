package code.jtduan.ticket.websocket;

/**
 * Created by djt on 9/24/16.
 */

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Sending msg to custom user(sessionid)
     * @param sessionId
     * @param msg
     */
    public void notifyOne(String sessionId,String msg) {
        messagingTemplate.convertAndSendToUser(sessionId, "/queue/notify",msg);
        return;
    }

    /**
     * Sending msg to users
     * @param msg
     */
    public void notifyAll(String msg) {
        messagingTemplate.convertAndSend("/queue/notify",msg);
        return;
    }

}

