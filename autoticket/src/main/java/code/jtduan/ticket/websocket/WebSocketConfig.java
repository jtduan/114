package code.jtduan.ticket.websocket;

import code.jtduan.ticket.TVariables;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.Map;

/**
 * The type Web socket config.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig extends
        AbstractWebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setHandshakeHandler(myHandshakeHandler())
                .withSockJS();
    }

    @Bean
    public MyHandshakeHandler myHandshakeHandler() {
        return new MyHandshakeHandler();
    }
}

/**
 * using MyHandshakeHandler can send messages to any one session
 */
class MyHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        Principal user = new Auth(TVariables.cardId);
        return user;
    }

    /**
     * The type Auth.
     */
    class Auth implements Principal {

        private String sessionId;

        @Override
        public String getName() {
            return sessionId;
        }

        @Override
        public boolean implies(Subject subject) {
            return true;
        }

        /**
         * Instantiates a new Auth.
         */
        public Auth() {
        }

        /**
         * Instantiates a new Auth.
         *
         * @param sessionId the session id
         */
        public Auth(String sessionId) {
            this.sessionId = sessionId;
        }
    }
}
