import json
import logging
from openai import AsyncOpenAI

from config import OPENROUTER_BASE, MAX_TOOL_ITERATIONS
from tools import TOOL_SCHEMAS, execute_tool, resolve_actor

logger = logging.getLogger(__name__)

SYSTEM_PROMPT = """Ты — AI-ассистент CRM системы TryNeuro.
Помогаешь клиентам с контактами, записями, услугами.
Отвечаешь кратко, по делу, на русском языке.

Используй инструменты CRM для работы с данными. Не выдумывай информацию — используй поиск.

Правила:
1. Для поиска контактов используй search_contacts
2. Для создания клиента используй create_contact
3. Для поиска услуг используй search_services
4. Для поиска сотрудников используй search_staff
5. Для записи на услугу используй create_appointment (нужно имя, телефон, услуга, дата/время)
6. Для отмены записи используй cancel_appointment
7. Для просмотра своих записей используй get_my_appointments
8. Для настройки уведомлений используй manage_notifications
9. Для отчётов используй get_report (только для ADMIN/MANAGER)
"""


async def run_agent(history: list, user_cfg: dict, chat_id: int) -> str:
    actor = await resolve_actor(chat_id)
    tenant_id = actor.get("tenant_id")
    role = actor.get("role", "CLIENT")

    system = SYSTEM_PROMPT
    if role in ("ADMIN", "MANAGER", "EMPLOYEE"):
        system += f"\nТвоя роль: {role}. У тебя полный доступ к CRM."
    else:
        system += f"\nТвоя роль: {role}. Ты можешь управлять только своими данными."

    messages = [{"role": "system", "content": system}, *history]

    api_key = user_cfg.get("api_key", "")
    model = user_cfg.get("llm_model", "openrouter/auto")
    if not model:
        model = "openrouter/auto"

    client = AsyncOpenAI(api_key=api_key, base_url=OPENROUTER_BASE)

    for iteration in range(MAX_TOOL_ITERATIONS):
        try:
            response = await client.chat.completions.create(
                model=model,
                messages=messages,
                tools=TOOL_SCHEMAS,
                tool_choice="auto",
            )
        except Exception as e:
            logger.error("LLM call error for tg=%s: %s", chat_id, e)
            return f"Ошибка при обращении к нейросети: {e}"

        choice = response.choices[0]

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
