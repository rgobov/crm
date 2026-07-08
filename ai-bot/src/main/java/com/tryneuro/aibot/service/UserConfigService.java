package com.tryneuro.aibot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserConfigService {

    private static final Logger log = LoggerFactory.getLogger(UserConfigService.class);

    private final JdbcTemplate jdbcTemplate;
    private final Map<Long, UserConfig> cache = new ConcurrentHashMap<>();
    private final Map<Long, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 5 * 60 * 1000;

    public UserConfigService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UserConfig getConfig(long telegramId) {
        Long cached = cacheTimestamps.get(telegramId);
        if (cached != null && System.currentTimeMillis() - cached < CACHE_TTL_MS) {
            return cache.get(telegramId);
        }

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

            cache.put(telegramId, cfg);
            cacheTimestamps.put(telegramId, System.currentTimeMillis());
            return cfg;
        } catch (Exception e) {
            log.warn("User config not found for telegram_id={}: {}", telegramId, e.getMessage());
            return null;
        }
    }

    public void invalidate(long telegramId) {
        cache.remove(telegramId);
        cacheTimestamps.remove(telegramId);
    }

    public record UserConfig(String apiKey, String llmModel) {}
}
