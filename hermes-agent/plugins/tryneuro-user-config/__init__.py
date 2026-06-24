"""tryneuro-user-config — injects <<UM>> marker via pre_gateway_dispatch.

Uses pre_gateway_dispatch (NOT pre_llm_call) because Hermes plugin hooks
pre_llm_call/post_llm_call/on_session_start/on_session_end are never invoked
(known bug #2817, closed as "not planned"). pre_gateway_dispatch IS wired up
in gateway/run.py and receives the MessageEvent with .source.user_id.
"""
import os
import sys
import time

DB_URL = os.getenv("DATABASE_URL", "postgresql://postgres:postgres@tryneuro_database:5432/tryneuro_db")
_USER_CONFIG_CACHE: dict = {}
_CACHE_TTL = 300


def _ensure_psycopg2():
    """Lazy-install psycopg2-binary if not available."""
    try:
        import psycopg2  # noqa: F401
    except ImportError:
        import subprocess
        try:
            subprocess.check_call(
                [sys.executable, "-m", "pip", "install", "psycopg2-binary", "-q"]
            )
        except (subprocess.CalledProcessError, ModuleNotFoundError):
            subprocess.check_call([sys.executable, "-m", "ensurepip", "--upgrade"])
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
    """Register pre_gateway_dispatch hook to inject <<UM>> marker."""
    print("[tryneuro-user-config] register() called")

    def inject_user_marker(event, gateway, session_store, **kwargs):
        """Rewrite incoming message with <<UM>> marker for the proxy."""
        telegram_id = None
        if event and event.source:
            try:
                telegram_id = int(event.source.user_id)
            except (ValueError, TypeError):
                pass

        if not telegram_id:
            return None

        cfg = _get_user_config(telegram_id)
        if not cfg:
            return None

        marker = f"<<UM tg={telegram_id} model=\"{cfg['llm_model']}\">>"
        return {"action": "rewrite", "text": f"{marker}\n{event.text}"}

    ctx.register_hook("pre_gateway_dispatch", inject_user_marker)
    print("[tryneuro-user-config] hook registered: pre_gateway_dispatch")
