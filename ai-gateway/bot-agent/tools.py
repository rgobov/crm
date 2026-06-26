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
            "name": "get_contact",
            "description": "Get contact details by ID",
            "parameters": {
                "type": "object",
                "properties": {
                    "contact_id": {"type": "string", "description": "Contact ID"},
                },
                "required": ["contact_id"],
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
            "name": "update_contact",
            "description": "Update contact info (ADMIN/MANAGER only)",
            "parameters": {
                "type": "object",
                "properties": {
                    "contact_id": {"type": "string", "description": "Contact ID"},
                    "name": {"type": "string", "description": "New name (optional)"},
                    "phone": {"type": "string", "description": "New phone (optional)"},
                    "email": {"type": "string", "description": "New email (optional)"},
                    "notes": {"type": "string", "description": "New notes (optional)"},
                },
                "required": ["contact_id"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "delete_contact",
            "description": "Delete a contact (ADMIN/MANAGER only)",
            "parameters": {
                "type": "object",
                "properties": {
                    "contact_id": {"type": "string", "description": "Contact ID to delete"},
                },
                "required": ["contact_id"],
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
            "name": "add_service",
            "description": "Create a new service (ADMIN/MANAGER only)",
            "parameters": {
                "type": "object",
                "properties": {
                    "name": {"type": "string", "description": "Service name"},
                    "duration_minutes": {"type": "integer", "description": "Duration in minutes"},
                    "price_min": {"type": "integer", "description": "Minimum price (optional)"},
                    "price_max": {"type": "integer", "description": "Maximum price (optional)"},
                },
                "required": ["name", "duration_minutes"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "update_service",
            "description": "Update a service (ADMIN/MANAGER only)",
            "parameters": {
                "type": "object",
                "properties": {
                    "service_id": {"type": "string", "description": "Service ID"},
                    "name": {"type": "string", "description": "New name (optional)"},
                    "duration_minutes": {"type": "integer", "description": "New duration (optional)"},
                    "price_min": {"type": "integer", "description": "New minimum price (optional)"},
                    "price_max": {"type": "integer", "description": "New maximum price (optional)"},
                },
                "required": ["service_id"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "delete_service",
            "description": "Delete a service (ADMIN/MANAGER only)",
            "parameters": {
                "type": "object",
                "properties": {
                    "service_id": {"type": "string", "description": "Service ID to delete"},
                },
                "required": ["service_id"],
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
            "name": "get_staff_schedule",
            "description": "Get staff working schedule for a date",
            "parameters": {
                "type": "object",
                "properties": {
                    "staff_id": {"type": "string", "description": "Staff member ID"},
                    "date": {"type": "string", "description": "Date in YYYY-MM-DD format"},
                },
                "required": ["staff_id", "date"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_branches",
            "description": "List all branches",
            "parameters": {
                "type": "object",
                "properties": {},
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "check_availability",
            "description": "Check if a staff member is available at a specific time slot",
            "parameters": {
                "type": "object",
                "properties": {
                    "staff_id": {"type": "string", "description": "Staff member ID"},
                    "date": {"type": "string", "description": "Date in YYYY-MM-DD format"},
                    "time": {"type": "string", "description": "Time in HH:MM format"},
                    "duration": {"type": "integer", "description": "Duration in minutes"},
                },
                "required": ["staff_id", "date", "time", "duration"],
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
                    "clientName": {"type": "string", "description": "Client name"},
                    "clientPhone": {"type": "string", "description": "Client phone"},
                    "serviceName": {"type": "string", "description": "Service name"},
                    "dateTime": {"type": "string", "description": "Date and time in ISO format (e.g. 2026-06-20T14:00:00+03:00)"},
                    "staffName": {"type": "string", "description": "Staff name (optional)"},
                    "durationMinutes": {"type": "integer", "description": "Duration in minutes (default 60)"},
                },
                "required": ["clientName", "clientPhone", "serviceName", "dateTime"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_appointment",
            "description": "Get appointment details by ID",
            "parameters": {
                "type": "object",
                "properties": {
                    "appointment_id": {"type": "string", "description": "Appointment ID"},
                },
                "required": ["appointment_id"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "update_appointment",
            "description": "Reschedule or update an appointment",
            "parameters": {
                "type": "object",
                "properties": {
                    "appointment_id": {"type": "string", "description": "Appointment ID to update"},
                    "dateTime": {"type": "string", "description": "New date and time in ISO format (optional)"},
                    "serviceName": {"type": "string", "description": "New service name (optional)"},
                    "staffName": {"type": "string", "description": "New staff name (optional)"},
                    "durationMinutes": {"type": "integer", "description": "New duration in minutes (optional)"},
                },
                "required": ["appointment_id"],
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
                    "notificationEnabled": {"type": "boolean", "description": "Enable or disable notifications"},
                    "notificationLeadTimeHours": {"type": "integer", "description": "Hours before appointment to send reminder (default 24)"},
                },
                "required": ["notificationEnabled"],
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
                    "reportType": {"type": "string", "description": "Report type: stats|appointments|clients"},
                    "period": {"type": "string", "description": "Period: day|week|month"},
                    "date": {"type": "string", "description": "Date in YYYY-MM-DD format (optional)"},
                },
                "required": ["reportType", "period"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "search_knowledge",
            "description": "Search knowledge base",
            "parameters": {
                "type": "object",
                "properties": {
                    "query": {"type": "string", "description": "Search query"},
                },
                "required": ["query"],
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


TOOLS_WITH_ACTOR = [
    "create_appointment", "cancel_appointment", "get_my_appointments",
    "manage_notifications", "get_report",
    "get_appointment", "update_appointment",
    "get_contact", "update_contact", "delete_contact",
    "add_service", "update_service", "delete_service",
]


async def execute_tool(name: str, args: dict, tenant_id: str, chat_id: int, actor: dict) -> str:
    headers = {
        "X-Internal-Secret": INTERNAL_SECRET,
        "Content-Type": "application/json",
    }

    if name in TOOLS_WITH_ACTOR:
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
            elif name == "get_contact":
                cid = args.get("contact_id", "")
                resp = await client.get(
                    f"{CRM_BACKEND_URL}/api/admin/ai/internal/contacts/{cid}",
                    params={"tenantId": tenant_id},
                    headers=headers,
                )
            elif name == "create_contact":
                resp = await client.post(
                    f"{CRM_BACKEND_URL}/api/admin/ai/internal/contacts",
                    json=body, headers=headers,
                )
            elif name == "update_contact":
                cid = args.get("contact_id", "")
                phone = args.get("phone")
                contact_body = {"name": args.get("name"),
                                "phones": [phone] if phone else None,
                                "email": args.get("email"), "notes": args.get("notes")}
                resp = await client.put(
                    f"{CRM_BACKEND_URL}/api/admin/ai/internal/contacts/{cid}",
                    json=contact_body,
                    headers={**headers, "X-Tenant-Id": tenant_id},
                )
            elif name == "delete_contact":
                cid = args.get("contact_id", "")
                resp = await client.delete(
                    f"{CRM_BACKEND_URL}/api/admin/ai/internal/contacts/{cid}",
                    params={"tenantId": tenant_id},
                    headers=headers,
                )
            elif name == "search_services":
                resp = await client.post(
                    f"{CRM_BACKEND_URL}/api/admin/ai/internal/services/search",
                    json=body, headers=headers,
                )
            elif name == "add_service":
                svc_body = {"tenantId": tenant_id, "name": args.get("name"),
                            "durationMinutes": args.get("duration_minutes"),
                            "priceMin": args.get("price_min"), "priceMax": args.get("price_max")}
                resp = await client.post(
                    f"{CRM_BACKEND_URL}/api/admin/ai/internal/services",
                    json=svc_body, headers=headers,
                )
            elif name == "update_service":
                sid = args.get("service_id", "")
                svc_body = {"tenantId": tenant_id, "name": args.get("name"),
                            "durationMinutes": args.get("duration_minutes"),
                            "priceMin": args.get("price_min"), "priceMax": args.get("price_max")}
                resp = await client.put(
                    f"{CRM_BACKEND_URL}/api/admin/ai/internal/services/{sid}",
                    json=svc_body, headers=headers,
                )
            elif name == "delete_service":
                sid = args.get("service_id", "")
                resp = await client.delete(
                    f"{CRM_BACKEND_URL}/api/admin/ai/internal/services/{sid}",
                    params={"tenantId": tenant_id},
                    headers=headers,
                )
            elif name == "search_staff":
                resp = await client.post(
                    f"{CRM_BACKEND_URL}/api/admin/ai/internal/staff/search",
                    json=body, headers=headers,
                )
            elif name == "get_staff_schedule":
                sched_body = {"tenantId": tenant_id, "staffId": args.get("staff_id"),
                              "date": args.get("date")}
                resp = await client.post(
                    f"{CRM_BACKEND_URL}/api/admin/ai/internal/staff/schedule",
                    json=sched_body, headers=headers,
                )
            elif name == "get_branches":
                headers["X-Tenant-Id"] = tenant_id
                resp = await client.get(
                    f"{CRM_BACKEND_URL}/api/admin/ai/internal/branches",
                    headers=headers,
                )
            elif name == "check_availability":
                avail_body = {"tenantId": tenant_id, "staffId": args.get("staff_id"),
                              "date": args.get("date"), "time": args.get("time"),
                              "duration": args.get("duration")}
                resp = await client.post(
                    f"{CRM_BACKEND_URL}/api/admin/ai/internal/availability",
                    json=avail_body, headers=headers,
                )
            elif name == "create_appointment":
                resp = await client.post(
                    f"{CRM_BACKEND_URL}/api/admin/ai/internal/appointments",
                    json=body, headers=headers,
                )
            elif name == "get_appointment":
                aid = args.get("appointment_id", "")
                headers["X-Tenant-Id"] = tenant_id
                resp = await client.get(
                    f"{CRM_BACKEND_URL}/api/admin/ai/internal/appointments/{aid}",
                    headers=headers,
                )
            elif name == "update_appointment":
                aid = args.get("appointment_id", "")
                apt_body = {"tenantId": tenant_id, "dateTime": args.get("dateTime"),
                            "serviceName": args.get("serviceName"),
                            "staffName": args.get("staffName"),
                            "durationMinutes": args.get("durationMinutes")}
                resp = await client.put(
                    f"{CRM_BACKEND_URL}/api/admin/ai/internal/appointments/{aid}",
                    json=apt_body, headers=headers,
                )
            elif name == "cancel_appointment":
                aid = args.get("appointment_id", "")
                headers["X-Tenant-Id"] = tenant_id
                resp = await client.delete(
                    f"{CRM_BACKEND_URL}/api/admin/ai/internal/appointments/{aid}",
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
            elif name == "search_knowledge":
                resp = await client.post(
                    f"{CRM_BACKEND_URL}/api/admin/ai/internal/knowledge/search",
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
