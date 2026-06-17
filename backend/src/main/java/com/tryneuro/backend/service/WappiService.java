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
        return settingsRepository.findByTenantId(tenantId).orElse(new WappiSettings());
    }

    public void sendTestMessage(String tenantId, String phone) {
        WappiSettings settings = settingsRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new RuntimeException("Настройки не найдены"));
        
        String testText = "🚀 Проверка связи! Ответьте на это сообщение словом 'Да' или 'Нет' для теста системы подтверждений.";
        sendMessage(settings, phone.replaceAll("[^0-9]", ""), testText);
    }

    public void sendReminder(Appointment appointment, Contact contact) {
        WappiSettings settings = settingsRepository.findByTenantId(appointment.getTenantId()).orElse(null);
        if (settings == null || !settings.isEnabled()) return;

        String masterName = staffMemberRepository.findById(appointment.getStaffMemberId())
                .map(StaffMember::getName).orElse("Специалист");

        String text = settings.getReminderTemplate()
                .replace("{name}", contact.getName())
                .replace("{service}", appointment.getService())
                .replace("{date}", appointment.getDate().format(DateTimeFormatter.ofPattern("dd.MM")))
                .replace("{time}", appointment.getTime().toString())
                .replace("{master}", masterName);

        // Добавляем инструкцию по ответу
        text += "\n\nПожалуйста, подтвердите визит ответным сообщением:\n✅ Да (буду)\n❌ Нет (отменить)";

        if (!contact.getPhones().isEmpty()) {
            sendMessage(settings, contact.getPhones().get(0).replaceAll("[^0-9]", ""), text);
        }
    }

    // Универсальный метод отправки сообщения (без лишних оберток кнопок)
    public void sendMessage(WappiSettings settings, String phone, String text) {
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
        body.put("body", text);

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(url, entity, String.class);
            System.out.println("DEBUG: Message sent to " + phone);
        } catch (Exception e) {
            System.err.println("DEBUG: Send error: " + e.getMessage());
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
        } catch (Exception e) { return new RestTemplate(); }
    }
}
