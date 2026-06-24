"""Diagnostic: patch OpenAI SDK completions.create to trace LLM calls and inject user."""
import sys
import os
import logging
import traceback

logging.basicConfig(level=logging.INFO, format="%(name)s [%(levelname)s] %(message)s")
logger = logging.getLogger("hermes-patch")

# ── 1. Patch AIAgent._build_api_kwargs (original approach) ──
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
            else:
                logger.warning("_build_api_kwargs called but _chat_id is None")
            return result
        setattr(AIAgent, "_build_api_kwargs", _patched_build)
        logger.info("Patched AIAgent._build_api_kwargs")
    else:
        logger.warning("AIAgent has no _build_api_kwargs method")

    # Also patch __init__ to log chat_id
    _init_prev = AIAgent.__init__
    def _patched_init(self, *args, **kwargs):
        _init_prev(self, *args, **kwargs)
        chat_id = getattr(self, '_chat_id', None)
        logger.info("AIAgent.__init__ called, _chat_id=%s, kwargs keys=%s",
                     chat_id, list(kwargs.keys()))
    AIAgent.__init__ = _patched_init
    logger.info("Patched AIAgent.__init__")

except Exception as e:
    logger.error("Failed to patch AIAgent: %s", e)
    traceback.print_exc()

# ── 2. Patch OpenAI SDK completions.create (most reliable interception) ──
try:
    import openai

    _orig_completions_create = openai.resources.chat.completions.Completions.create

    def _patched_completions_create(self, *args, **kwargs):
        # Log what we received
        logger.info("OPENAI CALL: model=%s, user=%s, messages=%d",
                     kwargs.get("model"),
                     kwargs.get("user"),
                     len(kwargs.get("messages", [])))

        # Try to find chat_id from any accessible context
        chat_id = kwargs.get("user")

        # If no user field, try to find AIAgent instance in call stack
        if not chat_id:
            for frame_info in traceback.extract_stack():
                if "run_agent" in frame_info.filename or "gateway" in frame_info.filename:
                    logger.info("  stack frame: %s:%d %s", frame_info.filename,
                                frame_info.lineno, frame_info.name)

        result = _orig_completions_create(self, *args, **kwargs)
        logger.info("OPENAI RESPONSE: model=%s, usage=%s",
                     getattr(result, "model", "?"),
                     getattr(result, "usage", "?"))
        return result

    openai.resources.chat.completions.Completions.create = _patched_completions_create
    logger.info("Patched openai.resources.chat.completions.Completions.create")

except Exception as e:
    logger.error("Failed to patch OpenAI SDK: %s", e)
    traceback.print_exc()

# ── 3. Try to patch client.chat.completions.create too ──
try:
    from openai import OpenAI
    _orig_client_create = OpenAI.chat.completions.create
    def _patched_client_create(self, *args, **kwargs):
        logger.info("CLIENT CALL: model=%s, user=%s", kwargs.get("model"), kwargs.get("user"))
        return _orig_client_create(self, *args, **kwargs)
    OpenAI.chat.completions.create = _patched_client_create
    logger.info("Patched OpenAI.chat.completions.create")
except Exception as e:
    logger.error("Failed to patch OpenAI client method: %s", e)

# ── Bootstrap Hermes ──
sys.argv = ['hermes', 'gateway', 'run']

from hermes_cli.main import main
main()
