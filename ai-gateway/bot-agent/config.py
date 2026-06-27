import os

BOT_TOKENS = [
    os.environ["BOT_TOKEN_1"],
    os.environ["BOT_TOKEN_2"],
    os.environ["BOT_TOKEN_3"],
    os.environ["BOT_TOKEN_4"],
]

DATABASE_URL = os.environ["DATABASE_URL"]
INTERNAL_SECRET = os.environ.get("INTERNAL_SECRET", "try-neuro-internal-secret-2026")
CRM_BACKEND_URL = os.environ.get("CRM_BACKEND_URL", "http://backend:8080")
OPENROUTER_BASE = "https://openrouter.ai/api/v1"
TELEGRAM_PROXY = os.environ.get("TELEGRAM_PROXY", "")
MAX_TOOL_ITERATIONS = 30
MAX_HISTORY_EXCHANGES = 10
