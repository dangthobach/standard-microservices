package com.enterprise.process.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // API endpoints
        registry.addMapping("/api/**")
                .allowedOriginPatterns("http://localhost:3000", "http://localhost:3001", "http://127.0.0.1:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
        
        // WebSocket endpoints
        registry.addMapping("/ws/**")
                .allowedOriginPatterns("http://localhost:3000", "http://localhost:3001", "http://127.0.0.1:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false);
                
        // All other endpoints
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:3000", "http://localhost:3001", "http://127.0.0.1:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false);
    }
}

