#!/usr/bin/env python3
"""Генератор шард-профилей Hermes для multiplexed gateway.

Читает user_ai_config из PostgreSQL, распределяет пользователей по 4 шардам
(chat_id % 4), создаёт профили с SOUL.md, config.yaml и hook-плагином.

Hook читает модель/api_key из PostgreSQL на каждый запрос (кэш 5 мин),
поэтому .env с ключами не нужен — обновление CRM сразу применяется.
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

HOOK_PLUGIN = """# dynamic_model_hook.py
import os
import time
import psycopg2
from psycopg2.extras import RealDictCursor

DB_URL = os.getenv("DATABASE_URL", "postgresql://postgres:postgres@tryneuro_database:5432/tryneuro_db")
_user_config_cache = {}

async def pre_model_request(ctx, kwargs):
    context_id = ctx.get("context_id")
    if not context_id:
        return kwargs
    cfg = _get_config(context_id)
    if cfg:
        kwargs["model"] = cfg["llm_model"]
        kwargs["api_key"] = cfg["api_key"]
        kwargs["base_url"] = "https://openrouter.ai/api/v1"
        kwargs.setdefault("default_headers", {})["Authorization"] = f"Bearer {cfg['api_key']}"
    return kwargs

def _get_config(telegram_id_str):
    now = time.time()
    if telegram_id_str in _user_config_cache:
        cached, ts = _user_config_cache[telegram_id_str]
        if now - ts < 300:
            return cached
    try:
        telegram_id = int(telegram_id_str)
        conn = psycopg2.connect(DB_URL)
        cur = conn.cursor(cursor_factory=RealDictCursor)
        cur.execute(\"\"\"
            SELECT uac.llm_model, uac.api_key
            FROM \"users\" u
            JOIN user_ai_config uac ON u.id = uac.user_id
            WHERE u.telegram_id = %s AND uac.api_key IS NOT NULL AND uac.api_key != ''
        \"\"\", (telegram_id,))
        row = cur.fetchone()
        cur.close()
        conn.close()
        if row:
            cfg = {"llm_model": row["llm_model"], "api_key": row["api_key"]}
            _user_config_cache[telegram_id_str] = (cfg, now)
            return cfg
    except Exception as e:
        print(f"[hook] Error for {telegram_id_str}: {e}")
    return None
"""


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
        }
        (prof_dir / "config.yaml").write_text(yaml.dump(prof_config, sort_keys=False))

        plugins_dir = prof_dir / "plugins"
        plugins_dir.mkdir(exist_ok=True)
        (plugins_dir / "dynamic_model_hook.py").write_text(HOOK_PLUGIN)

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
