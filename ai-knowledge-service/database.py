import sqlite3
import os

DB_PATH = os.getenv("DB_PATH", "/data/knowledge.db")

def init_db():
    conn = sqlite3.connect(DB_PATH)
    conn.execute("""
        CREATE TABLE IF NOT EXISTS knowledge_base (
            id TEXT PRIMARY KEY,
            tenant_id TEXT NOT NULL,
            question TEXT NOT NULL,
            answer TEXT NOT NULL,
            category TEXT DEFAULT 'FAQ',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """)
    conn.execute("""
        CREATE TABLE IF NOT EXISTS tenant_ai_config (
            tenant_id TEXT PRIMARY KEY,
            llm_provider TEXT NOT NULL DEFAULT 'yandex',
            llm_model TEXT NOT NULL DEFAULT 'yandexgpt',
            api_key TEXT,
            stt_provider TEXT DEFAULT 'vosk',
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """)
    conn.execute("""
        CREATE TABLE IF NOT EXISTS stt_log (
            id TEXT PRIMARY KEY,
            tenant_id TEXT NOT NULL,
            duration_ms INTEGER,
            text TEXT,
            confidence REAL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """)
    conn.commit()
    conn.close()

def query_db(sql, params=()):
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    cur = conn.execute(sql, params)
    rows = [dict(row) for row in cur.fetchall()]
    conn.close()
    return rows

def execute_db(sql, params=()):
    conn = sqlite3.connect(DB_PATH)
    cur = conn.execute(sql, params)
    conn.commit()
    last_id = cur.lastrowid
    conn.close()
    return last_id