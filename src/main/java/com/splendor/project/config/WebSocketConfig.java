package com.splendor.project.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 클라이언트가 구독할 때 사용하는 접두사 (서버 -> 클라이언트 메시지)
        config.enableSimpleBroker("/topic");
        // 클라이언트가 서버로 메시지를 보낼 때 사용하는 접두사 (클라이언트 -> 서버 액션)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 클라이언트가 연결할 STOMP 엔드포인트
        registry.addEndpoint("/ws-connect").setAllowedOriginPatterns("*");
    }
}
