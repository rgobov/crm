"""Sidecar: inject chat_id via httpx class-level patches.

Monkey-patches httpx.Client.send / AsyncClient.send at the CLASS level,
so even clients created before the patch are intercepted at send time.
chat_id is captured from AIAgent.__init__ and stored in thread-local storage.

Backup: class-level patch of Completions.create / AsyncCompletions.create.
"""
import sys
import json
import logging
import threading
import functools

logging.basicConfig(level=logging.INFO, format="%(name)s [%(levelname)s] %(message)s")
logger = logging.getLogger("hermes-patch")

_tls = threading.local()

# ── 1. Patch httpx at the class level ──────────────────────────────────────
import httpx

_orig_send = httpx.Client.send


def _patched_send(self, request, **kwargs):
    chat_id = getattr(_tls, "chat_id", None)
    if chat_id and "/v1/chat/completions" in request.url.path:
        # Method A: inject chat_id as HTTP header (backed by mcp-crm X-Chat-ID support)
        request.headers["X-Chat-ID"] = str(chat_id)
        # Method B: inject user field into JSON body (direct, no server change needed)
        try:
            body = json.loads(request.content)
            if not body.get("user"):
                body["user"] = str(chat_id)
                new_content = json.dumps(body).encode()
                object.__setattr__(request, "_content", new_content)
                request.headers["content-length"] = str(len(new_content))
                logger.info(
                    "injected user=%s into %s", chat_id, request.url
                )
        except Exception:
            logger.warning(
                "body injection failed for %s, header only", request.url, exc_info=True
            )
    return _orig_send(self, request, **kwargs)


httpx.Client.send = _patched_send
logger.info("patched httpx.Client.send (class-level)")

_orig_async_send = httpx.AsyncClient.send


async def _patched_async_send(self, request, **kwargs):
    chat_id = getattr(_tls, "chat_id", None)
    if chat_id and "/v1/chat/completions" in request.url.path:
        # Method A: inject chat_id as HTTP header (backed by mcp-crm X-Chat-ID support)
        request.headers["X-Chat-ID"] = str(chat_id)
        # Method B: inject user field into JSON body (direct, no server change needed)
        try:
            body = json.loads(request.content)
            if not body.get("user"):
                body["user"] = str(chat_id)
                new_content = json.dumps(body).encode()
                object.__setattr__(request, "_content", new_content)
                request.headers["content-length"] = str(len(new_content))
                logger.info(
                    "injected user=%s into %s (async)", chat_id, request.url
                )
        except Exception:
            logger.warning(
                "body injection failed for %s (async), header only",
                request.url,
                exc_info=True,
            )
    return await _orig_async_send(self, request, **kwargs)


httpx.AsyncClient.send = _patched_async_send
logger.info("patched httpx.AsyncClient.send (class-level)")

# ── 2. Backup: patch Completions.create at class level ─────────────────────
try:
    from openai.resources.chat.completions import Completions, AsyncCompletions

    _orig_create = Completions.create

    @functools.wraps(_orig_create)
    def _patched_create(self, *args, **kwargs):
        chat_id = getattr(_tls, "chat_id", None)
        if chat_id and not kwargs.get("user"):
            kwargs["user"] = str(chat_id)
            logger.info("injected user=%s via Completions.create", chat_id)
        return _orig_create(self, *args, **kwargs)

    Completions.create = _patched_create
    logger.info("patched Completions.create (class-level)")

    _orig_async_create = AsyncCompletions.create

    @functools.wraps(_orig_async_create)
    async def _patched_async_create(self, *args, **kwargs):
        chat_id = getattr(_tls, "chat_id", None)
        if chat_id and not kwargs.get("user"):
            kwargs["user"] = str(chat_id)
            logger.info("injected user=%s via AsyncCompletions.create", chat_id)
        return await _orig_async_create(self, *args, **kwargs)

    AsyncCompletions.create = _patched_async_create
    logger.info("patched AsyncCompletions.create (class-level)")
except Exception as e:
    logger.error("failed to patch Completions.create: %s", e)

# ── 3. Patch AIAgent to capture chat_id ────────────────────────────────────
try:
    from run_agent import AIAgent

    _init_prev = AIAgent.__init__

    def _patched_init(self, *args, **kwargs):
        _init_prev(self, *args, **kwargs)
        chat_id = getattr(self, "_chat_id", None)
        if chat_id:
            _tls.chat_id = str(chat_id)
            logger.info("AIAgent.__init__, _chat_id=%s", chat_id)

    AIAgent.__init__ = _patched_init
    logger.info("patched AIAgent.__init__")
except Exception as e:
    logger.error("failed to patch AIAgent: %s", e)

# ── 4. Bootstrap Hermes gateway ────────────────────────────────────────────
sys.argv = ["hermes", "gateway", "run"]
from hermes_cli.main import main

main()
