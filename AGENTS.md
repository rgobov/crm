# CRM Project — OpenClaw Multi-Agent Gateway

## Project Overview
- **Stack**: Spring Boot (Java) + PostgreSQL + Svelte (Node) + OpenClaw (Node) + Pyrogram (Python)
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
| OpenClaw (`@NineCRM_AI_bot_1` … `_4`) | **4 bots** | AI chat, MCP tools (contacts, appointments), 1 процесс, 4 аккаунта |

- The Pyrogram user client is NOT a bot — it's a Telegram user session authorized by phone + code
- OpenClaw runs as a **multi-agent gateway** (1 process, 4 Telegram accounts, 4 agents via `bindings`)

## Architecture: OpenClaw Multi-Agent + tryneuro-provider

```
User → Telegram (bot_N) → OpenClaw Gateway (1 process)
                                │
                    ┌───────────┼──────────────┐
                    ▼           ▼              ▼
              shard_1      shard_2       shard_3/4
              (agent)      (agent)        (agent)
                    │           │              │
                    └───────────┼──────────────┘
                                │
                    tryneuro-provider plugin
                    resolveSyntheticAuth + wrapStreamFn
                    читает user_ai_config из PostgreSQL
                                │
                                ▼
                          OpenRouter (ключ юзера + модель)
```

- **tryneuro-provider** (`extensions/tryneuro-provider/index.ts`) — OpenClaw provider plugin
- **No OpenRouter Proxy** — логика встроена в provider (бывший `proxy/main.py` удалён)
- **MCP tools** — `mcp-crm` (FastAPI, порт 8000), подключается через OpenClaw `mcp.servers`
- **Per-user auth** — `resolveSyntheticAuth` inject'ит api_key, `wrapStreamFn` подменяет model
- **Кэш 5 мин** — in-memory cache в provider плагине

## Files in the chain

| Layer | File | Role |
|---|---|---|
| Frontend | `frontend-svelte/src/lib/services/telegramService.js` | API вызовы к backend |
| Frontend | `frontend-svelte/src/lib/components/admin/TelegramSettingsModal.svelte` | UI авторизации (код, flood wait) |
| Backend | `backend/.../controller/svelte/AiConfigController.java` | User AI config (model, api_key) |
| Backend | `backend/.../controller/svelte/AiInternalController.java` | Internal API для MCP инструментов |
| Backend | `backend/.../model/UserAiConfig.java` | Per-user AI config entity |
| OpenClaw | `hermes-agent/openclaw.json` | Multi-agent config + Telegram accounts + MCP |
| OpenClaw | `hermes-agent/extensions/tryneuro-provider/index.ts` | Provider plugin — per-user OpenRouter api_key/model |
| OpenClaw | `hermes-agent/extensions/tryneuro-provider/package.json` | Package manifest |
| OpenClaw | `hermes-agent/extensions/tryneuro-provider/openclaw.plugin.json` | Plugin manifest |
| MCP | `hermes-agent/mcp-crm/server.py` | MCP tools server (порт 8000) — contacts, appointments, services |
| Python | `notifications-python/main.py` | Telegram клиент (Pyrogram) — уведомления |
| Deploy | `.github/workflows/deploy-main.yml` | Деплой основного стека (backend, notifications, frontend) |
| Deploy | `.github/workflows/deploy-openclaw.yml` | Деплой OpenClaw + MCP |

## Sharding Strategy

- **4 Telegram bots**: `@NineCRM_AI_1_bot` … `@NineCRM_AI_4_bot`
- **User assignment**: детерминированно через OpenClaw `bindings` — один аккаунт (`shard_N`) → один агент
- **Each account = one agent**: `channels.telegram.accounts.shard_N` → `bindings[].agentId`
- **~30 msg/sec per bot** → ~120 msg/sec total

## Dynamic AI Config (без per-user .env)

Пользователи сохраняют OpenRouter API key + model в CRM UI → записывается в `user_ai_config` (PostgreSQL).
OpenClaw provider plugin читает конфиг из PostgreSQL (кэш 5 мин).

**Flow:**
1. Пользователь сохраняет API key/model в CRM → backend пишет в PostgreSQL
2. Пользователь пишет боту → OpenClaw route'ит в агента по `accountId`
3. `tryneuro-provider` plugin hook (`wrapStreamFn`) срабатывает:
   - `resolveSyntheticAuth` читает api_key из PostgreSQL по `telegram_id`
   - Если у пользователя нет api_key — fallback на `OPENROUTER_API_KEY`
4. `wrapStreamFn` подменяет model на пользовательскую (если задана)
5. Запрос к OpenRouter с ключом пользователя

## Secrets/env

- `BOT_TOKEN_1` … `BOT_TOKEN_4` — Telegram bot tokens (GitHub secrets)
- `DATABASE_URL` — PostgreSQL connection для provider plugin
- `TELEGRAM_PROXY` — HTTP CONNECT proxy для доступа к Telegram из РФ (обязателен на VPS в России)
- `INTERNAL_SECRET` — shared secret для backend ↔ MCP communication

Новые env var добавляются в deploy-main.yml И deploy-openclaw.yml.

## Deploy Workflows (GitHub Actions)

**Два разделенных workflow:**

| Workflow | Branch Trigger | Path Filter | Деплоит |
|----------|--------------|-------------|--------|
| `deploy-main.yml` | `feature/roles`, `fix/ios-final-attempt` | `paths-ignore: 'hermes-agent/**'` | backend, notifications, frontend, database |
| `deploy-openclaw.yml` | `feature/roles`, `fix/ios-final-attempt` | `paths: 'hermes-agent/**'` | openclaw-gateway, mcp-crm |

**Изоляция:** оба workflow используют `concurrency.group: deploy-vps` → не выполняются одновременно.
**Деплой OpenClaw НЕ ломает CRM и уведомления** — workflow отдельные.

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
- `docker logs tryneuro_openclaw --tail 50 | grep "tryneuro-provider"` — проверка провайдера
- `docker logs tryneuro_mcp_crm --tail 20` — проверка MCP сервера
- `openclaw agents list --bindings` — 4 агента в статусе online
- `openclaw channels status --probe` — 4 Telegram аккаунта

## OpenClaw

- **Source**: https://github.com/openclaw/openclaw
- **Docs**: https://docs.openclaw.ai
- **Plugin SDK**: https://docs.openclaw.ai/plugins/building-plugins
- **Provider hooks**: https://docs.openclaw.ai/plugins/sdk-provider-plugins
- **Telegram channels**: https://docs.openclaw.ai/channels/telegram
- **Multi-agent routing**: https://docs.openclaw.ai/concepts/multi-agent

## Внимание: OpenClaw плагины

При любых правках `hermes-agent/extensions/` — обязательно сверяться с официальной документацией OpenClaw (ссылки выше).
Особенно: названия provider hooks (`before_model_resolve`, `wrapStreamFn`, `resolveSyntheticAuth`), API SDK (`definePluginEntry`), структура манифеста (`openclaw.plugin.json`).
TypeScript, не Python. Не делать предположений — API OpenClaw быстро меняется.