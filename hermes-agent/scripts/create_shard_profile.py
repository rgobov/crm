#!/usr/bin/env python3
"""Генератор шард-профилей Hermes для multiplexed gateway.

Читает user_ai_config из PostgreSQL, распределяет пользователей по 4 шардам
(chat_id % 4), создаёт профили с SOUL.md, config.yaml и нативным pre_llm_call плагином.

Плагин inject'ит маркер <<UM tg=id model="model_name">> в user message.
OpenRouter Proxy читает маркер, подменяет api_key и model в HTTP-запросе.
Кэш 5 мин. Нативный Hermes plugin hook.
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

## Доступные инструменты (MCP crm toolset)
Используй их для работы с CRM:

### Контакты
- search_contacts — найти клиента по имени или телефону
- create_contact — создать нового клиента

### Записи (appointments)
- create_appointment — записать клиента на услугу
- cancel_appointment — отменить запись по ID
- get_my_appointments — показать записи клиента

### Услуги и сотрудники
- search_services — найти услугу по названию
- search_staff — найти сотрудника по имени

### Уведомления и отчёты
- manage_notifications — включить/выключить уведомления, время напоминания
- get_report — бизнес-отчёты (stats, appointments, clients) для админов

## Правила работы
1. Всегда уточняй tenant_id из контекста разговора
2. Для записей проси: имя клиента, телефон, услугу, дату/время (ISO формат)
3. CLIENT роль — только свои записи/контакты, ADMIN/MANAGER — всё
4. Не придумывай данные — используй инструменты поиска
"""

PLUGIN_YAML = """name: tryneuro-user-config
version: "1.0"
description: Per-user model/api_key from PostgreSQL via pre_llm_call hook
requires_env:
  - name: DATABASE_URL
    description: PostgreSQL connection string for user_ai_config lookups
"""

PLUGIN_INIT = '''"""tryneuro-user-config — injects <<UM>> marker for OpenRouter Proxy.

Reads user_ai_config from PostgreSQL via session_context telegram_id,
injects <<UM tg=id model="model_name">> marker into the user message.
OpenRouter Proxy reads this marker and overrides api_key + model upstream.
Cached 5 minutes.
"""
import os
import time
import psycopg2
from psycopg2.extras import RealDictCursor

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


def register(ctx):
    """Register pre_llm_call hook to inject <<UM>> marker for OpenRouter Proxy."""
    
    @ctx.register_hook("pre_llm_call")
    async def inject_user_marker(session_context, **kwargs):
        """Inject <<UM tg=id model="name">> marker for the proxy."""
        telegram_id = None
        if session_context:
            chat_id_val = session_context.get("HERMES_SESSION_CHAT_ID")
            if chat_id_val:
                try:
                    telegram_id = int(chat_id_val)
                except (ValueError, TypeError):
                    pass
        
        if not telegram_id:
            return {"context": ""}
        
        cfg = _get_user_config(telegram_id)
        if not cfg:
            return {"context": ""}
        
        marker = f"<<UM tg={telegram_id} model=\\"{cfg['llm_model']}\\">>"
        return {"context": marker}
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
                "base_url": "http://proxy:8003/v1",
                "api_key": "proxy",
                "model": "proxy",
            },
            "toolsets": ["crm"],
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