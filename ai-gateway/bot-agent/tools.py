import json
import logging

import httpx

from config import CRM_BACKEND_URL, INTERNAL_SECRET

logger = logging.getLogger(__name__)

TOOL_SCHEMAS = [
    {
        "type": "function",
        "function": {
            "name": "search_contacts",
            "description": "Search contacts by name or phone",
            "parameters": {
                "type": "object",
                "properties": {
                    "query": {"type": "string", "description": "Name or phone to search"},
                },
                "required": ["query"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "create_contact",
            "description": "Create a new client in CRM",
            "parameters": {
                "type": "object",
                "properties": {
                    "name": {"type": "string", "description": "Client name"},
                    "phone": {"type": "string", "description": "Client phone number"},
                    "email": {"type": "string", "description": "Client email (optional)"},
                    "notes": {"type": "string", "description": "Additional notes (optional)"},
                },
                "required": ["name", "phone"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "search_services",
            "description": "Search available services by name",
            "parameters": {
                "type": "object",
                "properties": {
                    "query": {"type": "string", "description": "Service name to search"},
                },
                "required": ["query"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "search_staff",
            "description": "Search staff members by name",
            "parameters": {
                "type": "object",
                "properties": {
                    "query": {"type": "string", "description": "Staff name to search"},
                },
                "required": ["query"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "create_appointment",
            "description": "Book an appointment for a client",
            "parameters": {
                "type": "object",
                "properties": {
                    "client_name": {"type": "string", "description": "Client name"},
                    "client_phone": {"type": "string", "description": "Client phone"},
                    "service_name": {"type": "string", "description": "Service name"},
                    "date_time": {"type": "string", "description": "Date and time in ISO format (e.g. 2026-06-20T14:00:00+03:00)"},
                    "staff_name": {"type": "string", "description": "Staff name (optional)"},
                    "duration_minutes": {"type": "integer", "description": "Duration in minutes (default 60)"},
                },
                "required": ["client_name", "client_phone", "service_name", "date_time"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "cancel_appointment",
            "description": "Cancel an appointment by ID",
            "parameters": {
                "type": "object",
                "properties": {
                    "appointment_id": {"type": "string", "description": "Appointment ID to cancel"},
                },
                "required": ["appointment_id"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_my_appointments",
            "description": "Get your appointments list",
            "parameters": {
                "type": "object",
                "properties": {},
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "manage_notifications",
            "description": "Update your notification preferences",
            "parameters": {
                "type": "object",
                "properties": {
                    "enabled": {"type": "boolean", "description": "Enable or disable notifications"},
                    "lead_time_hours": {"type": "integer", "description": "Hours before appointment to send reminder (default 24)"},
                },
                "required": ["enabled"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_report",
            "description": "Get business reports (ADMIN/MANAGER only)",
            "parameters": {
                "type": "object",
                "properties": {
                    "report_type": {"type": "string", "description": "Report type: stats|appointments|clients"},
                    "period": {"type": "string", "description": "Period: day|week|month"},
                    "date": {"type": "string", "description": "Date in YYYY-MM-DD format (optional)"},
                },
                "required": ["report_type", "period"],
            },
        },
    },
]

TOOLS_MAP = {s["function"]["name"]: s["function"]["name"] for s in TOOL_SCHEMAS}


async def resolve_actor(chat_id: int) -> dict:
    async with httpx.AsyncClient(timeout=10.0) as client:
        try:
            resp = await client.get(
                f"{CRM_BACKEND_URL}/api/admin/ai/internal/users/by-telegram/{chat_id}",
                headers={"X-Internal-Secret": INTERNAL_SECRET},
            )
            if resp.status_code == 200:
                data = resp.json()
                return {
                    "role": data.get("role", "CLIENT"),
                    "contact_id": data.get("contactId"),
                    "staff_id": data.get("staffId"),
                    "tenant_id": data.get("tenantId"),
                    "user_id": data.get("userId"),
                }
        except Exception as e:
            logger.error("resolve_actor error for chat_id %s: %s", chat_id, e)
    return {"role": "CLIENT", "contact_id": None, "staff_id": None, "tenant_id": None, "user_id": None}


async def execute_tool(name: str, args: dict, tenant_id: str, chat_id: int, actor: dict) -> str:
    headers = {
        "X-Internal-Secret": INTERNAL_SECRET,
        "Content-Type": "application/json",
    }

    if name in ("create_appointment", "cancel_appointment", "get_my_appointments", "manage_notifications", "get_report"):
        headers["X-Actor-Role"] = actor.get("role", "CLIENT")
        if actor.get("contact_id"):
            headers["X-Actor-Contact-Id"] = str(actor["contact_id"])
        if actor.get("staff_id"):
            headers["X-Actor-Staff-Id"] = str(actor["staff_id"])

    body = {"tenantId": tenant_id, **args}

    async with httpx.AsyncClient(timeout=30.0) as client:
        try:
            if name == "search_contacts":
                resp = await client.post(
                    f"{CRM_BACKEND_URL}/api/admin/ai/internal/contacts/search",
                    json=body, headers=headers,
                )
            elif name == "create_contact":
                resp = await client.post(
                    f"{CRM_BACKEND_URL}/api/admin/ai/internal/contacts",
                    json=body, headers=headers,
                )
            elif name == "search_services":
                resp = await client.post(
                    f"{CRM_BACKEND_URL}/api/admin/ai/internal/services/search",
                    json=body, headers=headers,
                )
            elif name == "search_staff":
                resp = await client.post(
                    f"{CRM_BACKEND_URL}/api/admin/ai/internal/staff/search",
                    json=body, headers=headers,
                )
            elif name == "create_appointment":
                resp = await client.post(
                    f"{CRM_BACKEND_URL}/api/admin/ai/internal/appointments",
                    json=body, headers=headers,
                )
            elif name == "cancel_appointment":
                aid = args.get("appointment_id", "")
                resp = await client.delete(
                    f"{CRM_BACKEND_URL}/api/admin/ai/internal/appointments/{aid}",
                    params={"tenantId": tenant_id},
                    headers=headers,
                )
            elif name == "get_my_appointments":
                resp = await client.get(
                    f"{CRM_BACKEND_URL}/api/admin/ai/internal/appointments/my",
                    params={"tenantId": tenant_id},
                    headers=headers,
                )
            elif name == "manage_notifications":
                resp = await client.put(
                    f"{CRM_BACKEND_URL}/api/admin/ai/internal/notifications/preferences",
                    json=body, headers=headers,
                )
            elif name == "get_report":
                resp = await client.post(
                    f"{CRM_BACKEND_URL}/api/admin/ai/internal/reports",
                    json=body, headers=headers,
                )
            else:
                return json.dumps({"error": f"Unknown tool: {name}"}, ensure_ascii=False)

            resp.raise_for_status()
            if resp.status_code == 204:
                return json.dumps({"status": "ok"}, ensure_ascii=False)
            return json.dumps(resp.json(), ensure_ascii=False, default=str)

        except httpx.HTTPStatusError as e:
            logger.error("Tool %s error: %s", name, e)
            return json.dumps({"error": f"API error: {e.response.status_code} {e.response.text[:200]}"}, ensure_ascii=False)
        except Exception as e:
            logger.error("Tool %s exception: %s", name, e)
            return json.dumps({"error": str(e)}, ensure_ascii=False)
