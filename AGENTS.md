# CRM Project — Hermes Profiles (Multiplexed Gateway)

## Project Overview
- **Stack**: Spring Boot (Java) + PostgreSQL + Svelte (Node) + Python (FastAPI/Pyrogram)
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
| Hermes (`@NineCRM_AI_bot_1` … `_4`) | **4 bots** | AI chat, MCP tools (contacts, appointments), sharded by `chat_id % 4` |

- The Pyrogram user client is NOT a bot — it's a Telegram user session authorized by phone + code
- Hermes runs as a **multiplexed gateway** (1 process, 4 profiles/shard)

## Architecture: Hermes Profiles (Multiplexed Gateway)

```
User → Telegram (bot_N) → Hermes gateway (1 process, multiplexed)
                                │
                    ┌─────────────┼─────────────┐
                    ▼           ▼           ▼
               shard_1      shard_2      shard_3/4
                    │           │           │
                    └───────────┼─────────────┘
                                ▼
                      pre_llm_call hook (трyneuro-user-config)
                                │
                                ▼
                        PostgreSQL (user_ai_config)
                        JOIN users ON user_id
                        WHERE users.telegram_id = chat_id
                                │
                                ▼
                        OpenRouter (ключ юзера + модель)
```

- **No mcp-crm LLM proxy** — Hermes profiles call OpenRouter directly
- **No ai-knowledge-service** — удалён (заменён хуком + PostgreSQL)
- **MCP tools** — `mcp-crm` служит MCP сервером (порт 8000)
- **Native plugins** — используется `pre_llm_call` hook, не monkey-patch

## Files in the chain

| Layer | File | Role |
|---|---|---|
| Frontend | `frontend-svelte/src/lib/services/telegramService.js` | API вызовы к backend |
| Frontend | `frontend-svelte/src/lib/components/admin/TelegramSettingsModal.svelte` | UI авторизации (код, flood wait) |
| Backend | `backend/.../controller/svelte/AiConfigController.java` | User AI config (model, api_key) |
| Backend | `backend/.../controller/svelte/AiInternalController.java` | Internal API для MCP инструментов |
| Backend | `backend/.../model/UserAiConfig.java` | Per-user AI config entity |
| Hermes | `hermes-agent/config.yaml` | Global multiplexed config + routes (4 shards) |
| Hermes | `hermes-agent/profiles/shard_N/config.yaml` | Per-shard profile config (provider: custom, base_url: openrouter.ai) |
| Hermes | `hermes-agent/profiles/shard_N/plugins/tryneuro-user-config/` | **pre_llm_call hook plugin** — инъекция model/api_key из PostgreSQL |
| Hermes | `hermes-agent/profiles/shard_N/SOUL.md` | Персонality агента с инструкциями CRM инструментов |
| MCP | `hermes-agent/mcp-crm/server.py` | MCP tools server (порт 8000) — contacts, appointments, services |
| Python | `notifications-python/main.py` | Telegram клиент (Pyrogram) — уведомления |
| Script | `hermes-agent/scripts/create_shard_profile.py` | Генератор профилей из PostgreSQL |
| Deploy | `.github/workflows/deploy-main.yml` | Деплой основного стека (backend, notifications, frontend) |
| Deploy | `.github/workflows/deploy-hermes.yml` | Деплой Hermes стека + генерация профилей |

## Sharding Strategy

- **4 Telegram bots**: `@NineCRM_AI_1_bot` … `@NineCRM_AI_4_bot`
- **User assignment**: `chat_id % 4` — детерминированно, один пользователь → один бот всегда
- **Each bot has its own profile**: `shard_1` … `shard_4`
- **~30 msg/sec per bot** → ~120 msg/sec total
- **Routes in config.yaml** map `bot_token` → `agent_id` (shard)

## Dynamic AI Config (без per-user .env)

Пользователи сохраняют OpenRouter API key + model в CRM UI → записывается в `user_ai_config` (PostgreSQL).
Hermes профили используют `pre_llm_call` hook plugin, который читает конфиг из PostgreSQL (кэш 5 мин).

**Flow:**
1. Пользователь сохраняет API key/model в CRM → backend пишет в PostgreSQL
2. Пользователь пишет боту Hermes → gateway роутит в shard профиль
3. `tryneuro-user-config` hook срабатывает → читает `user_ai_config` по `telegram_id`
4. Hook инъектирует `model` и `api_key` в контекст LLM запроса
5. Hermes вызывает OpenRouter с ключом пользователя

## Secrets/env

- `BOT_TOKEN_1` … `BOT_TOKEN_4` — Telegram bot tokens (GitHub secrets)
- `DATABASE_URL` — PostgreSQL connection для profile hook
- `TELEGRAM_PROXY` — **опционально** — HTTP CONNECT proxy (пусто = без прокси)
- `INTERNAL_SECRET` — shared secret для backend ↔ MCP communication

Новые env var добавляются в deploy-main.yml И deploy-hermes.yml.

## Deploy Workflows (GitHub Actions)

**Два разделенных workflow:**

| Workflow | Branch Trigger | Path Filter | Деплоит |
|----------|--------------|-------------|--------|
| `deploy-main.yml` | `feature/roles`, `fix/ios-final-attempt` | `paths-ignore: 'hermes-agent/**'` | backend, notifications, frontend, database |
| `deploy-hermes.yml` | `feature/roles`, `fix/ios-final-attempt` | `paths: 'hermes-agent/**'` | mcp-crm, hermes-agent, генерацию профилей |

**Изоляция:** оба workflow используют `concurrency.group: deploy-vps` → не выполняются одновременно.
**Деплой Hermes НЕ ломает CRM и уведомления** — workflow отдельные.

**Текущая ветка:** `feature/roles`

## Database Schema (Flyway)

- **Tool**: Flyway — Spring Boot auto-config (`spring.flyway.enabled=true`)
- **Location**: `backend/src/main/resources/db/migration/V*__.sql`
- **Current latest**: `V37__Add_User_Id_To_Staff_Member.sql` (2026-06-24)
- **Table naming**: все таблицы в `snake_case` + plural:
  - `users` (**НЕ** `"user"`, а `"users"`) — CRM пользователи
  - `user_ai_config` — per-user AI config (V35)
  - `staff_members`, `contacts`, `appointments` и т.д.
- **Схема создаётся при старте `backend`** (не через `ddl-auto`, а Flyway)
- **Hermes скрипты** (`create_shard_profile.py`) должны использовать `"users"`, а не `"user"` (PostgreSQL reserved word)

## Pyrogram-specific rules (notifications-python/main.py)

When editing `notifications-python/main.py`:

1. **Verify every attribute access** against the installed Pyrogram version:
   - `SentCode` has ONLY: `type`, `phone_code_hash`, `next_type`, `timeout` — NO `phone_registered`
   - `User` has: `id`, `username`, `first_name`, `last_name`, `phone`
   - `Message` has: `id`, `date`, `outgoing`
2. **Check via `pip show pyrogram`** or read Pyrogram source before accessing a new attribute
3. **Import specific exceptions**: `FloodWait`, `PhoneCodeExpired`, `PhoneCodeInvalid`, `PhoneNumberInvalid`, `PhoneNumberFlood`, `PhoneNumberBanned`, `SessionPasswordNeeded`, `ApiIdInvalid`

## Build & Deploy

**Только через GitHub Actions:**
- Push в ветку `feature/roles` или `fix/ios-final-attempt` → автоматический деплой
- Workflow определяется по изменённым путям (см. Deploy Workflows выше)

**Проверка после деплоя:**
- `docker logs tryneuro_hermes --tail 50 | grep "tryneuro-user-config"` — проверка хука
- `docker logs tryneuro_mcp_crm --tail 20` — проверка MCP сервера

## Hermes

- **Source**: https://github.com/NousResearch/hermes-agent
- **Profiles docs**: https://hermes-agent.nousresearch.com/docs/user-guide/profiles
- **Multiplexed gateway**: https://hermes-agent.nousresearch.com/docs/user-guide/multi-profile-gateways