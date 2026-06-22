# CRM Project — Notification Chain Invariant

## Project Overview
- **Stack**: Spring Boot (Java) + PostgreSQL + Svelte (Node) + Python (FastAPI/Pyrogram)
- **Deploy**: GitHub Actions → VPS via SSH, docker-compose
- **Proxy**: HTTP CONNECT `87.121.86.253:8888` — all Telegram traffic
- **Network**: `tryneuro_network` (external)

## CRITICAL: Telegram Notification Chain

Any modification to files in this chain MUST verify consistency across ALL layers.

### Files in the chain

| Layer | File | Role |
|---|---|---|
| Frontend | `frontend-svelte/src/lib/services/telegramService.js` | API calls to backend |
| Frontend | `frontend-svelte/src/lib/components/admin/TelegramSettingsModal.svelte` | Auth UI (code input, flood wait) |
| Backend | `backend/.../client/NotificationClient.java` | Feign client to Python service |
| Backend | `backend/.../service/NotificationManager.java` | Sends reminders via Telegram |
| Backend | `backend/.../service/TelegramSettingsService.java` | Auth settings |
| Backend | `backend/.../controller/.../TelegramSettingsController.java` | REST endpoints |
| Backend | `backend/.../exception/GlobalExceptionHandler.java` | Error propagation |
| Python | `notifications-python/main.py` | Telegram client (Pyrogram) |
| Deploy | `.github/workflows/deploy.yml` | Deploy pipeline |
| Deploy | `docker-compose.yml` | Infrastructure definition |

### What to verify before editing any of these files

1. **API contract consistency**: request/response field names match across all layers
   - `tenantId` / `phone` / `code` / `status` / `phoneCodeHash` — same names everywhere
2. **Phone number format**: digits only (`79022566116`), no `+` or formatting (NotificationManager strips non-digits)
3. **Error types**: `FLOOD_WAIT`, `CODE_EXPIRED`, `CODE_INVALID`, `PASSWORD_NEEDED` — handled identically in Python and Java
4. **Secrets/env**: any new env var must be added to deploy.yml AND docker-compose.yml
5. **Proxy**: never remove `TELEGRAM_PROXY` — VPS in Russia, Telegram blocked without it

### Pyrogram-specific rules (notifications-python/main.py)

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
