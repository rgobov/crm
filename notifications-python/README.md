# Notifications Service (Python)

FastAPI сервис с Pyrogram для Telegram уведомлений. Полная бизнес-логика перенесена из Java сервиса.

## Особенности

- **Бесконечная сессия** - нет 24-часового ограничения TDLib
- **Меньше overhead** - чем TDLight/TDLib
- **Async/await** - асинхронная обработка
- **FastAPI** - современный и быстрый фреймворк
- **Полная бизнес-логика** - перенесена из Java сервиса

## Бизнес-логика

### Управление клиентами
- **Auto-warmup** - автоматический запуск клиентов при старте сервиса
- **Thread-safe storage** - безопасное хранение клиентов в многопоточной среде
- **Session locks** - блокировки для предотвращения race conditions
- **Graceful shutdown** - корректное завершение всех клиентов

### Статусы авторизации
- **WAITING_QR** - ожидание сканирования QR кода
- **WAITING_PASSWORD** - ожидание ввода 2FA пароля
- **CONNECTED** - клиент подключен и готов к работе
- **DISCONNECTED** - клиент отключен

### API Endpoints

#### POST /api/telegram/send-by-phone
Отправить сообщение в Telegram

#### GET /api/telegram/qr
Получить QR код для авторизации

#### DELETE /api/telegram/session
Отключить и удалить сессию

#### POST /api/telegram/connect
Подключить/переподключить клиента

#### POST /api/telegram/password
Проверить 2FA пароль

#### POST /api/telegram/cancel-qr
Отменить генерацию QR кода

### Синхронизация с backend
Автоматическая синхронизация статуса с backend сервисом через HTTP API.

## Локальный запуск

### Требования

- Python 3.11+ (проверено на 3.13)
- pip

### Установка зависимостей

```bash
pip install -r requirements.txt
```

### Конфигурация

Создать `.env` файл:

```bash
cp .env.example .env
```

Отредактировать `.env`:

```env
TELEGRAM_API_ID=your_api_id
TELEGRAM_API_HASH=your_api_hash
BACKEND_SERVICE_URL=http://localhost:8080
INTERNAL_SECRET=try-neuro-internal-secret-2026
```

### Запуск

```bash
python main.py
```

Или через uvicorn:

```bash
uvicorn main:app --host 0.0.0.0 --port 8081 --reload
```

## Docker запуск

```bash
docker build -t notifications-python .
docker run -p 8082:8081 --env-file .env notifications-python
```

## Переключение между Java и Python

В `.env` файле backend сервиса:

```env
NOTIFICATIONS_IMPLEMENTATION=java  # или python
```

Или в docker-compose.yml:

```yaml
environment:
  NOTIFICATIONS_IMPLEMENTATION: python
```

## Отличия от Java версии

| Параметр | Java (TDLight) | Python (Pyrogram) |
|----------|---------------|-------------------|
| Сессия | 24 часа | Бесконечная |
| Memory на клиента | ~50-100 MB | ~30-50 MB |
| Авторизация | QR код (автоматическая) | QR код (ручная) |
| Производительность | Хорошая | Отличная |
