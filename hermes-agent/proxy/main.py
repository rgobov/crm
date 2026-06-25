"""OpenRouter Proxy — проксирует запросы от Hermes к OpenRouter с per-user api_key/model.

Архитектура:
  Hermes → (base_url=http://proxy:8003/v1) → OpenRouter Proxy → OpenRouter
                                                  │
                                           PostgreSQL (user_ai_config)
                                           Кэш 5 мин

Маркер в user message: <<UM tg=12345 model="openrouter/gpt-4">>
  - inject'ится pre_llm_call хуком в tryneuro-user-config плагине
  - proxy парсит, удаляет из сообщения, читает api_key из БД, подменяет model
"""
import os
import re
import json
import time
import logging
from contextlib import asynccontextmanager

import httpx
import psycopg2
from psycopg2.extras import RealDictCursor
from fastapi import FastAPI, Request, Response
from fastapi.middleware.cors import CORSMiddleware

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("openrouter-proxy")

OPENROUTER_BASE = "https://openrouter.ai/api/v1"
DB_URL = os.getenv("DATABASE_URL", "postgresql://postgres:postgres@tryneuro_database:5432/tryneuro_db")
PROXY_PORT = int(os.getenv("PROXY_PORT", "8003"))

# Маркер: <<UM tg=12345 model="openrouter/gpt-4">>
MARKER_RE = re.compile(r'<<UM\s+tg=(\d+)\s+model="([^"]+)"\s*>>')

# Кэш конфигов: {telegram_id: (config_dict, timestamp)}
_USER_CONFIG_CACHE: dict = {}
_CACHE_TTL = 300  # 5 минут

http_client = httpx.AsyncClient(timeout=120.0)


def _get_user_config(telegram_id: int) -> dict | None:
    """Читает user_ai_config из PostgreSQL, кэш 5 мин."""
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
            logger.info("loaded config for tg=%s model=%s", telegram_id, row["llm_model"])
            return cfg
    except Exception as e:
        logger.error("DB error for tg=%s: %s", telegram_id, e)
    return None


def _parse_marker(messages: list) -> tuple[int | None, str | None]:
    """Ищет маркер <<UM ...>> в сообщениях, возвращает (telegram_id, model)."""
    for msg in messages:
        content = msg.get("content", "")
        if isinstance(content, list):
            # Мультимодальные сообщения: content = [{"type": "text", "text": "..."}, ...]
            for part in content:
                if isinstance(part, dict) and part.get("type") == "text":
                    m = MARKER_RE.search(part["text"])
                    if m:
                        return int(m.group(1)), m.group(2)
        elif isinstance(content, str):
            m = MARKER_RE.search(content)
            if m:
                return int(m.group(1)), m.group(2)
    return None, None


def _strip_marker(messages: list) -> list:
    """Удаляет маркер из содержимого сообщений."""
    result = []
    for msg in messages:
        content = msg.get("content", "")
        if isinstance(content, list):
            cleaned_parts = []
            for part in content:
                if isinstance(part, dict) and part.get("type") == "text":
                    cleaned_parts.append({**part, "text": MARKER_RE.sub("", part["text"]).strip()})
                else:
                    cleaned_parts.append(part)
            result.append({**msg, "content": cleaned_parts})
        elif isinstance(content, str):
            result.append({**msg, "content": MARKER_RE.sub("", content).strip()})
        else:
            result.append(msg)
    return result


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("OpenRouter proxy starting on port %s", PROXY_PORT)
    yield
    await http_client.aclose()
    logger.info("OpenRouter proxy stopped")


app = FastAPI(title="OpenRouter Proxy", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health")
async def health():
    return {"status": "ok"}


@app.get("/v1/models")
async def list_models():
    """Прокси запроса списка моделей в OpenRouter."""
    headers = {
        "Content-Type": "application/json",
    }
    try:
        resp = await http_client.get(f"{OPENROUTER_BASE}/models", headers=headers)
        return Response(content=resp.content, status_code=resp.status_code, media_type="application/json")
    except Exception as e:
        logger.error("Failed to list models: %s", e)
        return {"data": []}


@app.post("/v1/chat/completions")
async def chat_completions(request: Request):
    body = await request.json()
    messages = body.get("messages", [])

    # Парсим маркер
    telegram_id, user_model = _parse_marker(messages)
    if telegram_id is None:
        logger.warning("No marker found in messages")
        return Response(
            content=json.dumps({
                "choices": [{
                    "message": {
                        "role": "assistant",
                        "content": "⚠️ Не удалось определить ваш профиль. Пожалуйста, настройте AI в CRM → Настройки → AI провайдер (укажите API-ключ OpenRouter и модель)."
                    }
                }]
            }),
            status_code=200,
            media_type="application/json",
        )

    # Читаем api_key из БД
    cfg = _get_user_config(telegram_id)
    if cfg is None:
        logger.warning("No config found for tg=%s", telegram_id)
        return Response(
            content=json.dumps({
                "choices": [{
                    "message": {
                        "role": "assistant",
                        "content": "⚠️ API-ключ не настроен. Перейдите в CRM → Настройки → AI провайдер и укажите ваш OpenRouter API ключ."
                    }
                }]
            }),
            status_code=200,
            media_type="application/json",
        )

    # Удаляем маркер из сообщений
    cleaned_messages = _strip_marker(messages)

    # Подменяем model на пользовательскую
    request_model = user_model or body.get("model", "openrouter/auto")

    # Формируем запрос к OpenRouter
    openrouter_body = {**body, "model": request_model, "messages": cleaned_messages}
    openrouter_headers = {
        "Authorization": f"Bearer {cfg['api_key']}",
        "Content-Type": "application/json",
        "HTTP-Referer": "https://tryneuro.ru",
        "X-Title": "TryNeuro CRM",
    }

    is_stream = openrouter_body.get("stream", False)

    if is_stream:
        return await _proxy_stream(openrouter_body, openrouter_headers)
    else:
        return await _proxy_sync(openrouter_body, openrouter_headers)


async def _proxy_sync(body: dict, headers: dict) -> Response:
    """Прокси без streaming — возвращает JSON-ответ."""
    try:
        resp = await http_client.post(
            f"{OPENROUTER_BASE}/chat/completions",
            json=body,
            headers=headers,
        )
        return Response(content=resp.content, status_code=resp.status_code, media_type="application/json")
    except Exception as e:
        logger.error("Sync proxy error: %s", e)
        return Response(
            content=json.dumps({"error": f"Proxy upstream error: {e}"}),
            status_code=502,
            media_type="application/json",
        )


async def _proxy_stream(body: dict, headers: dict) -> Response:
    """Прокси streaming — forward SSE чанков."""
    from fastapi.responses import StreamingResponse

    async def event_stream():
        try:
            async with http_client.stream(
                "POST",
                f"{OPENROUTER_BASE}/chat/completions",
                json=body,
                headers=headers,
            ) as upstream:
                async for chunk in upstream.aiter_bytes():
                    yield chunk
        except Exception as e:
            logger.error("Stream proxy error: %s", e)
            yield f"data: {json.dumps({'error': str(e)})}\n\n"
            yield "data: [DONE]\n\n"

    return StreamingResponse(
        event_stream(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        },
    )


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=PROXY_PORT)
