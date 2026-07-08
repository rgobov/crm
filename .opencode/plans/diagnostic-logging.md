# Диагностика "API ключ не найден"

## Проблема
Пользователь настроил API-ключ в CRM, но бот отвечает "У вас не настроен API ключ".

## Гипотезы
1. SQL-запрос (исправлен: JOIN users по u.telegram_id) возвращает 0 строк — telegram_id не совпадает
2. В `user_ai_config` нет записи для этого пользователя
3. Старый Python bot-agent перехватывает сообщения (работает параллельно с теми же токенами)

## План

### 1. Добавить логирование в UserConfigService.java
- Логировать cache hit/miss
- Логировать результат SQL (apiKey маскирован)
- Логировать тип исключения и stack trace при ошибке

Файл: `ai-bot/src/main/java/com/tryneuro/aibot/service/UserConfigService.java`

### 2. Включить DEBUG логирование
Файл: `ai-bot/src/main/resources/application.yml`
```yaml
logging:
  level:
    com.tryneuro.aibot.service.UserConfigService: DEBUG
```

### 3. Показать логи бота в деплое
Файл: `.github/workflows/deploy-spring-ai.yml`
Добавить после `docker-compose ps`:
```bash
echo "--- BOT LOGS ---"
sleep 10
docker logs tryneuro_spring_ai_bot --tail 50
```

### 4. Деплой + проверка
- Закоммитить и запушить
- Пользователь отправляет сообщение боту
- Смотрим в GitHub Actions лог деплоя (docker logs)
- Если нужно — зайти на сервер и выполнить `docker logs tryneuro_spring_ai_bot --tail 50`
