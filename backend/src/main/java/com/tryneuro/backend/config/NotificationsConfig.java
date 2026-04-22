package com.tryneuro.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationsConfig {

    @Value("${services.notifications.url:http://notifications:8081}")
    private String notificationsUrl;

    @Bean(name = "notificationsUrl")
    public String notificationsUrl() {
        return notificationsUrl;
    }
}
