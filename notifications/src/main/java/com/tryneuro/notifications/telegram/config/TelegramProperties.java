package com.tryneuro.notifications.telegram.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "telegram")
public class TelegramProperties {
    private int apiId;
    private String apiHash;
    private String sessionsPath = "./tg-sessions";
}
