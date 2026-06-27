import asyncio
import logging
import signal
import sys

from telegram import Update
from telegram.ext import Application, MessageHandler, CommandHandler, filters

from config import BOT_TOKENS, MAX_HISTORY_EXCHANGES, TELEGRAM_PROXY
from db import get_user_config
from agent import run_agent

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)

conversations: dict = {}
shutdown_event = asyncio.Event()


async def handle_start(update: Update, _context):
    await update.message.reply_text(
        "Привет! Я AI-ассистент CRM TryNeuro.\n"
        "Напиши свой вопрос — помогу с контактами, записями и услугами.\n"
        "Команды:\n"
        "/new — начать новый диалог\n"
        "/help — справка"
    )


async def handle_new(update: Update, _context):
    chat_id = update.effective_chat.id
    conversations.pop(chat_id, None)
    await update.message.reply_text("Диалог очищен. Задавай новый вопрос.")


async def handle_help(update: Update, _context):
    await update.message.reply_text(
        "Я могу:\n"
        "• Искать и создавать клиентов\n"
        "• Записывать на услуги\n"
        "• Искать услуги и сотрудников\n"
        "• Показывать и отменять записи\n"
        "• Настраивать уведомления\n"
        "• Формировать отчёты\n\n"
        "Просто напиши, что нужно сделать."
    )


async def keep_typing(context, chat_id, done_event):
    while not done_event.is_set():
        await context.bot.send_chat_action(chat_id=chat_id, action="typing")
        await asyncio.sleep(4)


async def handle_message(update: Update, context):
    chat_id = update.effective_chat.id
    user_text = update.message.text.strip()

    if not user_text:
        return

    user_cfg = get_user_config(chat_id)
    if not user_cfg:
        await update.message.reply_text(
            "У вас не настроен API ключ для нейросети.\n"
            "Перейдите в CRM → AI Настройки, сохраните ваш OpenRouter API key и Telegram ID."
        )
        return

    done_event = asyncio.Event()
    typing_task = asyncio.create_task(keep_typing(context, chat_id, done_event))

    if chat_id not in conversations:
        conversations[chat_id] = []
    conversations[chat_id].append({"role": "user", "content": user_text})

    try:
        response = await run_agent(conversations[chat_id], user_cfg, chat_id)
    except Exception as e:
        logger.error("Agent error for tg=%s: %s", chat_id, e)
        response = "Произошла внутренняя ошибка. Попробуйте позже."
    finally:
        done_event.set()
        await typing_task

    conversations[chat_id].append({"role": "assistant", "content": response})

    _prune_history(chat_id)

    try:
        await update.message.reply_text(response)
    except Exception as e:
        logger.error("Failed to send reply for tg=%s: %s", chat_id, e)


def _prune_history(chat_id: int):
    history = conversations.get(chat_id, [])
    if len(history) > MAX_HISTORY_EXCHANGES * 2 + 1:
        system_prompt = history[0] if history[0]["role"] == "system" else None
        keep = history[-(MAX_HISTORY_EXCHANGES * 2):]
        conversations[chat_id] = keep
        if system_prompt and keep[0]["role"] != "system":
            conversations[chat_id].insert(0, system_prompt)


async def build_application(token: str, bot_index: int) -> Application:
    builder = Application.builder().token(token)
    if TELEGRAM_PROXY:
        builder.proxy(TELEGRAM_PROXY)
    app = builder.build()
    app.add_handler(CommandHandler("start", handle_start))
    app.add_handler(CommandHandler("new", handle_new))
    app.add_handler(CommandHandler("help", handle_help))
    app.add_handler(MessageHandler(filters.TEXT & ~filters.COMMAND, handle_message))
    app.bot_data["bot_index"] = bot_index
    return app


async def run_bot(token: str, bot_index: int):
    app = await build_application(token, bot_index)
    logger.info("Bot %d starting...", bot_index + 1)
    async with app:
        await app.start()
        await app.updater.start_polling()
        logger.info("Bot %d started", bot_index + 1)
        await shutdown_event.wait()
        logger.info("Bot %d shutting down...", bot_index + 1)
        await app.updater.stop()
        await app.stop()


async def main():
    logger.info("Starting %d bots...", len(BOT_TOKENS))
    tasks = [run_bot(token, i) for i, token in enumerate(BOT_TOKENS)]
    await asyncio.gather(*tasks)


def handle_signal():
    logger.info("Shutdown signal received, stopping...")
    shutdown_event.set()


if __name__ == "__main__":
    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)
    for sig in (signal.SIGINT, signal.SIGTERM):
        loop.add_signal_handler(sig, handle_signal)
    try:
        loop.run_until_complete(main())
    except KeyboardInterrupt:
        pass
    finally:
        loop.close()
        sys.exit(0)
