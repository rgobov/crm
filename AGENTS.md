# CRM Project — GigaChat Bot (Java Telegram AI Gateway)

## Project Overview
- **Stack**: Spring Boot (Java) + PostgreSQL (pgvector) + Svelte (Node) + Python (FastAPI/Pyrogram for notifications)
- **AI Bot**: GigaChat SDK (ai-bot — **Java 21**, backend — **Java 17**, 4 Telegram бота) — замена Python bot-agent. **Spring AI удалён** (не OpenAI-совместимый API)
- **Build**: Maven (`mvn package`), Python 3.13+, Node for Svelte
- **Deploy**: GitHub Actions → VPS via SSH, docker-compose (push в `feature/spring-ai`, `feature/roles` или `fix/ios-final-attempt`)
- **Proxy**: HTTP CONNECT `87.121.86.253:8888` — только для Telegram (GigaChat напрямую)
- **Network**: `tryneuro_network` (external)

## Language Policy
- Все ответы, комментарии, UI, коммиты, документация — **русский**
- Документация (AGENTS.md, коммиты) — русский.

## Два Telegram-подключения

| Подключение | Тип | Отвечает за |
|---|---|---|
| `notifications-python` (Pyrogram) | **User client** | Отправка уведомлений, авторизация по phone |
| AI-бот (`@NineCRM_AI_1_bot` … `_4`) | **4 бота (Java)** | AI чат, CRM tools, RAG |

- Pyrogram — **не бот**, а user session (авторизация по phone + code)
- AI-бот — один Java-процесс на 4 ботов, **основной AI-бот**
- Python bot-agent (`ai-gateway/bot-agent`) — **DEPRECATED**, пока работает на сервере, но не деплоится

## Architecture

```
User → Telegram (bot_N) → GigaChat Bot (Java, 1 процесс, 4 бота)
                                │
                      Per-user api_key lookup
                      (PostgreSQL user_ai_config, без кеша — читается на каждый запрос)
                                │
                                ▼
                     GigaChat (ключ + модель пользователя)
                     (через GigaChatClient SDK, verifySslCerts=false)
                                │
                     ┌──────────┴──────────┐
                     ▼                      ▼
                  Backend API             PGvector RAG
         (AiInternalController)   (ai_knowledge_chunks)
```

- **Per-user auth**: `GigaChatClient.builder().authClient(AuthClient.builder().withOAuth(scope, authKey))` из `user_ai_config.api_key`
- **ReAct loop** (ручной): `GigaChatClient.completions(...)` → LLM → function_call → `CrmToolService.executeTool()` → continue → ответ (до 8 итераций, таймаут 30 сек)
- **Auto-RAG**: перед каждым LLM вызовом в system prompt добавляется контекст из PGvector поиска
- **keep_typing**: фоновая задача `chat_action="typing"` каждые 4 сек
- **resolve_actor**: `GET /users/by-telegram/{chatId}` → role, tenant_id, contact_id, staff_id
- **MCP server** (`mcp-crm`) — оставлен для future external integration (не используется)

## Ролевая матрица AI Tools

Инструменты в `CrmToolService.java` (getToolDefinitions), роли проверяются в `AiInternalController.java`:

| Инструмент | ADMIN | MANAGER | EMPLOYEE | CLIENT |
|---|---|---|---|---|
| `search_contacts` | ✅ | ✅ | ✅ | ❌ |
| `get_contact` | ✅ | ✅ | ✅ | ❌ |
| `create_contact` | ✅ | ✅ | ❌ | ❌ |
| `update_contact` | ✅ | ✅ | ❌ | ❌ |
| `delete_contact` | ✅ | ✅ | ❌ | ❌ |
| `search_services` | ✅ | ✅ | ✅ | ✅ |
| `add_service` | ✅ | ✅ | ❌ | ❌ |
| `update_service` | ✅ | ✅ | ❌ | ❌ |
| `delete_service` | ✅ | ✅ | ❌ | ❌ |
| `search_staff` | ✅ | ✅ | ✅ | ✅ |
| `get_staff_schedule` | ✅ | ✅ | ✅ | ✅ |
| `search_resources` | ✅ | ✅ | ✅ | ✅ |
| `get_branches` | ✅ | ✅ | ✅ | ✅ |
| `get_instructions` | ✅ | ✅ | ✅ | ✅ |
| `check_availability` | ✅ | ✅ | ✅ | ✅ |
| `create_appointment` | ✅ | ✅ | ✅ | ✅ (себе) |
| `get_appointment` | ✅ | ✅ | ✅ | ✅ (свои) |
| `update_appointment` | ✅ | ✅ | ✅ (свои) | ✅ (свои) |
| `cancel_appointment` | ✅ | ✅ | ✅ (свои) | ✅ (свои) |
| `get_my_appointments` | ✅ (все) | ✅ (все) | ✅ (свои) | ✅ (свои) |
| `manage_notifications` | ❌ | ❌ | ❌ | ✅ |
| `get_report` | ✅ | ✅ | ❌ | ❌ |
| `search_knowledge` | ✅ | ✅ | ✅ | ✅ |
| `search_knowledge_rag` | ✅ | ✅ | ✅ | ✅ |

- EMPLOYEE может создавать записи для любых клиентов, но отменять/изменять — только свои
- CLIENT управляет только своими данными (contactId проверяется)
- Заголовки: `X-Actor-Role`, `X-Actor-Contact-Id`, `X-Actor-Staff-Id`, `X-Tenant-Id`
- snake_case от LLM → маппинг в camelCase в `execute_tool()` (CrmToolService.java)

## AiInternalController — эндпоинты

Базовый путь: `/api/admin/ai/internal`

| Метод | Путь | Роли |
|---|---|---|
| POST | `/contacts/search` | ADMIN, MANAGER, EMPLOYEE |
| POST | `/contacts` | ADMIN, MANAGER |
| GET | `/contacts/{id}` | ADMIN, MANAGER, EMPLOYEE |
| PUT | `/contacts/{id}` | ADMIN, MANAGER |
| DELETE | `/contacts/{id}` | ADMIN, MANAGER |
| POST | `/services/search` | любые |
| POST | `/services` | ADMIN, MANAGER |
| PUT | `/services/{id}` | ADMIN, MANAGER |
| DELETE | `/services/{id}` | ADMIN, MANAGER |
| POST | `/staff/search` | любые |
| POST | `/staff/schedule` | любые |
| GET | `/branches` | любые |
| GET | `/instructions` | любые |
| POST | `/availability` | любые |
| POST | `/appointments` | ADMIN, MANAGER, EMPLOYEE, CLIENT |
| GET | `/appointments/{id}` | ADMIN, MANAGER, EMPLOYEE, CLIENT |
| PUT | `/appointments/{id}` | ADMIN, MANAGER, EMPLOYEE, CLIENT |
| DELETE | `/appointments/{id}` | ADMIN, MANAGER, EMPLOYEE, CLIENT |
| GET | `/appointments/my` | ADMIN, MANAGER, EMPLOYEE, CLIENT |
| GET/PUT | `/notifications/preferences` | CLIENT only |
| GET | `/users/by-telegram/{id}` | внутренний |
| GET | `/tenant/by-telegram/{id}` | внутренний |
| GET | `/user-config/{userId}` | внутренний |
| POST | `/telegram/bind` | внутренний |
| POST | `/reports` | ADMIN, MANAGER |
| POST | `/knowledge/search` | любые |
| POST | `/knowledge/rag-search` | внутренний (X-Internal-Secret) |
| POST | `/knowledge/ingest` | внутренний (X-Internal-Secret) |
| POST | `/knowledge/reindex` | внутренний (X-Internal-Secret) |

## Files in the chain

| Layer | File | Role |
|---|---|---|
| Frontend | `frontend-svelte/src/lib/services/aiService.js` | API to backend |
| Frontend | `frontend-svelte/src/routes/admin/settings/ai/+page.svelte` | AI config UI |
| Backend | `.../controller/svelte/AiConfigController.java` | AI config CRUD |
| Backend | `.../controller/svelte/AiInternalController.java` | Internal API (CRM tools + RAG) |
| Backend | `.../controller/svelte/AiKnowledgeController.java` | Knowledge base CRUD |
| Backend | `.../dto/ai/*.java` | Request DTOs (9 шт) |
| Backend (RAG) | `.../service/EmbeddingService.java` | OpenRouter text-embedding-ada-002 |
| Backend (RAG) | `.../service/RagSearchService.java` | PGvector cosine similarity search |
| Backend (RAG) | `.../service/KnowledgeIngestService.java` | Chunking (512/50 токенов) + вставка |
| Backend (Flyway) | `.../V40__pgvector.sql` | pgvector extension + ai_knowledge_chunks |
| Bot Agent (GigaChat) | `ai-bot/.../CrmToolService.java` | 24 tool definitions (getToolDefinitions) + executeTool |
| Bot Agent (GigaChat) | `ai-bot/.../AiAgentService.java` | GigaChatClient + ручной ReAct + auto-RAG |
| Bot Agent (GigaChat) | `ai-bot/.../RagService.java` | RAG search call to backend |
| Bot Agent (GigaChat) | `ai-bot/.../UserConfigService.java` | PostgreSQL lookup (без кеша) |
| Bot Agent (GigaChat) | `ai-bot/.../MapResolverService.java` | resolve actor by telegramId |
| Bot Agent (GigaChat) | `ai-bot/.../TryNeuroBot.java` | 4 TelegramLongPollingBot + команды |
| Bot Agent (GigaChat) | `ai-bot/.../BotInitializer.java` | Регистрация 4 ботов + proxy |
| Bot Agent (GigaChat) | `ai-bot/.../AppConfig.java` | RestTemplate + JdbcTemplate бины |
| Bot Agent (Python) | `ai-gateway/bot-agent/*` | **DEPRECATED** (не деплоится) |
| Notifications | `notifications-python/main.py` | Telegram notifications (Pyrogram) |

## Secrets/env

- `BOT_TOKEN_1` … `BOT_TOKEN_4` — Telegram bot tokens
- `SPRING_DATASOURCE_URL` — PostgreSQL JDBC URL (`jdbc:postgresql://tryneuro_db:5432/tryneuro_db`)
- `TELEGRAM_PROXY` — HTTP CONNECT proxy (обязателен на VPS в РФ), также используется для OpenRouter
- `INTERNAL_SECRET` — shared secret для backend ↔ bot
- `CRM_BACKEND_URL` — URL backend для ai-bot (`http://backend:8080`)
- `GIGACHAT_SCOPE` — скоуп GigaChat OAuth (`GIGACHAT_API_PERS` по умолчанию)
- Новые env var добавляются в **нужные** workflow: deploy-main.yml / deploy-spring-ai.yml

## Deploy Workflows

| Workflow | Branch | Path Filter | Деплоит |
|---|---|---|---|
| `deploy-main.yml` | `feature/spring-ai`, `feature/roles`, `fix/ios-final-attempt` | `paths-ignore: ai-gateway/**, ai-bot/**, *.md, deploy-ai-gateway.yml, deploy-spring-ai.yml` | backend, notifications, frontend |
| `deploy-spring-ai.yml` | `feature/spring-ai` | `paths: ai-bot/**` | spring-ai бот (tryneuro_spring_ai_bot) |
| `deploy-ai-gateway.yml` | `feature/roles`, `fix/ios-final-attempt` | `paths: ai-gateway/**` | Python bot-agent, mcp-crm **(DEPRECATED)** |

- Все три используют `concurrency.group: deploy-vps` → не выполняются одновременно.
- `notifications-python` деплоится ТОЛЬКО через `deploy-main.yml`.
- `deploy-spring-ai.yml` запускается ТОЛЬКО при изменении `ai-bot/**` (не дублируется с deploy-main.yml).

## Database Schema (Flyway)

- **Tool**: Flyway (`spring.flyway.enabled=true`)
- **Location**: `backend/src/main/resources/db/migration/V*__.sql`
- **Table naming**: snake_case + plural
- **pgvector**: образ БД `pgvector/pgvector:0.7.4-pg15` (PostgreSQL 15 + vector extension)
- **Таблицы**: `users`, `staff_members`, `appointments`, `user_ai_config`, `ai_knowledge`, `ai_knowledge_chunks`
- **ai_knowledge_chunks**: `id UUID`, `tenant_id VARCHAR(36)`, `knowledge_id VARCHAR(36)`, `chunk_index INT`, `content TEXT`, `embedding vector(1536)`, `metadata JSONB`
- **Индекс**: ivfflat на `embedding` (vector_cosine_ops, lists=100)
- **Роли**: `UserRole` enum — `ADMIN`, `MANAGER`, `EMPLOYEE`, `CLIENT`

## Pyrogram-specific rules (notifications-python/main.py)

- `SentCode` has: `type`, `phone_code_hash`, `next_type`, `timeout` — NO `phone_registered`
- `User` has: `id`, `username`, `first_name`, `last_name`, `phone`
- `Message` has: `id`, `date`, `outgoing`
- Imports: `FloodWait`, `PhoneCodeExpired`, `PhoneCodeInvalid`, `PhoneNumberInvalid`, `PhoneNumberFlood`, `PhoneNumberBanned`, `SessionPasswordNeeded`, `ApiIdInvalid`

## Rollback при неудачном деплое

Если деплой сломал прод:

**1. Откат кода + передеплой** (самое быстрое):
```bash
git revert HEAD --no-edit
git push origin feature/spring-ai    # или feature/roles — в зависимости от активной ветки
# GitHub Actions сам передеплоит предыдущую версию
```

**2. Откат БД** (если Flyway сломал схему):
```bash
ssh user@server
ls ~/crm/backups/db-*.sql                     # найти дамп до деплоя
cat ~/crm/backups/db-2026-06-26_2200.sql | docker exec -i tryneuro_db psql -U postgres -d tryneuro_db
```

**3. Health check:** если деплой прошёл, но backend не запустился — workflow упадёт с `exit 1` до очистки образов.

**Безопасность:** `docker image prune -a --filter "until=24h"` — образы младше 24ч не трогаются, можно откатиться через `docker-compose up -d` с образом из кеша.

## Build & Deploy

**Только через GitHub Actions:**
- Push в `feature/spring-ai` → автодеплой Spring AI стека
- Push в `feature/roles` или `fix/ios-final-attempt` → автодеплой основного стека
- **Проверка после деплоя:**
  - `docker logs tryneuro_backend --tail 20`
  - `docker logs tryneuro_spring_ai_bot --tail 20`
  - `docker logs tryneuro_notifications_python --tail 20`

## TODO (future)
- **External MCP** — публичный MCP-сервер для сторонних разработчиков
- **Streaming** — ответ чанками через `editMessageText`
- **Redis** — для session state и персистентности диалогов
- **MAX Messenger** — интеграция уведомлений (ждёт верифицированное юрлицо РФ)
- **Удаление Python bot-agent** — после стабилизации GigaChat ai-bot
