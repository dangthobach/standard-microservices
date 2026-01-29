package com.enterprise.process.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable a simple in-memory message broker to carry the messages back to the client
        registry.enableSimpleBroker("/topic", "/queue");
        
        // Designates the "/app" prefix for messages that are bound for methods annotated with @MessageMapping
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register "/ws" endpoint, enabling SockJS fallback options
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://localhost:3000", "http://localhost:3001", "http://127.0.0.1:3000")
                .withSockJS();
    }
}

