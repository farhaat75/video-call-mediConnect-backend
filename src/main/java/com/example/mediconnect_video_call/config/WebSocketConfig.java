package com.example.mediconnect_video_call.config;

import com.example.mediconnect_video_call.handler.SignalingHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(signalingHandler(), "/ws/video")
                .setAllowedOrigins("*");
    }

    @Bean
    public SignalingHandler signalingHandler() {
        return new SignalingHandler();
    }
}
