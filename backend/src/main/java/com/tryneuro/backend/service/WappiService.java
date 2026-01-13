package com.tryneuro.backend.service;

import com.tryneuro.backend.model.Appointment;
import com.tryneuro.backend.model.Contact;
import com.tryneuro.backend.model.StaffMember;
import com.tryneuro.backend.model.WappiSettings;
import com.tryneuro.backend.repository.WappiSettingsRepository;
import com.tryneuro.backend.repository.StaffMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.cert.X509Certificate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WappiService {

    private final WappiSettingsRepository settingsRepository;
    private final StaffMemberRepository staffMemberRepository;
    
    // Создаем RestTemplate с обходом SSL и Hostname для локальных тестов
    private final RestTemplate restTemplate = createUnsecureRestTemplate();

    public WappiSettings saveSettings(String tenantId, WappiSettings newSettings) {
        WappiSettings settings = settingsRepository.findByTenantId(tenantId)
                .orElse(new WappiSettings());
        settings.setTenantId(tenantId);
        settings.setApiKey(newSettings.getApiKey());
        settings.setProfileId(newSettings.getProfileId());
        settings.setEnabled(newSettings.isEnabled());
        settings.setReminderTemplate(newSettings.getReminderTemplate());
        settings.setMessengerType("TELEGRAM");
        settings.setLeadTimeMinutes(newSettings.getLeadTimeMinutes());
        return settingsRepository.save(settings);
    }

    public WappiSettings getSettings(String tenantId) {
        WappiSettings settings = settingsRepository.findByTenantId(tenantId)
                .orElse(new WappiSettings());
        settings.setTenantId(tenantId);
        return settings;
    }

    public void sendTestMessage(String tenantId, String phone) {
        WappiSettings settings = settingsRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new RuntimeException("Настройки не найдены"));
        
        String testText = "🚀 Тестовое сообщение. Нажмите на /confirm для проверки связи!";
        sendToWappiTAPI(settings, phone.replaceAll("[^0-9]", ""), testText);
    }

    public void sendReminder(Appointment appointment, Contact contact) {
        WappiSettings settings = settingsRepository.findByTenantId(appointment.getTenantId())
                .orElse(null);

        if (settings == null || !settings.isEnabled()) return;

        String message = buildMessage(appointment, contact, settings.getReminderTemplate());
        if (contact.getPhones().isEmpty()) return;
        
        String phone = contact.getPhones().get(0).replaceAll("[^0-9]", "");
        sendToWappiTAPI(settings, phone, message);
    }

    private String buildMessage(Appointment appointment, Contact contact, String template) {
        String masterName = staffMemberRepository.findById(appointment.getStaffMemberId())
                .map(StaffMember::getName).orElse("Специалист");

        return template
                .replace("{name}", contact.getName())
                .replace("{service}", appointment.getService())
                .replace("{date}", appointment.getDate().format(DateTimeFormatter.ofPattern("dd.MM")))
                .replace("{time}", appointment.getTime().toString())
                .replace("{master}", masterName);
    }

    private void sendToWappiTAPI(WappiSettings settings, String phone, String text) {
        // Добавляем «команды-кнопки» в конец сообщения
        String finalMessage = text + "\n\n✅ Подтвердить: /confirm\n❌ Отмена: /cancel";

        // Используем асинхронный эндпоинт TAPI для текста
        String url = UriComponentsBuilder.fromHttpUrl("https://api.wappi.pro/tapi/async/message/send")
                .queryParam("profile_id", settings.getProfileId())
                .queryParam("timeout_from", 1)
                .queryParam("timeout_to", 3)
                .toUriString();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", settings.getApiKey());

        Map<String, Object> body = new HashMap<>();
        body.put("recipient", phone);
        body.put("body", finalMessage);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        System.out.println("DEBUG: Sending TAPI text command to: " + phone);
        
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            System.out.println("DEBUG: Wappi Response: " + response.getStatusCode());
        } catch (HttpStatusCodeException e) {
            System.err.println("DEBUG: Wappi API Error: " + e.getResponseBodyAsString());
            throw new RuntimeException("TAPI error: " + e.getStatusCode());
        } catch (Exception e) {
            System.err.println("DEBUG: Connection Error: " + e.getMessage());
            throw new RuntimeException("Connection error");
        }
    }

    private RestTemplate createUnsecureRestTemplate() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
                @Override
                protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                    if (connection instanceof HttpsURLConnection) {
                        ((HttpsURLConnection) connection).setSSLSocketFactory(sc.getSocketFactory());
                        ((HttpsURLConnection) connection).setHostnameVerifier((hostname, session) -> true);
                    }
                    super.prepareConnection(connection, httpMethod);
                }
            };
            return new RestTemplate(factory);
        } catch (Exception e) {
            return new RestTemplate();
        }
    }
}
