import asyncio
import json
import logging
from openai import AsyncOpenAI

from config import OPENROUTER_BASE, MAX_TOOL_ITERATIONS
from tools import TOOL_SCHEMAS, execute_tool, resolve_actor

logger = logging.getLogger(__name__)

MAX_RETRIES = 3
RETRY_DELAY = 2
FALLBACK_MODELS = [
    "openai/gpt-oss-120b:free",
    "liquid/lfm-2.5-1.2b-thinking:free",
]

SYSTEM_PROMPT = """Ты — AI-ассистент CRM системы TryNeuro.
Помогаешь клиентам с контактами, записями, услугами.
Отвечаешь кратко, по делу, на русском языке.

Используй инструменты CRM для работы с данными. Не выдумывай информацию — используй поиск.

Правила:
- Для сложных многошаговых задач сначала вызови get_instructions
- Используй search_contacts для поиска клиентов, create_contact для создания
- Используй search_services, search_staff, search_resources, check_availability перед create_appointment
- Если что-то не найдено — ищи шире (пустой query) или предложи создать (ADMIN/MANAGER)
- Для просмотра/изменения/отмены записи используй get_appointment, update_appointment, cancel_appointment
- Если ты EMPLOYEE, ты можешь создавать записи для любых клиентов, но отменять только свои
- После получения результатов инструментов — проанализируй и реши, нужны ли ещё шаги
- Когда задача выполнена — дай ответ пользователю
"""


async def run_agent(history: list, user_cfg: dict, chat_id: int) -> str:
    actor = await resolve_actor(chat_id)
    tenant_id = actor.get("tenant_id")
    role = actor.get("role", "CLIENT")

    system = SYSTEM_PROMPT
    if role in ("ADMIN", "MANAGER"):
        system += f"\nТвоя роль: {role}. У тебя полный доступ ко всем функциям CRM."
    elif role == "EMPLOYEE":
        system += f"\nТвоя роль: {role}. Ты можешь создавать записи для любых клиентов, искать контакты, просматривать расписание и филиалы, но отменять можешь только свои записи."
    else:
        system += f"\nТвоя роль: {role}. Ты можешь управлять только своими данными."

    messages = [{"role": "system", "content": system}, *history]

    api_key = user_cfg.get("api_key", "")
    model = user_cfg.get("llm_model", "openrouter/auto")
    if not model:
        model = "openrouter/auto"

    client = AsyncOpenAI(api_key=api_key, base_url=OPENROUTER_BASE)

    models_to_try = [model] + [m for m in FALLBACK_MODELS if m != model]

    for iteration in range(MAX_TOOL_ITERATIONS):
        logger.info("Agent iteration %d/%d for chat_id=%s", iteration + 1, MAX_TOOL_ITERATIONS, chat_id)
        success = False
        for try_model in models_to_try:
            for attempt in range(1, MAX_RETRIES + 1):
                try:
                    logger.info("LLM call model=%s attempt=%d for chat_id=%s", try_model, attempt, chat_id)
                    extra_kwargs = {}
                    if try_model == model:
                        extra_kwargs["extra_body"] = {
                            "provider": {"ignore": ["Poolside"]}
                        }
                    response = await client.chat.completions.create(
                        model=try_model,
                        messages=messages,
                        tools=TOOL_SCHEMAS,
                        tool_choice="auto",
                        **extra_kwargs,
                    )
                    success = True
                    break
                except Exception as e:
                    err_str = str(e)
                    is_retryable = any(c in err_str for c in ("502", "503", "500", "429"))
                    if attempt < MAX_RETRIES and is_retryable:
                        logger.warning("LLM model=%s attempt %d/%d failed for tg=%s, retrying: %s",
                                       try_model, attempt, MAX_RETRIES, chat_id, e)
                        await asyncio.sleep(RETRY_DELAY * attempt)
                    elif is_retryable:
                        logger.warning("LLM model=%s exhausted retries for tg=%s, trying next model: %s",
                                       try_model, chat_id, e)
                        await asyncio.sleep(RETRY_DELAY)
                    else:
                        logger.error("LLM non-retryable error model=%s for tg=%s: %s", try_model, chat_id, e)
                        return f"Ошибка при обращении к нейросети: {e}"
            if success:
                break
        if not success:
            logger.error("All models exhausted for chat_id=%s", chat_id)
            return "Ошибка при обращении к нейросети: все провайдеры недоступны"

        choice = response.choices[0]
        num_tool_calls = len(choice.message.tool_calls) if choice.message.tool_calls else 0
        logger.info("Agent iteration %d: finish_reason=%s tool_calls=%d for chat_id=%s",
                     iteration + 1, choice.finish_reason, num_tool_calls, chat_id)

        if choice.finish_reason == "stop":
            return choice.message.content or ""

        if choice.finish_reason == "tool_calls":
            if not choice.message.tool_calls:
                return choice.message.content or ""

            messages.append(choice.message)

            for tc in choice.message.tool_calls:
                func_name = tc.function.name
                try:
                    func_args = json.loads(tc.function.arguments)
                except json.JSONDecodeError:
                    func_args = {}

                logger.info("Tool call: %s args=%s", func_name, func_args)
                result = await execute_tool(func_name, func_args, tenant_id, chat_id, actor)

                messages.append({
                    "role": "tool",
                    "tool_call_id": tc.id,
                    "content": result,
                })

        elif choice.finish_reason == "length":
            return choice.message.content or ""

        else:
            return choice.message.content or ""

    return "Превышено количество шагов обработки. Пожалуйста, уточните запрос."

