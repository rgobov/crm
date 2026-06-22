import asyncio
import threading
import os
import logging

import httpx
import uvicorn
from fastapi import FastAPI, Request, HTTPException
from fastapi.responses import JSONResponse
from fastmcp import FastMCP

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

CRM_URL = os.getenv("CRM_BACKEND_URL", "http://backend:8080")
INTERNAL_SECRET = os.getenv("INTERNAL_SECRET", "try-neuro-internal-secret-2026")
AI_KNOWLEDGE_URL = os.getenv("AI_KNOWLEDGE_URL", "http://ai-knowledge-service:8082")

http_client = httpx.AsyncClient(timeout=30.0)


async def _crm_call(endpoint: str, tenant_id: str, method: str = "POST", **kwargs):
    headers = {"X-Internal-Secret": INTERNAL_SECRET, "Content-Type": "application/json"}
    body = {"tenantId": tenant_id, **kwargs}
    url = f"{CRM_URL}/api/admin/ai/internal{endpoint}"
    if method == "POST":
        resp = await http_client.post(url, json=body, headers=headers)
    elif method == "GET":
        resp = await http_client.get(url, params=body, headers=headers)
    elif method == "DELETE":
        resp = await http_client.delete(url, params=body, headers=headers)
    else:
        raise ValueError(f"Unsupported method: {method}")
    resp.raise_for_status()
    return resp.json()


async def _crm_call_actor(
    endpoint: str, tenant_id: str,
    actor_role: str = "", actor_contact_id: str = "", actor_staff_id: str = "",
    method: str = "POST", **kwargs
):
    headers = {
        "X-Internal-Secret": INTERNAL_SECRET,
        "Content-Type": "application/json",
        "X-Actor-Role": actor_role or "CLIENT",
        "X-Tenant-Id": tenant_id,
    }
    if actor_contact_id:
        headers["X-Actor-Contact-Id"] = actor_contact_id
    if actor_staff_id:
        headers["X-Actor-Staff-Id"] = actor_staff_id

    body = {"tenantId": tenant_id, **kwargs}
    url = f"{CRM_URL}/api/admin/ai/internal{endpoint}"
    if method == "POST":
        resp = await http_client.post(url, json=body, headers=headers)
    elif method == "GET":
        resp = await http_client.get(url, params=body, headers=headers)
    elif method == "DELETE":
        resp = await http_client.delete(url, params=body, headers=headers)
    else:
        raise ValueError(f"Unsupported method: {method}")

    if resp.status_code in (401, 403):
        raise HTTPException(resp.status_code, resp.text)
    resp.raise_for_status()
    if resp.status_code == 204:
        return {"status": "ok"}
    return resp.json()


mcp = FastMCP("CRM Tools")


@mcp.tool()
async def search_contacts(
    tenant_id: str, query: str,
    chat_id: int,
    actor_role: str = "ADMIN", actor_contact_id: str = "", actor_staff_id: str = "",
) -> list:
    """Search contacts by name or phone. STAFF/ADMIN/MANAGER only."""
    actor = await resolve_actor_from_chat_id(chat_id)
    return await _crm_call_actor(
        "/contacts/search", tenant_id,
        actor_role=actor["role"], actor_contact_id=actor["contact_id"], actor_staff_id=actor["staff_id"],
        query=query,
    )


@mcp.tool()
async def create_contact(
    tenant_id: str, name: str, phone: str,
    chat_id: int,
    email: str = "", notes: str = "",
    actor_role: str = "ADMIN", actor_contact_id: str = "", actor_staff_id: str = "",
) -> dict:
    """Create a new client in CRM. ADMIN/MANAGER only."""
    actor = await resolve_actor_from_chat_id(chat_id)
    return await _crm_call_actor(
        "/contacts", tenant_id,
        actor_role=actor["role"], actor_contact_id=actor["contact_id"], actor_staff_id=actor["staff_id"],
        name=name, phone=phone, email=email, notes=notes,
    )


@mcp.tool()
async def search_services(tenant_id: str, query: str, chat_id: int) -> list:
    """Search available services by name. Any role."""
    return await _crm_call("/services/search", tenant_id, query=query)


@mcp.tool()
async def search_staff(tenant_id: str, query: str, chat_id: int) -> list:
    """Search staff members by name. Any role."""
    return await _crm_call("/staff/search", tenant_id, query=query)


@mcp.tool()
async def create_appointment(
    tenant_id: str,
    client_name: str,
    client_phone: str,
    service_name: str,
    date_time: str,
    chat_id: int,
    staff_name: str = "",
    branch_id: str = "",
    duration_minutes: int = 60,
    actor_role: str = "ADMIN",
    actor_contact_id: str = "",
    actor_staff_id: str = "",
) -> dict:
    """Book an appointment. CLIENT gets contactId auto-populated. Provide tenant_id from conversation context. date_time is ISO format (e.g. 2026-06-20T14:00:00+03:00)."""
    actor = await resolve_actor_from_chat_id(chat_id)
    return await _crm_call_actor(
        "/appointments", tenant_id,
        actor_role=actor["role"], actor_contact_id=actor["contact_id"], actor_staff_id=actor["staff_id"],
        clientName=client_name,
        clientPhone=client_phone,
        serviceName=service_name,
        staffName=staff_name,
        branchId=branch_id,
        dateTime=date_time,
        durationMinutes=duration_minutes,
    )


@mcp.tool()
async def cancel_appointment(
    appointment_id: str,
    tenant_id: str,
    chat_id: int,
    actor_role: str = "CLIENT",
    actor_contact_id: str = "",
    actor_staff_id: str = "",
) -> dict:
    """Cancel an appointment by ID. CLIENT can only cancel own appointments."""
    actor = await resolve_actor_from_chat_id(chat_id)
    return await _crm_call_actor(
        f"/appointments/{appointment_id}", tenant_id,
        actor_role=actor["role"], actor_contact_id=actor["contact_id"], actor_staff_id=actor["staff_id"],
        method="DELETE",
    )


@mcp.tool()
async def get_my_appointments(
    tenant_id: str,
    chat_id: int,
    actor_role: str = "CLIENT",
    actor_contact_id: str = "",
    actor_staff_id: str = "",
) -> list:
    """Get your appointments. CLIENT provides actor_contact_id from conversation context."""
    actor = await resolve_actor_from_chat_id(chat_id)
    return await _crm_call_actor(
        "/appointments/my", tenant_id,
        actor_role=actor["role"], actor_contact_id=actor["contact_id"], actor_staff_id=actor["staff_id"],
        method="GET",
    )


@mcp.tool()
async def manage_notifications(
    tenant_id: str,
    chat_id: int,
    enabled: bool = True,
    lead_time_hours: int = 24,
    actor_role: str = "CLIENT",
    actor_contact_id: str = "",
    actor_staff_id: str = "",
) -> dict:
    """Update your notification preferences. CLIENT only."""
    actor = await resolve_actor_from_chat_id(chat_id)
    return await _crm_call_actor(
        "/notifications/preferences", tenant_id,
        actor_role=actor["role"], actor_contact_id=actor["contact_id"], actor_staff_id=actor["staff_id"],
        method="PUT",
        notificationEnabled=enabled,
        notificationLeadTimeHours=lead_time_hours,
    )


@mcp.tool()
async def get_report(
    tenant_id: str,
    chat_id: int,
    report_type: str = "stats",
    period: str = "day",
    date: str = "",
    actor_role: str = "ADMIN",
    actor_contact_id: str = "",
    actor_staff_id: str = "",
) -> dict:
    """Get business reports. ADMIN/MANAGER only. report_type: stats|appointments|clients. period: day|week|month."""
    actor = await resolve_actor_from_chat_id(chat_id)
    return await _crm_call_actor(
        "/reports", tenant_id,
        actor_role=actor["role"], actor_contact_id=actor["contact_id"], actor_staff_id=actor["staff_id"],
        reportType=report_type,
        period=period,
        date=date or None,
    )


async def resolve_actor_from_chat_id(chat_id: int) -> dict:
    """Resolve actor info from chat_id using CRM internal API."""
    try:
        resp = await http_client.get(
            f"{CRM_URL}/api/admin/ai/internal/users/by-telegram/{chat_id}",
            headers={"X-Internal-Secret": INTERNAL_SECRET},
        )
        if resp.status_code == 200:
            return resp.json()
        elif resp.status_code == 404:
            return {"role": "CLIENT", "contact_id": "", "staff_id": "", "tenant_id": ""}
        else:
            raise HTTPException(resp.status_code, resp.text)
    except Exception as e:
        logger.error(f"Failed to resolve actor from chat_id {chat_id}: {e}")
        return {"role": "CLIENT", "contact_id": "", "staff_id": "", "tenant_id": ""}


llm_app = FastAPI(title="LLM Proxy")


async def get_user_key(user_id: str):
    """Get OpenRouter API key for a specific user."""
    try:
        resp = await http_client.get(
            f"{AI_KNOWLEDGE_URL}/api/v1/user-config/{user_id}",
            headers={"X-Internal-Secret": INTERNAL_SECRET},
        )
        if resp.status_code == 200:
            config = resp.json()
            return config.get("api_key") or None
    except Exception:
        pass
    return None


async def resolve_user_id_by_chat_id(chat_id: int) -> str:
    """Resolve user_id by Telegram chat_id via backend API."""
    try:
        resp = await http_client.get(
            f"{CRM_URL}/api/admin/ai/internal/tenant/by-telegram/{chat_id}",
            headers={"X-Internal-Secret": INTERNAL_SECRET},
        )
        if resp.status_code == 200:
            data = resp.json()
            return data.get("userId", "")
    except Exception as e:
        logger.error(f"Failed to resolve user_id by chat_id {chat_id}: {e}")
    return ""


@llm_app.post("/v1/chat/completions")
async def llm_proxy(request: Request):
    body = await request.json()
    
    # Try to get user_id from various sources
    user_id = ""
    
    # 1. X-User-ID header (if set by caller)
    user_id = request.headers.get("X-User-ID", "")
    
    # 2. Resolve via backend if 'user' field contains a chat_id (numeric)
    if not user_id:
        user_field = body.get("user", "")
        if isinstance(user_field, (str, int)):
            try:
                chat_id = int(user_field)
                resolved = await resolve_user_id_by_chat_id(chat_id)
                if resolved:
                    user_id = resolved
            except (ValueError, TypeError):
                pass
    
    if not user_id:
        return JSONResponse(content={
            "id": "chatcmpl-unknown-user",
            "object": "chat.completion",
            "created": 0,
            "model": body.get("model", "unknown"),
            "choices": [{
                "index": 0,
                "message": {
                    "role": "assistant",
                    "content": "❌ Ваш Telegram-аккаунт не привязан к CRM.\n\nПожалуйста, привяжите Telegram ID в настройках AI:\n1. Напишите @userinfobot, скопируйте ваш ID (число)\n2. В CRM: Настройки → AI → поле Telegram ID\n3. Сохраните и попробуйте снова"
                },
                "finish_reason": "stop"
            }]
        }, status_code=200)

    api_key = await get_user_key(user_id)
    if not api_key:
        return JSONResponse(content={
            "id": "chatcmpl-no-key",
            "object": "chat.completion",
            "created": 0,
            "model": body.get("model", "unknown"),
            "choices": [{
                "index": 0,
                "message": {
                    "role": "assistant",
                    "content": "❌ У вас не настроен API-ключ OpenRouter.\n\nПожалуйста, добавьте ключ в CRM: Настройки → AI → поле OpenRouter API Key.\n\nПолучить ключ: https://openrouter.ai/keys"
                },
                "finish_reason": "stop"
            }]
        }, status_code=200)
    
    headers = {
        "Authorization": f"Bearer {api_key}",
        "Content-Type": "application/json",
        "HTTP-Referer": "https://crm.999crm.ru",
        "X-Title": "TryNeuro CRM",
    }
    async with httpx.AsyncClient(timeout=120.0) as client:
        resp = await client.post(
            "https://openrouter.ai/api/v1/chat/completions",
            json=body,
            headers=headers,
        )
        return JSONResponse(content=resp.json(), status_code=resp.status_code)


@llm_app.get("/health")
async def health():
    return {"status": "ok"}


def run_mcp():
    logger.info("Starting MCP server on port 8000")
    mcp.run(transport="streamable-http", port=8000)


def run_llm():
    logger.info("Starting LLM proxy on port 8001")
    uvicorn.run(llm_app, host="0.0.0.0", port=8001, log_level="info")


if __name__ == "__main__":
    t1 = threading.Thread(target=run_mcp, daemon=True)
    t2 = threading.Thread(target=run_llm, daemon=True)
    t1.start()
    t2.start()
    t1.join()
    t2.join()
