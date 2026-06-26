import asyncio
import json
import logging
from openai import AsyncOpenAI

from config import OPENROUTER_BASE, MAX_TOOL_ITERATIONS
from tools import TOOL_SCHEMAS, execute_tool, resolve_actor

logger = logging.getLogger(__name__)

MAX_RETRIES = 3
RETRY_DELAY = 2

SYSTEM_PROMPT = """Ты — AI-ассистент CRM системы TryNeuro.
Помогаешь клиентам с контактами, записями, услугами.
Отвечаешь кратко, по делу, на русском языке.

Используй инструменты CRM для работы с данными. Не выдумывай информацию — используй поиск.

Правила:
1. Для поиска контактов используй search_contacts
2. Для просмотра контакта используй get_contact
3. Для создания клиента используй create_contact
4. Для изменения контакта используй update_contact (только ADMIN/MANAGER)
5. Для удаления контакта используй delete_contact (только ADMIN/MANAGER)
6. Для поиска услуг используй search_services
7. Для создания услуги используй add_service (только ADMIN/MANAGER)
8. Для изменения услуги используй update_service (только ADMIN/MANAGER)
9. Для удаления услуги используй delete_service (только ADMIN/MANAGER)
10. Для поиска сотрудников используй search_staff
11. Для расписания сотрудника используй get_staff_schedule
12. Для списка филиалов используй get_branches
13. Для проверки свободного времени используй check_availability
14. Для записи на услугу используй create_appointment (нужно имя, телефон, услуга, дата/время)
15. Для просмотра записи используй get_appointment
16. Для изменения записи используй update_appointment
17. Для отмены записи используй cancel_appointment
18. Для просмотра своих записей используй get_my_appointments
19. Для настройки уведомлений используй manage_notifications
20. Для отчётов используй get_report (только для ADMIN/MANAGER)
21. Для поиска по базе знаний используй search_knowledge
22. Если ты EMPLOYEE, ты можешь записывать клиентов на услуги, но отменять можешь только свои записи
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

    for iteration in range(MAX_TOOL_ITERATIONS):
        logger.info("Agent iteration %d/%d for chat_id=%s", iteration + 1, MAX_TOOL_ITERATIONS, chat_id)
        last_error = None
        for attempt in range(1, MAX_RETRIES + 1):
            try:
                response = await client.chat.completions.create(
                    model=model,
                    messages=messages,
                    tools=TOOL_SCHEMAS,
                    tool_choice="auto",
                )
                last_error = None
                break
            except Exception as e:
                last_error = e
                err_str = str(e)
                if attempt < MAX_RETRIES and ("502" in err_str or "503" in err_str or "500" in err_str):
                    logger.warning("LLM call attempt %d/%d failed for tg=%s, retrying: %s", attempt, MAX_RETRIES, chat_id, e)
                    await asyncio.sleep(RETRY_DELAY * attempt)
                else:
                    break
        if last_error:
            logger.error("LLM call error for tg=%s after %d attempts: %s", chat_id, MAX_RETRIES, last_error)
            return f"Ошибка при обращении к нейросети: {last_error}"

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

