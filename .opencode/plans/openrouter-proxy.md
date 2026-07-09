# Фикс 403 OpenRouter: роутинг через Estonia-прокси

## Корень проблемы
- OpenRouter гео-блокирует российские IP. Сервер `87.121.86.253` (РФ) получает `403 Access denied by security policy` на `/api/v1/chat/completions`.
- Проверено: с Windows (другой IP) тот же запрос возвращает `429` (rate limit) — модель рабочая, код бота верный.
- Телеграм работает через `TELEGRAM_PROXY` (`87.121.86.253:8888`) — это **HTTP CONNECT прокси, выходящий через Эстонию** (не-РФ IP).
- HTTP CONNECT туннелирует ЛЮБОЙ HTTPS-трафик → можно переиспользовать тот же прокси для OpenRouter.

## Затронутые компоненты
1. **ai-bot** — `AiAgentService.java` создаёт `OpenAiApi` → зовёт OpenRouter с сервера → 403
2. **backend** — `EmbeddingService.java` зовёт OpenRouter с сервера (эмбеддинги) → тоже 403

## План

### 1. ai-bot: `AiAgentService.java`
Spring AI 1.0.0-M1 `OpenAiApi(String apiKey, String baseUrl, RestClient.Builder, WebClient.Builder, ResponseErrorHandler)` принимает кастомный `RestClient.Builder`.

Добавить метод создания API с прокси:
```java
private OpenAiApi buildOpenAiApi(String apiKey) {
    RestClient.Builder restBuilder = RestClient.builder();
    String proxyUrl = System.getenv("OPENROUTER_PROXY");
    if (proxyUrl == null || proxyUrl.isEmpty()) proxyUrl = System.getenv("TELEGRAM_PROXY");
    if (proxyUrl != null && !proxyUrl.isEmpty()) {
        try {
            URI uri = URI.create(proxyUrl.startsWith("http") ? proxyUrl : "http://" + proxyUrl);
            Proxy proxy = new Proxy(Proxy.Type.HTTP,
                new InetSocketAddress(uri.getHost(), uri.getPort() > 0 ? uri.getPort() : 8888));
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setProxy(proxy);
            restBuilder = RestClient.builder(factory);
            log.info("OpenRouter proxy enabled: {}:{}", uri.getHost(), uri.getPort());
        } catch (Exception e) {
            log.warn("Failed to parse proxy {}: {}", proxyUrl, e.getMessage());
        }
    }
    return new OpenAiApi("https://openrouter.ai/api/v1", apiKey,
        restBuilder, WebClient.builder(), RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER);
}
```

В `processMessage` заменить:
```java
// было:
OpenAiApi openAiApi = new OpenAiApi("https://openrouter.ai/api/v1", cfg.apiKey());
// стало:
OpenAiApi openAiApi = buildOpenAiApi(cfg.apiKey());
```

Импорты: `java.net.Proxy`, `java.net.InetSocketAddress`, `java.net.URI`, `org.springframework.http.client.SimpleClientHttpRequestFactory`, `org.springframework.web.client.RestClient`.

### 2. backend: `EmbeddingService.java`
`RestTemplate` тоже нужно пустить через прокси:
```java
public EmbeddingService(
        @Value("${openrouter.api.key:}") String apiKey,
        @Value("${internal.api.secret:...}") String internalSecret) {
    String proxyUrl = System.getenv("OPENROUTER_PROXY");
    if (proxyUrl == null || proxyUrl.isEmpty()) proxyUrl = System.getenv("TELEGRAM_PROXY");
    if (proxyUrl != null && !proxyUrl.isEmpty()) {
        try {
            URI uri = URI.create(proxyUrl.startsWith("http") ? proxyUrl : "http://" + proxyUrl);
            Proxy proxy = new Proxy(Proxy.Type.HTTP,
                new InetSocketAddress(uri.getHost(), uri.getPort() > 0 ? uri.getPort() : 8888));
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setProxy(proxy);
            this.rest = new RestTemplate(factory);
        } catch (Exception e) {
            this.rest = new RestTemplate();
        }
    } else {
        this.rest = new RestTemplate();
    }
    this.apiKey = ...;
}
```

### 3. deploy-spring-ai.yml
`TELEGRAM_PROXY` уже в `.env`. `OPENROUTER_PROXY` необязателен (fallback на `TELEGRAM_PROXY` в коде). Можно явно добавить:
```bash
OPENROUTER_PROXY=${{ secrets.OPENROUTER_PROXY }}
```
с секретом по умолчанию = `TELEGRAM_PROXY` (или не задавать, тогда код возьмёт `TELEGRAM_PROXY`).

### 4. Проверка
После деплоя на сервере:
```bash
docker logs tryneuro_spring_ai_bot --tail 20
```
Ожидаем: `OpenRouter proxy enabled: 87.121.86.253:8888` и успешный ответ от модели.

Также можно проверить вручную с прокси:
```bash
curl -s -x http://87.121.86.253:8888 \
  -H "Authorization: Bearer sk-or-v1-..." \
  -H "Content-Type: application/json" \
  -d '{"model":"meta-llama/llama-3.3-70b-instruct:free","messages":[{"role":"user","content":"test"}]}' \
  https://openrouter.ai/api/v1/chat/completions
```
Если через прокси вернёт `choices` — фикс подтверждён.
