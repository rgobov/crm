"""Fix: inject chat_id as user field by patching _perform_api_call in conversation_loop."""
import sys
import logging
import traceback
import threading

logging.basicConfig(level=logging.INFO, format="%(name)s [%(levelname)s] %(message)s")
logger = logging.getLogger("hermes-patch")

_tls = threading.local()

# ── 1. Patch AIAgent to capture chat_id into thread-local ──
try:
    from run_agent import AIAgent

    _init_prev = AIAgent.__init__
    def _patched_init(self, *args, **kwargs):
        _init_prev(self, *args, **kwargs)
        chat_id = getattr(self, '_chat_id', None)
        if chat_id:
            _tls.chat_id = str(chat_id)
            logger.info("AIAgent.__init__ called, _chat_id=%s", chat_id)
    AIAgent.__init__ = _patched_init
    logger.info("Patched AIAgent.__init__")

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

# ── 2. Patch _perform_api_call — the ONLY guaranteed interception point ──
try:
    from agent import conversation_loop

    _orig_perform = conversation_loop._perform_api_call

    def _patched_perform(next_api_kwargs):
        chat_id = next_api_kwargs.get("user") or getattr(_tls, "chat_id", None)
        if chat_id and not next_api_kwargs.get("user"):
            next_api_kwargs["user"] = str(chat_id)
            logger.info("Injected user=%s into API call (model=%s)",
                         chat_id, next_api_kwargs.get("model", "?"))
        elif not chat_id:
            logger.warning("No chat_id available — _tls.chat_id=%s",
                           getattr(_tls, "chat_id", None))
        return _orig_perform(next_api_kwargs)

    conversation_loop._perform_api_call = _patched_perform
    logger.info("Patched conversation_loop._perform_api_call")
except Exception as e:
    logger.error("Failed to patch _perform_api_call: %s", e)
    traceback.print_exc()

# ── 3. Bootstrap Hermes ──
sys.argv = ['hermes', 'gateway', 'run']

from hermes_cli.main import main
main()
