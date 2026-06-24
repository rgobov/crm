"""Diagnostic + fix: inject chat_id as user field into all LLM API calls (sync + async)."""
import sys
import os
import logging
import traceback
import threading

logging.basicConfig(level=logging.INFO, format="%(name)s [%(levelname)s] %(message)s")
logger = logging.getLogger("hermes-patch")

# Thread-local to pass chat_id from AIAgent context to OpenAI call
_tls = threading.local()

# ── 1. Patch AIAgent to capture chat_id ──
try:
    from run_agent import AIAgent

    # _build_api_kwargs (original approach)
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

    # __init__ — log and store chat_id in thread-local
    _init_prev = AIAgent.__init__
    def _patched_init(self, *args, **kwargs):
        _init_prev(self, *args, **kwargs)
        chat_id = getattr(self, '_chat_id', None)
        logger.info("AIAgent.__init__ called, _chat_id=%s, kwargs keys=%s",
                     chat_id, list(kwargs.keys()))
        if chat_id:
            _tls.chat_id = str(chat_id)
    AIAgent.__init__ = _patched_init
    logger.info("Patched AIAgent.__init__")

    # run_conversation — also store chat_id
    if hasattr(AIAgent, "run_conversation"):
        _run_prev = AIAgent.run_conversation
        def _patched_run(self, *args, **kwargs):
            chat_id = getattr(self, '_chat_id', None) or getattr(self, '_user_id', None)
            if chat_id:
                _tls.chat_id = str(chat_id)
                logger.info("run_conversation called, chat_id=%s", _tls.chat_id)
            else:
                logger.warning("run_conversation called but no chat_id on agent")
            return _run_prev(self, *args, **kwargs)
        AIAgent.run_conversation = _patched_run
        logger.info("Patched AIAgent.run_conversation")

except Exception as e:
    logger.error("Failed to patch AIAgent: %s", e)
    traceback.print_exc()

# ── 2. Helper: inject user into kwargs ──
def _inject_user(kwargs):
    chat_id = kwargs.get("user") or getattr(_tls, "chat_id", None)
    if chat_id:
        kwargs["user"] = str(chat_id)
        logger.info("Injected user=%s into API call (model=%s)", chat_id, kwargs.get("model"))
    elif not kwargs.get("user"):
        logger.warning("No chat_id available to inject — _tls.chat_id=%s",
                       getattr(_tls, "chat_id", None))
    return kwargs

# ── 3. Patch SYNC completions.create ──
try:
    import openai
    _orig_sync = openai.resources.chat.completions.Completions.create

    def _patched_sync(self, *args, **kwargs):
        _inject_user(kwargs)
        logger.info("OPENAI SYNC CALL: model=%s, user=%s, messages=%d",
                     kwargs.get("model"), kwargs.get("user"),
                     len(kwargs.get("messages", [])))
        result = _orig_sync(self, *args, **kwargs)
        logger.info("OPENAI SYNC RESPONSE: model=%s", getattr(result, "model", "?"))
        return result

    openai.resources.chat.completions.Completions.create = _patched_sync
    logger.info("Patched SYNC Completions.create")
except Exception as e:
    logger.error("Failed to patch SYNC Completions.create: %s", e)

# ── 4. Patch ASYNC completions.create ──
try:
    _orig_async = openai.resources.chat.completions.AsyncCompletions.create

    async def _patched_async(self, *args, **kwargs):
        _inject_user(kwargs)
        logger.info("OPENAI ASYNC CALL: model=%s, user=%s, messages=%d",
                     kwargs.get("model"), kwargs.get("user"),
                     len(kwargs.get("messages", [])))
        result = await _orig_async(self, *args, **kwargs)
        logger.info("OPENAI ASYNC RESPONSE: model=%s", getattr(result, "model", "?"))
        return result

    openai.resources.chat.completions.AsyncCompletions.create = _patched_async
    logger.info("Patched ASYNC AsyncCompletions.create")
except Exception as e:
    logger.error("Failed to patch ASYNC AsyncCompletions.create: %s", e)

# ── Bootstrap Hermes ──
sys.argv = ['hermes', 'gateway', 'run']

from hermes_cli.main import main
main()
