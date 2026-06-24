"""Monkey-patch Hermes AIAgent to inject chat_id as user field in API calls."""
import sys
import logging
import os

logging.basicConfig(level=logging.INFO, format="%(name)s [%(levelname)s] %(message)s")
logger = logging.getLogger("hermes-patch")

PATCH_METHOD = "_build_api_kwargs"

from run_agent import AIAgent

_prev = getattr(AIAgent, PATCH_METHOD, None)
if _prev is None:
    logger.error("AIAgent has no '%s' method — check Hermes version", PATCH_METHOD)
    logger.error("Available methods: %s", [m for m in dir(AIAgent) if not m.startswith("_")])
    sys.exit(1)

def _patched(self, *args, **kwargs):
    result = _prev(self, *args, **kwargs)
    chat_id = getattr(self, 'chat_id', None) or getattr(self, 'user_id', None)
    if chat_id is not None:
        result['user'] = str(chat_id)
        logger.info("Injected user=%s into API request", chat_id)
    return result

setattr(AIAgent, PATCH_METHOD, _patched)
logger.info("Patched AIAgent.%s — user field will be injected with chat_id", PATCH_METHOD)

sys.argv = ['hermes', 'gateway', 'run']

from hermes_cli.main import main
main()
