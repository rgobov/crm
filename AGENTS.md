# CRM Project — Bot Agent (Python Telegram AI Gateway)

## Project Overview
- **Stack**: Spring Boot (Java) + PostgreSQL + Svelte (Node) + Python (FastAPI/Pyrogram for notifications, custom bot-agent for AI)
- **Build**: Maven (`mvn package`), Python 3.13+, Node for Svelte
- **Deploy**: GitHub Actions → VPS via SSH, docker-compose (только push в `feature/roles` или `fix/ios-final-attempt`)
- **Proxy**: HTTP CONNECT `87.121.86.253:8888` — опционально
- **Network**: `tryneuro_network` (external)

## Language Policy
- Все ответы, комментарии, UI, коммиты, документация — **русский**
- Документация (AGENTS.md, коммиты) — русский.

## Two Telegram Connections

| Connection | Type | Responsible for |
|---|---|---|
| `notifications-python` (Pyrogram) | **User client** | Sending notifications, auth by phone |
| Bot Agent (`@NineCRM_AI_1_bot` … `_4`) | **4 bots** | AI chat, CRM tools |

- Pyrogram — **не бот**, а user session (авторизация по phone + code)
- Bot Agent — один Python asyncio процесс на 4 ботов

## Architecture

```
User → Telegram (bot_N) → bot-agent (Python, 1 process, 4 bots)
                                │
                      Per-user api_key lookup
                      (PostgreSQL user_ai_config)
                                │
                                ▼
                        OpenRouter (ключ + модель пользователя)
                                │
                                ▼
              Backend API (AiInternalController — CRM tools)
```

- **Per-user auth**: `AsyncOpenAI(api_key=user_cfg["api_key"])` из `user_ai_config`
- **ReAct loop**: LLM → tool call (function calling) → Backend API → continue → ответ
- **keep_typing**: фоновая задача `chat_action="typing"` каждые 4 сек
- **resolve_actor**: `GET /users/by-telegram/{chatId}` → role, tenant_id, contact_id, staff_id
- **MCP server** (`mcp-crm`) — оставлен для future external integration

## Ролевая матрица AI Tools

Инструменты в `ai-gateway/bot-agent/tools.py` (TOOL_SCHEMAS), роли проверяются в `AiInternalController.java`:

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
| `get_branches` | ✅ | ✅ | ✅ | ✅ |
| `check_availability` | ✅ | ✅ | ✅ | ✅ |
| `create_appointment` | ✅ | ✅ | ✅ | ✅ (себе) |
| `get_appointment` | ✅ | ✅ | ✅ | ✅ (свои) |
| `update_appointment` | ✅ | ✅ | ✅ (свои) | ✅ (свои) |
| `cancel_appointment` | ✅ | ✅ | ✅ (свои) | ✅ (свои) |
| `get_my_appointments` | ✅ (все) | ✅ (все) | ✅ (свои) | ✅ (свои) |
| `manage_notifications` | ❌ | ❌ | ❌ | ✅ |
| `get_report` | ✅ | ✅ | ❌ | ❌ |
| `search_knowledge` | ✅ | ✅ | ✅ | ✅ |

- EMPLOYEY может создавать записи для любых клиентов, но отменять/изменять — только свои
- CLIENT управляет только своими данными (contactId проверяется)
- Заголовки: `X-Actor-Role`, `X-Actor-Contact-Id`, `X-Actor-Staff-Id`, `X-Tenant-Id`

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

**Важно:** snake_case от LLM → маппинг в camelCase в `execute_tool()` (tools.py:390-500)

## Files in the chain

| Layer | File | Role |
|---|---|---|
| Frontend | `frontend-svelte/src/lib/services/aiService.js` | API to backend |
| Frontend | `frontend-svelte/src/routes/admin/settings/ai/+page.svelte` | AI config UI |
| Backend | `.../controller/svelte/AiConfigController.java` | AI config CRUD |
| Backend | `.../controller/svelte/AiInternalController.java` | Internal API (CRM tools) |
| Backend | `.../controller/svelte/AiKnowledgeController.java` | Knowledge base CRUD |
| Backend | `.../dto/ai/*.java` | Request DTOs (7 шт) |
| Bot Agent | `ai-gateway/bot-agent/tools.py` | Tool schemas + Backend calls |
| Bot Agent | `ai-gateway/bot-agent/agent.py` | ReAct loop |
| Bot Agent | `ai-gateway/bot-agent/main.py` | 4 bot instances, handlers |
| Bot Agent | `ai-gateway/bot-agent/db.py` | PostgreSQL cache (5 мин) |
| Bot Agent | `ai-gateway/bot-agent/config.py` | Env vars |
| Python | `notifications-python/main.py` | Telegram notifications (Pyrogram) |

## Secrets/env

- `BOT_TOKEN_1` … `BOT_TOKEN_4` — Telegram bot tokens
- `DATABASE_URL` — PostgreSQL
- `TELEGRAM_PROXY` — HTTP CONNECT proxy (обязателен на VPS в РФ)
- `INTERNAL_SECRET` — shared secret для backend ↔ bot
- Новые env var добавляются в **оба** workflow: deploy-main.yml + deploy-ai-gateway.yml

## Deploy Workflows

| Workflow | Branch | Path Filter | Деплоит |
|---|---|---|---|
| `deploy-main.yml` | `feature/roles`, `fix/ios-final-attempt` | `paths-ignore: 'ai-gateway/**'` | backend, notifications, frontend |
| `deploy-ai-gateway.yml` | `feature/roles`, `fix/ios-final-attempt` | `paths: 'ai-gateway/**'` | bot-agent, mcp-crm |
| `deploy-spring-ai.yml` | `feature/spring-ai` | `paths: 'ai-bot/**'` | spring-ai бот (tryneuro_spring_ai_bot) |

- `deploy-main.yml`, `deploy-ai-gateway.yml` — имеют `workflow_dispatch` (ручной запуск из GitHub UI)
- Все три используют `concurrency.group: deploy-vps` → не выполняются одновременно.
- `notifications-python` деплоится ТОЛЬКО через `deploy-main.yml` и никак не связан с ai-gateway/ai-bot.

## Database Schema (Flyway)

- **Tool**: Flyway (`spring.flyway.enabled=true`)
- **Location**: `backend/src/main/resources/db/migration/V*__.sql`
- **Table naming**: snake_case + plural (`users`, `staff_members`, `appointments`, `user_ai_config`, `ai_knowledge`)
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
git push origin feature/roles
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
- Push в `feature/roles` или `fix/ios-final-attempt` → автодеплой
- **Проверка после деплоя:**
  - `docker logs tryneuro_bot_agent --tail 20`
  - `docker logs tryneuro_notifications_python --tail 20`

## TODO (future)
- **External MCP** — публичный MCP-сервер для сторонних разработчиков
- **Streaming** — ответ чанками через `editMessageText`
- **Redis** — для session state и персистентности диалогов
- **MAX Messenger** — интеграция уведомлений (ждёт верифицированное юрлицо РФ)
