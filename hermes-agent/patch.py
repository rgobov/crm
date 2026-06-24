"""Fix: inject chat_id as user field by patching AIAgent API call methods."""
import sys
import logging
import traceback
import threading

logging.basicConfig(level=logging.INFO, format="%(name)s [%(levelname)s] %(message)s")
logger = logging.getLogger("hermes-patch")

_tls = threading.local()

# ── 1. Patch AIAgent — capture chat_id + intercept API calls ──
try:
    from run_agent import AIAgent

    _init_prev = AIAgent.__init__
    def _patched_init(self, *args, **kwargs):
        _init_prev(self, *args, **kwargs)
        chat_id = getattr(self, '_chat_id', None)
        if chat_id:
            _tls.chat_id = str(chat_id)
            logger.info("AIAgent.__init__, _chat_id=%s", chat_id)
    AIAgent.__init__ = _patched_init
    logger.info("Patched AIAgent.__init__")

    # Patch _interruptible_streaming_api_call — kwargs dict passed to provider
    if hasattr(AIAgent, "_interruptible_streaming_api_call"):
        _orig_stream_call = AIAgent._interruptible_streaming_api_call
        def _patched_stream_call(self, next_api_kwargs, *args, **kwargs):
            chat_id = next_api_kwargs.get("user") or getattr(_tls, "chat_id", None)
            if chat_id and not next_api_kwargs.get("user"):
                next_api_kwargs["user"] = str(chat_id)
                logger.info("Injected user=%s into streaming call (model=%s)",
                             chat_id, next_api_kwargs.get("model", "?"))
            return _orig_stream_call(self, next_api_kwargs, *args, **kwargs)
        AIAgent._interruptible_streaming_api_call = _patched_stream_call
        logger.info("Patched AIAgent._interruptible_streaming_api_call")

    # Patch _interruptible_api_call — non-streaming fallback
    if hasattr(AIAgent, "_interruptible_api_call"):
        _orig_api_call = AIAgent._interruptible_api_call
        def _patched_api_call(self, next_api_kwargs, *args, **kwargs):
            chat_id = next_api_kwargs.get("user") or getattr(_tls, "chat_id", None)
            if chat_id and not next_api_kwargs.get("user"):
                next_api_kwargs["user"] = str(chat_id)
                logger.info("Injected user=%s into API call (model=%s)",
                             chat_id, next_api_kwargs.get("model", "?"))
            return _orig_api_call(self, next_api_kwargs, *args, **kwargs)
        AIAgent._interruptible_api_call = _patched_api_call
        logger.info("Patched AIAgent._interruptible_api_call")

    # run_conversation — also store chat_id (backup)
    if hasattr(AIAgent, "run_conversation"):
        _run_prev = AIAgent.run_conversation
        def _patched_run(self, *args, **kwargs):
            chat_id = getattr(self, '_chat_id', None) or getattr(self, '_user_id', None)
            if chat_id:
                _tls.chat_id = str(chat_id)
                logger.info("run_conversation, chat_id=%s", chat_id)
            return _run_prev(self, *args, **kwargs)
        AIAgent.run_conversation = _patched_run
        logger.info("Patched AIAgent.run_conversation")
except Exception as e:
    logger.error("Failed to patch AIAgent: %s", e)
    traceback.print_exc()

# ── 2. Bootstrap Hermes ──
sys.argv = ['hermes', 'gateway', 'run']

from hermes_cli.main import main
main()
