# CRM Project — Bot Agent (Python Telegram AI Gateway)

## Project Overview
- **Stack**: Spring Boot (Java) + PostgreSQL + Svelte (Node) + Python (FastAPI/Pyrogram for notifications, custom bot-agent for AI)
- **Deploy**: GitHub Actions → VPS via SSH, docker-compose (только через push в ветку `feature/roles` или `fix/ios-final-attempt`)
- **Proxy**: HTTP CONNECT `87.121.86.253:8888` — optional, только если нужен доступ к Telegram из РФ
- **Network**: `tryneuro_network` (external)

## Language Policy
- **Все ответы должны быть на русском языке**, если иное не указано.
- Комментарии в коде, UI тексты, сообщения ботов — русский.
- Документация (AGENTS.md, коммиты) — русский.

## Two Telegram Connections

| Connection | Type | Responsible for |
|---|---|---|
| `notifications-python` (Pyrogram) | **User client** | Sending notifications, auth by phone number |
| Bot Agent (`@NineCRM_AI_1_bot` … `_4`) | **4 bots** | AI chat, CRM tools (contacts, appointments), sharded by `chat_id % 4` |

- The Pyrogram user client is NOT a bot — it's a Telegram user session authorized by phone + code
- Bot Agent runs as a single Python process handling 4 bots

## Architecture: Python Bot Agent + Direct OpenRouter

```
User → Telegram (bot_N) → bot-agent (Python, 1 process, 4 bots)
                                │
                      Per-user api_key lookup
                      (PostgreSQL user_ai_config)
                                │
                                ▼
                        OpenRouter (ключ пользователя + его модель)
                                │
                                ▼
                    ┌───────────────────────┐
                    │ Backend API (CRM tools)│
                    │ contacts, appointments,│
                    │ services, staff, etc.  │
                    └───────────────────────┘
```

- **Bot Agent** (`ai-gateway/bot-agent/`) — Python asyncio, 4 Telegram bot instances (каждый слушает свои сообщения напрямую от Telegram, шардирование не требуется)
- **Per-user auth**: `AsyncOpenAI(api_key=user_cfg["api_key"])` — читает из PostgreSQL `user_ai_config`
- **CRM tools**: ReAct loop с OpenAI function calling → вызов Backend API напрямую (без MCP)
- **keep_typing**: фоновая задача отправляет `chat_action="typing"` каждые 4 сек пока ждёт ответ от OpenRouter
- **resolve_actor**: получает из Backend API роль, tenant_id, contact_id, staff_id через `X-Internal-Secret`. Backend возвращает camelCase → маппинг в snake_case в `tools.py`
- **MCP server** (`mcp-crm`) — оставлен для future external integration

## Flow: Per-User AI Config

1. Пользователь сохраняет API key/model в CRM → backend пишет в `user_ai_config`
2. Пользователь пишет боту → bot-agent получает `chat_id`
3. `db.py` читает `api_key` и `llm_model` из PostgreSQL по `telegram_id` (кэш 5 мин)
4. `agent.py` создаёт `AsyncOpenAI(api_key=..., base_url=openrouter.ai)` с ключом пользователя
5. ReAct цикл: LLM → tool call (если нужно) → Backend API → continue → ответ
6. Ответ отправляется в Telegram

## Files in the chain

| Layer | File | Role |
|---|---|---|---|
| Frontend | `frontend-svelte/src/lib/services/aiService.js` | API calls to backend |
| Frontend | `frontend-svelte/src/routes/admin/settings/ai/+page.svelte` | AI config UI (api_key, model, telegram_id, knowledge base) |
| Backend | `backend/.../controller/svelte/AiConfigController.java` | User AI config CRUD |
| Backend | `backend/.../controller/svelte/AiInternalController.java` | Internal API for CRM tools |
| Backend | `backend/.../controller/svelte/AiKnowledgeController.java` | Knowledge base CRUD (tenant-scoped) |
| Backend | `backend/.../model/UserAiConfig.java` | Per-user AI config entity |
| Backend | `backend/.../model/AiKnowledge.java` | Knowledge base entry entity |
| Backend | `backend/.../service/AiKnowledgeService.java` | Knowledge base logic |
| Backend | `backend/.../repository/AiKnowledgeRepository.java` | Knowledge base DB access |
| Bot Agent | `ai-gateway/bot-agent/main.py` | 4 Telegram bots, message handlers, conv. history |
| Bot Agent | `ai-gateway/bot-agent/agent.py` | ReAct loop: OpenRouter + tool execution |
| Bot Agent | `ai-gateway/bot-agent/tools.py` | Tool schemas + Backend API calls |
| Bot Agent | `ai-gateway/bot-agent/db.py` | PostgreSQL cache for user AI config |
| Bot Agent | `ai-gateway/bot-agent/config.py` | Environment variables |
| Python | `notifications-python/main.py` | Telegram user client (Pyrogram) — notifications |
| MCP | `ai-gateway/mcp-crm/server.py` | MCP tools server (port 8000) — keep for external use |
| Deploy | `.github/workflows/deploy-main.yml` | Main stack (backend, notifications, frontend) |
| Deploy | `.github/workflows/deploy-openclaw.yml` | AI Gateway (bot-agent + mcp-crm) |

## Secrets/env

- `BOT_TOKEN_1` … `BOT_TOKEN_4` — Telegram bot tokens (GitHub secrets)
- `DATABASE_URL` — PostgreSQL connection
- `TELEGRAM_PROXY` — HTTP CONNECT proxy (обязателен на VPS в России)
- `INTERNAL_SECRET` — shared secret для backend ↔ bot communication

Новые env var добавляются в deploy-main.yml И deploy-openclaw.yml.

## Deploy Workflows (GitHub Actions)

**Два раздельных workflow:**

| Workflow | Branch Trigger | Path Filter | Деплоит |
|----------|--------------|-------------|--------|
| `deploy-main.yml` | `feature/roles`, `fix/ios-final-attempt` | `paths-ignore: 'ai-gateway/**', '.github/workflows/deploy-openclaw.yml', 'AGENTS.md', '*.md'` | backend, notifications, frontend, database |
| `deploy-openclaw.yml` | `feature/roles`, `fix/ios-final-attempt` | `paths: 'ai-gateway/**', '.github/workflows/deploy-openclaw.yml'` | bot-agent, mcp-crm |

**Изоляция:** оба workflow используют `concurrency.group: deploy-vps` → не выполняются одновременно.

## Database Schema (Flyway)

- **Tool**: Flyway — Spring Boot auto-config (`spring.flyway.enabled=true`)
- **Location**: `backend/src/main/resources/db/migration/V*__.sql`
- **Table naming**: все таблицы в `snake_case` + plural:
  - `users` (**НЕ** `"user"`, а `"users"`) — CRM пользователи
  - `user_ai_config` — per-user AI config (V35)
  - `ai_knowledge` — knowledge base entries (V38)
  - `staff_members`, `contacts`, `appointments` и т.д.

## Pyrogram-specific rules (notifications-python/main.py)

When editing `notifications-python/main.py`:

1. **Verify every attribute access** against the installed Pyrogram version:
   - `SentCode` has ONLY: `type`, `phone_code_hash`, `next_type`, `timeout` — NO `phone_registered`
   - `User` has: `id`, `username`, `first_name`, `last_name`, `phone`
   - `Message` has: `id`, `date`, `outgoing`
2. **Import specific exceptions**: `FloodWait`, `PhoneCodeExpired`, `PhoneCodeInvalid`, `PhoneNumberInvalid`, `PhoneNumberFlood`, `PhoneNumberBanned`, `SessionPasswordNeeded`, `ApiIdInvalid`

## Build & Deploy

**Только через GitHub Actions:**
- Push в ветку `feature/roles` или `fix/ios-final-attempt` → автоматический деплой
- Workflow определяется по изменённым путям (см. Deploy Workflows выше)

**Проверка после деплоя:**
- `docker logs tryneuro_bot_agent --tail 20` — проверка bot-agent
- `docker logs tryneuro_mcp_crm --tail 20` — проверка MCP сервера

## TODO (future)
- **External MCP** — публичный MCP-сервер для сторонних разработчиков (HTTPS, API ключи, документация)
- **Streaming** — ответ чанками через `editMessageText`
- **Knowledge Base** — интеграция с AI Settings KB (search для bot-agent)
- **Redis** — для session state и персистентности диалогов
