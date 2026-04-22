package com.tryneuro.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationsConfig {

    @Value("${services.notifications.implementation:java}")
    private String implementation;

    @Value("${services.notifications.java.url:http://notifications-java:8081}")
    private String javaUrl;

    @Value("${services.notifications.python.url:http://notifications-python:8081}")
    private String pythonUrl;

    @Bean(name = "notificationsUrl")
    public String notificationsUrl() {
        if ("python".equalsIgnoreCase(implementation)) {
            return pythonUrl;
        }
        return javaUrl;
    }
}
