package com.tryneuro.aibot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.io.IOException;

@Configuration
public class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(1200_000);
        RestTemplate rt = new RestTemplate(factory);
        rt.getInterceptors().add(new ClientHttpRequestInterceptor() {
            @Override
            public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                                ClientHttpRequestExecution execution) throws IOException {
                long start = System.currentTimeMillis();
                String uri = request.getURI().toString();
                String maskedUri = uri.replaceAll("(?<=secret=)[^&]+|(?<=api_key=)[^&]+", "***");
                log.debug("HTTP {} {} len={}", request.getMethod(), maskedUri, body.length);
                try {
                    ClientHttpResponse response = execution.execute(request, body);
                    long elapsed = System.currentTimeMillis() - start;
                    log.debug("HTTP {} {} -> {} ({}ms)", request.getMethod(), maskedUri,
                        response.getStatusCode(), elapsed);
                    return response;
                } catch (IOException e) {
                    long elapsed = System.currentTimeMillis() - start;
                    log.warn("HTTP {} {} failed after {}ms: {}", request.getMethod(), maskedUri, elapsed, e.getMessage());
                    throw e;
                }
            }
        });
        return rt;
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
