package com.tryneuro.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.net.URLDecoder;

@Service
public class TelegramAuthService {

    @Value("${telegram.bot.token}")
    private String botToken;

    public Map<String, String> validateAndParseData(String initData) throws Exception {
        String cleanToken = botToken.trim();

        // 1. Парсим входящую строку
        // Мы не декодируем всю строку сразу, чтобы не сломать структуру, если внутри JSON есть символы &
        Map<String, String> data = Arrays.stream(initData.split("&"))
                .map(s -> s.split("=", 2))
                .collect(Collectors.toMap(
                        s -> s[0],
                        s -> s[1],
                        (oldV, newV) -> oldV
                ));

        // 2. Извлекаем ожидаемый хэш и УДАЛЯЕМ ТОЛЬКО ЕГО
        String expectedHash = data.remove("hash");
        
        // ВАЖНО: Поле 'signature' МЫ НЕ УДАЛЯЕМ. Оно должно участвовать в проверке hash, если оно пришло.

        // 3. Формируем data_check_string
        // По стандарту Telegram Web App: ключи сортируются, значения ДЕКОДИРУЮТСЯ, разделитель \n
        TreeMap<String, String> sortedData = new TreeMap<>(data);
        String dataCheckString = sortedData.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLDecoder.decode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("\n"));

        // 4. Вычисляем HMAC-SHA256
        // secret_key = HMAC_SHA256(key="WebAppData", message=botToken)
        byte[] secretKey = hmacSha256("WebAppData".getBytes(StandardCharsets.UTF_8), cleanToken.getBytes(StandardCharsets.UTF_8));
        
        // calculated_hash = HMAC_SHA256(key=secretKey, message=dataCheckString)
        byte[] calculatedHashBytes = hmacSha256(secretKey, dataCheckString.getBytes(StandardCharsets.UTF_8));
        String calculatedHash = bytesToHex(calculatedHashBytes);

        if (calculatedHash.equalsIgnoreCase(expectedHash)) {
            System.out.println("--- TELEGRAM VALIDATION SUCCESS ---");
            // Возвращаем декодированные данные для работы
            return data.entrySet().stream().collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> URLDecoder.decode(e.getValue(), StandardCharsets.UTF_8)
            ));
        } else {
            System.err.println("--- TELEGRAM VALIDATION FAILED ---");
            System.err.println("DataCheckString:\n" + dataCheckString);
            System.err.println("Calculated: " + calculatedHash);
            System.err.println("Expected:   " + expectedHash);
            throw new SecurityException("Invalid Telegram data signature");
        }
    }

    private byte[] hmacSha256(byte[] key, byte[] message) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(key, "HmacSHA256");
        sha256_HMAC.init(secret_key);
        return sha256_HMAC.doFinal(message);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
