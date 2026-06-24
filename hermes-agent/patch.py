"""Diagnostic + fix: inject chat_id as user field into LLM API calls at the httpx transport layer."""
import sys
import json
import os
import logging
import traceback
import threading

logging.basicConfig(level=logging.INFO, format="%(name)s [%(levelname)s] %(message)s")
logger = logging.getLogger("hermes-patch")

_tls = threading.local()

# ── 1. Patch AIAgent to capture chat_id ──
try:
    from run_agent import AIAgent

    _build_prev = getattr(AIAgent, "_build_api_kwargs", None)
    if _build_prev:
        def _patched_build(self, *args, **kwargs):
            result = _build_prev(self, *args, **kwargs)
            chat_id = getattr(self, '_chat_id', None) or getattr(self, '_user_id', None)
            if chat_id is not None:
                result['user'] = str(chat_id)
                logger.info("Injected user=%s into _build_api_kwargs result", chat_id)
            return result
        setattr(AIAgent, "_build_api_kwargs", _patched_build)
        logger.info("Patched AIAgent._build_api_kwargs")

    _init_prev = AIAgent.__init__
    def _patched_init(self, *args, **kwargs):
        _init_prev(self, *args, **kwargs)
        chat_id = getattr(self, '_chat_id', None)
        logger.info("AIAgent.__init__ called, _chat_id=%s", chat_id)
        if chat_id:
            _tls.chat_id = str(chat_id)
    AIAgent.__init__ = _patched_init
    logger.info("Patched AIAgent.__init__")

    if hasattr(AIAgent, "run_conversation"):
        _run_prev = AIAgent.run_conversation
        def _patched_run(self, *args, **kwargs):
            chat_id = getattr(self, '_chat_id', None) or getattr(self, '_user_id', None)
            if chat_id:
                _tls.chat_id = str(chat_id)
                logger.info("run_conversation called, chat_id=%s", _tls.chat_id)
            return _run_prev(self, *args, **kwargs)
        AIAgent.run_conversation = _patched_run
        logger.info("Patched AIAgent.run_conversation")
except Exception as e:
    logger.error("Failed to patch AIAgent: %s", e)
    traceback.print_exc()

# ── 2. Patch httpx transport — catches ALL HTTP calls regardless of SDK ──
def _patch_httpx():
    import httpx

    def _inject_user_in_body(request: httpx.Request) -> httpx.Request:
        url = str(request.url)
        if request.method != "POST" or "/chat/completions" not in url:
            return request
        try:
            body = json.loads(request.content)
        except Exception:
            return request

        chat_id = body.get("user") or getattr(_tls, "chat_id", None)
        if body.get("user") or not chat_id:
            return request

        body["user"] = str(chat_id)
        logger.info("Injected user=%s into httpx request to %s", chat_id, url)

        new_content = json.dumps(body).encode()
        headers = dict(request.headers)
        headers["content-length"] = str(len(new_content))
        return httpx.Request(
            method=request.method,
            url=request.url,
            content=new_content,
            headers=headers,
        )

    # Async client
    _orig_async_send = httpx.AsyncClient.send
    async def _patched_async_send(self, request: httpx.Request, *args, **kwargs):
        request = _inject_user_in_body(request)
        if "/chat/completions" in str(request.url) and request.method == "POST":
            logger.info("HTTPX ASYNC CALL: url=%s, body_preview=%s",
                         str(request.url)[:80], request.content[:200])
        return await _orig_async_send(self, request, *args, **kwargs)
    httpx.AsyncClient.send = _patched_async_send
    logger.info("Patched httpx.AsyncClient.send")

    # Sync client
    _orig_sync_send = httpx.Client.send
    def _patched_sync_send(self, request: httpx.Request, *args, **kwargs):
        request = _inject_user_in_body(request)
        if "/chat/completions" in str(request.url) and request.method == "POST":
            logger.info("HTTPX SYNC CALL: url=%s", str(request.url)[:80])
        return _orig_sync_send(self, request, *args, **kwargs)
    httpx.Client.send = _patched_sync_send
    logger.info("Patched httpx.Client.send")

try:
    _patch_httpx()
except Exception as e:
    logger.error("Failed to patch httpx: %s", e)
    traceback.print_exc()

# ── 3. Bootstrap Hermes ──
sys.argv = ['hermes', 'gateway', 'run']

from hermes_cli.main import main
main()
