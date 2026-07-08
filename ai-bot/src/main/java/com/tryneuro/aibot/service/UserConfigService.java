package com.tryneuro.aibot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UserConfigService {

    private static final Logger log = LoggerFactory.getLogger(UserConfigService.class);

    private final JdbcTemplate jdbcTemplate;

    public UserConfigService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UserConfig getConfig(long telegramId) {
        log.debug("Querying DB for telegram_id={}", telegramId);

        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT c.api_key, c.llm_model FROM user_ai_config c " +
                "JOIN users u ON u.id = c.user_id WHERE u.telegram_id = ?",
                telegramId
            );

            UserConfig cfg = new UserConfig(
                (String) row.get("api_key"),
                (String) row.get("llm_model")
            );

            log.debug("Found config for telegram_id={}: apiKey={}, model={}",
                telegramId,
                cfg.apiKey() != null ? "***" + cfg.apiKey().substring(Math.max(0, cfg.apiKey().length() - 4)) : "null",
                cfg.llmModel());

            return cfg;
        } catch (Exception e) {
            log.error("Failed to get config for telegram_id={}: {} ({})",
                telegramId, e.getClass().getSimpleName(), e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("Stack trace:", e);
            }
            return null;
        }
    }

    public record UserConfig(String apiKey, String llmModel) {}
}
