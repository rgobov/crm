#!/usr/bin/env python3
"""Генератор шард-профилей Hermes для multiplexed gateway.

Читает user_ai_config из PostgreSQL, распределяет пользователей по 4 шардам
(chat_id % 4), создаёт профили с SOUL.md, config.yaml и httpx-плагином.

Плагин перехватывает HTTP-запросы Hermes к OpenRouter и подставляет
модель/api_key из PostgreSQL для каждого пользователя (через session_context).
Кэш 5 мин. Без прокси, без мёртвых хуков.
"""
import os
import yaml
import psycopg2
from psycopg2.extras import RealDictCursor
from pathlib import Path

DB_URL = os.getenv("DATABASE_URL", "postgresql://postgres:postgres@tryneuro_database:5432/tryneuro_db")
BOT_TOKENS = [os.getenv(f"BOT_TOKEN_{i}") for i in range(1, 5)]
NUM_SHARDS = len([t for t in BOT_TOKENS if t])
PROFILES_DIR = Path(os.getenv("PROFILES_DIR", "/opt/hermes/profiles"))
CONFIG_DIR = Path(os.getenv("CONFIG_DIR", str(PROFILES_DIR.parent)))

BASE_SOUL = """# TryNeuro CRM Assistant
Ты — AI-ассистент CRM системы TryNeuro.
Помогаешь клиентам с контактами, записями, услугами.
Отвечаешь кратко, по делу, на русском.
"""

PLUGIN_YAML = """name: tryneuro-user-config
version: "1.0"
description: Per-user model/api_key from PostgreSQL via httpx monkey-patch
requires_env:
  - name: DATABASE_URL
    description: PostgreSQL connection string for user_ai_config lookups
"""

PLUGIN_INIT = r'''"""tryneuro-user-config — intercepts httpx requests to OpenRouter.

Reads current session chat_id from gateway.session_context,
looks up user_ai_config from PostgreSQL, and rewrites the request
with the user's model and API key. Cached 5 minutes.
"""
import os
import sys
import json
import time
import httpx

DB_URL = os.getenv("DATABASE_URL", "postgresql://postgres:postgres@tryneuro_database:5432/tryneuro_db")
_USER_CONFIG_CACHE: dict = {}
_CACHE_TTL = 300


def _ensure_psycopg2():
    """Lazy-install psycopg2-binary if not available."""
    try:
        import psycopg2  # noqa: F401
    except ImportError:
        import subprocess
        subprocess.check_call(
            [sys.executable, "-m", "pip", "install", "psycopg2-binary", "-q"]
        )


def _get_user_config(telegram_id: int) -> dict | None:
    """Read user AI config from PostgreSQL, cached 5 min."""
    _ensure_psycopg2()
    import psycopg2
    from psycopg2.extras import RealDictCursor

    now = time.time()
    key = str(telegram_id)
    if key in _USER_CONFIG_CACHE:
        cached, ts = _USER_CONFIG_CACHE[key]
        if now - ts < _CACHE_TTL:
            return cached
    try:
        conn = psycopg2.connect(DB_URL)
        cur = conn.cursor(cursor_factory=RealDictCursor)
        cur.execute(
            'SELECT uac.llm_model, uac.api_key '
            'FROM "users" u '
            'JOIN user_ai_config uac ON u.id = uac.user_id '
            'WHERE u.telegram_id = %s AND uac.api_key IS NOT NULL AND uac.api_key != \'\'',
            (telegram_id,),
        )
        row = cur.fetchone()
        cur.close()
        conn.close()
        if row:
            cfg = {"llm_model": row["llm_model"], "api_key": row["api_key"]}
            _USER_CONFIG_CACHE[key] = (cfg, now)
            print(f"[tryneuro-user-config] loaded tg={telegram_id} model={row['llm_model']}")
            return cfg
    except Exception as e:
        print(f"[tryneuro-user-config] error for tg={telegram_id}: {e}")
    return None


def _get_session_chat_id() -> int | None:
    """Read Telegram chat_id from session context."""
    try:
        from gateway.session_context import get_session_env
        val = get_session_env("HERMES_SESSION_CHAT_ID")
        if val:
            return int(val)
    except Exception:
        pass
    try:
        from gateway.session_context import _SESSION_CHAT_ID
        val = _SESSION_CHAT_ID.get()
        if val:
            return int(val)
    except Exception:
        pass
    return None


# ── httpx.Client monkey-patch ──────────────────────────────────────────
_ORIGINAL_SEND = getattr(httpx.Client, "send", None)


def _patched_send(self, request: httpx.Request, **kwargs):
    """Patched httpx.Client.send — rewrites OpenRouter requests per user."""
    url = str(request.url)
    if _ORIGINAL_SEND is None or ("openrouter.ai" not in url and "/v1/" not in url):
        return _ORIGINAL_SEND(self, request, **kwargs)

    telegram_id = _get_session_chat_id()
    if telegram_id:
        cfg = _get_user_config(telegram_id)
        if cfg:
            request.headers["Authorization"] = f"Bearer {cfg['api_key']}"
            if request.content:
                try:
                    body = json.loads(request.content)
                    orig_model = body.get("model", "")
                    body["model"] = cfg["llm_model"]
                    request.content = json.dumps(body).encode()
                    request.headers.pop("Content-Length", None)
                    print(f"[tryneuro-user-config] rewrite tg={telegram_id} {orig_model} -> {cfg['llm_model']}")
                except (json.JSONDecodeError, Exception) as e:
                    print(f"[tryneuro-user-config] body rewrite failed: {e}")

    return _ORIGINAL_SEND(self, request, **kwargs)


# ── httpx.AsyncClient monkey-patch ─────────────────────────────────────
_ORIGINAL_ASYNC_SEND = getattr(httpx.AsyncClient, "send", None)


async def _patched_async_send(self, request: httpx.Request, **kwargs):
    """Patched httpx.AsyncClient.send — rewrites OpenRouter requests per user."""
    url = str(request.url)
    if _ORIGINAL_ASYNC_SEND is None or ("openrouter.ai" not in url and "/v1/" not in url):
        return await _ORIGINAL_ASYNC_SEND(self, request, **kwargs)

    telegram_id = _get_session_chat_id()
    if telegram_id:
        cfg = _get_user_config(telegram_id)
        if cfg:
            request.headers["Authorization"] = f"Bearer {cfg['api_key']}"
            if request.content:
                try:
                    body = json.loads(request.content)
                    orig_model = body.get("model", "")
                    body["model"] = cfg["llm_model"]
                    request.content = json.dumps(body).encode()
                    request.headers.pop("Content-Length", None)
                    print(f"[tryneuro-user-config] rewrite tg={telegram_id} {orig_model} -> {cfg['llm_model']}")
                except (json.JSONDecodeError, Exception) as e:
                    print(f"[tryneuro-user-config] body rewrite failed: {e}")

    return await _ORIGINAL_ASYNC_SEND(self, request, **kwargs)


def register(ctx):
    """Apply httpx patches on plugin load."""
    # Patch sync client
    httpx.Client.send = _patched_send
    # Patch async client
    httpx.AsyncClient.send = _patched_async_send
    print("[tryneuro-user-config] httpx patches applied")
'''


def assign_shard(telegram_id: int) -> int:
    return telegram_id % NUM_SHARDS


def main():
    conn = psycopg2.connect(DB_URL)
    cur = conn.cursor(cursor_factory=RealDictCursor)
    cur.execute("""
        SELECT u.id as user_id, u.telegram_id, u.tenant_id
        FROM "users" u
        WHERE u.telegram_id IS NOT NULL
    """)
    users = cur.fetchall()
    cur.close()
    conn.close()
    print(f"Found {len(users)} users with telegram_id")

    shards = {i: {"users": [], "bot_token": BOT_TOKENS[i]} for i in range(NUM_SHARDS)}
    for u in users:
        idx = assign_shard(u["telegram_id"])
        shards[idx]["users"].append(u)

    for idx, data in shards.items():
        if not data["bot_token"]:
            print(f"⚠️ BOT_TOKEN_{idx+1} not set, skip")
            continue

        prof_dir = PROFILES_DIR / f"shard_{idx+1}"
        prof_dir.mkdir(parents=True, exist_ok=True)

        (prof_dir / "SOUL.md").write_text(BASE_SOUL)

        prof_config = {
            "model": {
                "provider": "custom",
                "base_url": "https://openrouter.ai/api/v1",
                "api_key": "dummy",
                "model": "openrouter/auto",
            },
            "toolsets": ["crm", "files", "web"],
            "memory": {"enabled": True, "max_tokens": 4000},
            "plugins": {"enabled": ["tryneuro-user-config"]},
        }
        (prof_dir / "config.yaml").write_text(yaml.dump(prof_config, sort_keys=False))

        plugin_dir = prof_dir / "plugins" / "tryneuro-user-config"
        plugin_dir.mkdir(parents=True, exist_ok=True)
        (plugin_dir / "plugin.yaml").write_text(PLUGIN_YAML)
        (plugin_dir / "__init__.py").write_text(PLUGIN_INIT)

        # Remove old dead hook if present
        old_hook = prof_dir / "plugins" / "dynamic_model_hook.py"
        if old_hook.exists():
            old_hook.unlink()

        print(f"✅ shard_{idx+1}: {len(data['users'])} users, bot={data['bot_token'][:12]}...")

    global_config = {
        "gateway": {"multiplex_profiles": True},
        "routes": [
            {"match": {"platform": "telegram", "bot_token": f"${{BOT_TOKEN_{i+1}}}"}, "agent_id": f"shard_{i+1}"}
            for i in range(NUM_SHARDS)
        ],
    }
    (CONFIG_DIR / "config.yaml").write_text(yaml.dump(global_config, sort_keys=False))
    print("✅ Global config.yaml written")


if __name__ == "__main__":
    main()
