# CRM Project — Hermes Profiles (Multiplexed Gateway)

## Project Overview
- **Stack**: Spring Boot (Java) + PostgreSQL + Svelte (Node) + Python (FastAPI/Pyrogram)
- **Deploy**: GitHub Actions → VPS via SSH, docker-compose
- **Proxy**: HTTP CONNECT `87.121.86.253:8888` — all Telegram traffic
- **Network**: `tryneuro_network` (external)

## Two Telegram Connections

| Connection | Type | Responsible for |
|---|---|---|
| `notifications-python` (Pyrogram) | **User client** | Sending notifications, auth by phone number |
| Hermes (`@NineCRM_AI_bot_1` … `_4`) | **4 bots** | AI chat, MCP tools (contacts, appointments), sharded by `chat_id % 4` |

- The Pyrogram user client is NOT a bot — it's a Telegram user session authorized by phone + code
- Hermes runs as a **multiplexed gateway** (1 process, 4 profiles/shard)

## Architecture: Hermes Profiles (Multiplexed Gateway)

```
User → Telegram (bot_N) → Hermes gateway (1 process)
                                │
                    ┌───────────┼───────────┐
                    │           │           │
               shard_1      shard_2     shard_3  shard_4
                    │           │           │
              ┌─────┘    hook: pre_model_request  │
              ▼                                  │
        PostgreSQL (user_ai_config) ← reads model + api_key per user
              │
              ▼
         OpenRouter (with user's api_key/model)
```

- **No mcp-crm LLM proxy** — Hermes profiles call OpenRouter directly
- **No ai-knowledge-service** — removed (replaced by hook + PostgreSQL)
- **MCP tools still needed** — `mcp-crm` serves as MCP server only (port 8000)
- **patch.py removed** — no monkey-patches needed (native Hermes profiles)

## Files in the chain

| Layer | File | Role |
|---|---|---|
| Frontend | `frontend-svelte/src/lib/services/telegramService.js` | API calls to backend |
| Frontend | `frontend-svelte/src/lib/components/admin/TelegramSettingsModal.svelte` | Auth UI (code input, flood wait) |
| Backend | `backend/.../controller/svelte/AiConfigController.java` | User AI config (model, api_key) |
| Backend | `backend/.../controller/svelte/AiInternalController.java` | Internal API for MCP tools |
| Backend | `backend/.../model/UserAiConfig.java` | Per-user AI config entity |
| Hermes | `hermes-agent/config.yaml` | Global multiplexed config + routes (4 shards) |
| Hermes | `hermes-agent/profiles/shard_N/config.yaml` | Per-shard profile config |
| Hermes | `hermes-agent/profiles/shard_N/plugins/dynamic_model_hook.py` | Hook: reads model/api_key from PostgreSQL per request |
| Hermes | `hermes-agent/profiles/shard_N/SOUL.md` | Per-shard agent personality |
| MCP | `hermes-agent/mcp-crm/server.py` | MCP tools server (port 8000) — contacts, appointments, services |
| Python | `notifications-python/main.py` | Telegram client (Pyrogram) — notifications only |
| Script | `hermes-agent/scripts/create_shard_profile.py` | Generator: creates profiles from PostgreSQL data |
| Deploy | `.github/workflows/deploy-main.yml` | Main stack deploy |
| Deploy | `.github/workflows/deploy-hermes.yml` | Hermes stack deploy (includes profile generation) |

## Sharding Strategy

- **4 Telegram bots**: `@NineCRM_AI_1_bot` … `@NineCRM_AI_4_bot`
- **User assignment**: `chat_id % 4` — deterministic, same user → same bot always
- **Each bot has its own profile**: `shard_1` … `shard_4`
- **~30 msg/sec per bot** → ~120 msg/sec total
- **Routes in config.yaml** map `bot_token` → `agent_id` (shard)

## Dynamic AI Config (no per-user .env)

Users save their OpenRouter API key + model in CRM UI → stored in `user_ai_config` table (PostgreSQL).
Hermes profiles use a `pre_model_request` hook plugin that reads config from PostgreSQL per request (cached 5 min).

Flow:
1. User saves API key/model in CRM → backend writes to PostgreSQL
2. User messages Hermes bot → gateway routes to shard profile
3. `dynamic_model_hook.py` fires → reads `user_ai_config` from PostgreSQL for that `telegram_id`
4. Hook injects `model` and `api_key` into LLM request kwargs
5. Hermes calls OpenRouter directly with user's key + model

## Secrets/env

- `BOT_TOKEN_1` … `BOT_TOKEN_4` — Telegram bot tokens (GitHub secrets)
- `DATABASE_URL` — PostgreSQL connection for profile hook
- `TELEGRAM_PROXY` — HTTP CONNECT proxy (VPS in Russia)
- `INTERNAL_SECRET` — shared secret for backend ↔ MCP communication

Any new env var must be added to deploy-main.yml AND deploy-hermes.yml.

## Database Schema (Flyway)

- **Tool**: Flyway — Spring Boot auto-config (`spring.flyway.enabled=true`)
- **Location**: `backend/src/main/resources/db/migration/V*__.sql`
- **Current latest**: `V37__Add_User_Id_To_Staff_Member.sql` (2026-06-24)
- **Table naming**: все таблицы в `snake_case` + plural:
  - `users` (**НЕ** `"user"`, а `"users"`) — CRM пользователи
  - `user_ai_config` — per-user AI config (V35)
  - `staff_members`, `contacts`, `appointments` и т.д.
- **Схема создаётся при старте `backend`** (не через `ddl-auto`, а Flyway)
- **Hermes скрипты** (`create_shard_profile.py`, `dynamic_model_hook.py`) должны использовать `"users"`, а не `"user"` (PostgreSQL reserved word)

## Pyrogram-specific rules (notifications-python/main.py)

When editing `notifications-python/main.py`:

1. **Verify every attribute access** against the installed Pyrogram version:
   - `SentCode` has ONLY: `type`, `phone_code_hash`, `next_type`, `timeout` — NO `phone_registered`
   - `User` has: `id`, `username`, `first_name`, `last_name`, `phone`
   - `Message` has: `id`, `date`, `outgoing`
2. **Check via `pip show pyrogram`** or read Pyrogram source before accessing a new attribute
3. **Import specific exceptions**: `FloodWait`, `PhoneCodeExpired`, `PhoneCodeInvalid`, `PhoneNumberInvalid`, `PhoneNumberFlood`, `PhoneNumberBanned`, `SessionPasswordNeeded`, `ApiIdInvalid`

## Build & Deploy

- `docker compose build --no-cache --parallel` + `docker compose up -d` on VPS
- `docker logs tryneuro_notifications_python --tail 50` — check logs
- `docker compose up -d --build notifications` — rebuild single service
- `docker logs tryneuro_hermes --tail 50 | grep "hook"` — check profile hook logs

## Hermes

- **Source**: https://github.com/NousResearch/hermes-agent
- **Profiles docs**: https://hermes-agent.nousresearch.com/docs/user-guide/profiles
- **Multiplexed gateway**: https://hermes-agent.nousresearch.com/docs/user-guide/multi-profile-gateways
