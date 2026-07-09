package com.tryneuro.aibot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class CrmToolService {

    private static final Logger log = LoggerFactory.getLogger(CrmToolService.class);

    private final RestTemplate rest;
    private final ObjectMapper mapper;
    private final String backendUrl;
    private final String internalSecret;

    private static final List<String> TOOLS_WITH_ACTOR = List.of(
        "create_appointment", "cancel_appointment", "get_my_appointments",
        "manage_notifications", "get_report", "get_appointment", "update_appointment",
        "get_contact", "update_contact", "delete_contact",
        "add_service", "update_service", "delete_service"
    );

    private static final java.util.regex.Pattern ID_FIELD_PATTERN =
            java.util.regex.Pattern.compile(".*(_id|Id)$");
    private static final java.util.regex.Pattern INVALID_ID_PATTERN =
            java.util.regex.Pattern.compile("[\\s\\p{IsCyrillic}]");

    private static String validateIdArg(String fieldName, Object value) {
        if (value == null) return null;
        String s = String.valueOf(value);
        if (INVALID_ID_PATTERN.matcher(s).find()) {
            throw new IllegalArgumentException(
                    "Поле '" + fieldName + "' должно быть ID из search/get tools, а получено значение '" + s
                            + "'. Сначала вызови get_branches/search_staff/search_services и возьми из ответа поле id.");
        }
        return s;
    }

    public CrmToolService(RestTemplate rest, ObjectMapper mapper) {
        this.rest = rest;
        this.mapper = mapper;
        String url = System.getenv("CRM_BACKEND_URL");
        this.backendUrl = (url != null && !url.isEmpty()) ? url : "http://backend:8080";
        String secret = System.getenv("INTERNAL_SECRET");
        this.internalSecret = (secret != null && !secret.isEmpty()) ? secret : "try-neuro-internal-secret-2026";
    }

    public String executeTool(String name, Map<String, Object> args, String tenantId,
                              Map<String, String> actorHeaders) {
        long startMs = System.currentTimeMillis();
        log.info("executeTool: name={}, args={}, tenantId={}", name, args, tenantId);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Secret", internalSecret);
        headers.set("Content-Type", "application/json");

        if (TOOLS_WITH_ACTOR.contains(name)) {
            if (actorHeaders.containsKey("X-Actor-Role"))
                headers.set("X-Actor-Role", actorHeaders.get("X-Actor-Role"));
            if (actorHeaders.containsKey("X-Actor-Contact-Id"))
                headers.set("X-Actor-Contact-Id", actorHeaders.get("X-Actor-Contact-Id"));
            if (actorHeaders.containsKey("X-Actor-Staff-Id"))
                headers.set("X-Actor-Staff-Id", actorHeaders.get("X-Actor-Staff-Id"));
        }

        Map<String, Object> body = new java.util.LinkedHashMap<>(args);
        body.put("tenantId", tenantId);

        try {
            String json = mapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(json, headers);

            String result = switch (name) {
                case "search_contacts" -> post("/api/admin/ai/internal/contacts/search", entity);
                case "get_contact" -> get("/api/admin/ai/internal/contacts/" + validateIdArg("contact_id", args.get("contact_id")), Map.of("tenantId", tenantId), headers);
                case "create_contact" -> post("/api/admin/ai/internal/contacts", entity);
                case "update_contact" -> {
                    Map<String, Object> contactBody = new java.util.LinkedHashMap<>();
                    contactBody.put("name", args.get("name"));
                    contactBody.put("phones", args.get("phone") != null ? List.of(args.get("phone")) : null);
                    contactBody.put("email", args.get("email"));
                    contactBody.put("notes", args.get("notes"));
                    String contactJson = mapper.writeValueAsString(contactBody);
                    HttpEntity<String> contactEntity = new HttpEntity<>(contactJson, headers);
                    headers.set("X-Tenant-Id", tenantId);
                    yield put("/api/admin/ai/internal/contacts/" + validateIdArg("contact_id", args.get("contact_id")), contactEntity);
                }
                case "delete_contact" -> delete("/api/admin/ai/internal/contacts/" + validateIdArg("contact_id", args.get("contact_id")), Map.of("tenantId", tenantId), headers);
                case "search_services" -> post("/api/admin/ai/internal/services/search", entity);
                case "add_service" -> {
                    Map<String, Object> svcBody = new java.util.LinkedHashMap<>();
                    svcBody.put("tenantId", tenantId);
                    svcBody.put("name", args.get("name"));
                    svcBody.put("durationMinutes", args.get("duration_minutes"));
                    svcBody.put("priceMin", args.get("price_min"));
                    svcBody.put("priceMax", args.get("price_max"));
                    String svcJson = mapper.writeValueAsString(svcBody);
                    yield post("/api/admin/ai/internal/services", new HttpEntity<>(svcJson, headers));
                }
                case "update_service" -> {
                    Map<String, Object> svcBody = new java.util.LinkedHashMap<>();
                    svcBody.put("tenantId", tenantId);
                    svcBody.put("name", args.get("name"));
                    svcBody.put("durationMinutes", args.get("duration_minutes"));
                    svcBody.put("priceMin", args.get("price_min"));
                    svcBody.put("priceMax", args.get("price_max"));
                    String svcJson = mapper.writeValueAsString(svcBody);
                    yield put("/api/admin/ai/internal/services/" + validateIdArg("service_id", args.get("service_id")), new HttpEntity<>(svcJson, headers));
                }
                case "delete_service" -> delete("/api/admin/ai/internal/services/" + validateIdArg("service_id", args.get("service_id")), Map.of("tenantId", tenantId), headers);
                case "get_staff_schedule" -> {
                    Map<String, Object> schedBody = new java.util.LinkedHashMap<>();
                    schedBody.put("tenantId", tenantId);
                    schedBody.put("staffId", validateIdArg("staff_id", args.get("staff_id")));
                    schedBody.put("date", args.get("date"));
                    String schedJson = mapper.writeValueAsString(schedBody);
                    yield post("/api/admin/ai/internal/staff/schedule", new HttpEntity<>(schedJson, headers));
                }
                case "search_staff" -> {
                    Map<String, Object> staffBody = new java.util.LinkedHashMap<>();
                    staffBody.put("tenantId", tenantId);
                    staffBody.put("query", args.getOrDefault("query", ""));
                    if (args.get("branch_id") != null && !String.valueOf(args.get("branch_id")).isBlank()) {
                        staffBody.put("branchId", validateIdArg("branch_id", args.get("branch_id")));
                    }
                    String staffJson = mapper.writeValueAsString(staffBody);
                    yield post("/api/admin/ai/internal/staff/search", new HttpEntity<>(staffJson, headers));
                }
                case "get_available_slots" -> {
                    Map<String, Object> slotsBody = new java.util.LinkedHashMap<>();
                    slotsBody.put("tenantId", tenantId);
                    slotsBody.put("staffId", validateIdArg("staff_id", args.get("staff_id")));
                    slotsBody.put("date", args.get("date"));
                    Object dur = args.get("duration");
                    slotsBody.put("duration", dur != null ? dur : 60);
                    String slotsJson = mapper.writeValueAsString(slotsBody);
                    yield post("/api/admin/ai/internal/availability/slots", new HttpEntity<>(slotsJson, headers));
                }
                case "search_resources" -> post("/api/admin/ai/internal/resources/search", entity);
                case "get_branches" -> {
                    headers.set("X-Tenant-Id", tenantId);
                    Object bq = args.get("query");
                    yield get("/api/admin/ai/internal/branches",
                            bq != null && !String.valueOf(bq).isBlank() ? Map.of("query", bq) : Map.of(),
                            headers);
                }
                case "get_instructions" -> get("/api/admin/ai/internal/instructions", Map.of(), headers);
                case "check_availability" -> {
                    Map<String, Object> availBody = new java.util.LinkedHashMap<>();
                    availBody.put("tenantId", tenantId);
                    availBody.put("staffId", validateIdArg("staff_id", args.get("staff_id")));
                    availBody.put("date", args.get("date"));
                    availBody.put("time", args.get("time"));
                    availBody.put("duration", args.get("duration"));
                    availBody.put("resourceId", args.get("resource_id"));
                    String availJson = mapper.writeValueAsString(availBody);
                    yield post("/api/admin/ai/internal/availability", new HttpEntity<>(availJson, headers));
                }
                case "create_appointment" -> {
                    Map<String, Object> aptBody = new java.util.LinkedHashMap<>(args);
                    aptBody.put("tenantId", tenantId);
                    if (args.get("staffId") != null && !String.valueOf(args.get("staffId")).isBlank()) {
                        aptBody.put("staffId", validateIdArg("staffId", args.get("staffId")));
                    }
                    String aptJson = mapper.writeValueAsString(aptBody);
                    yield post("/api/admin/ai/internal/appointments", new HttpEntity<>(aptJson, headers));
                }
                case "get_appointment" -> {
                    headers.set("X-Tenant-Id", tenantId);
                    yield get("/api/admin/ai/internal/appointments/" + validateIdArg("appointment_id", args.get("appointment_id")), Map.of(), headers);
                }
                case "update_appointment" -> {
                    Map<String, Object> aptBody = new java.util.LinkedHashMap<>();
                    aptBody.put("tenantId", tenantId);
                    if (args.get("date_time") != null) aptBody.put("dateTime", args.get("date_time"));
                    if (args.get("service_name") != null) aptBody.put("serviceName", args.get("service_name"));
                    if (args.get("staff_name") != null) aptBody.put("staffName", args.get("staff_name"));
                    if (args.get("staff_id") != null && !String.valueOf(args.get("staff_id")).isBlank())
                        aptBody.put("staffId", validateIdArg("staff_id", args.get("staff_id")));
                    if (args.get("duration_minutes") != null) aptBody.put("durationMinutes", args.get("duration_minutes"));
                    String aptJson = mapper.writeValueAsString(aptBody);
                    yield put("/api/admin/ai/internal/appointments/" + validateIdArg("appointment_id", args.get("appointment_id")), new HttpEntity<>(aptJson, headers));
                }
                case "cancel_appointment" -> {
                    headers.set("X-Tenant-Id", tenantId);
                    yield delete("/api/admin/ai/internal/appointments/" + validateIdArg("appointment_id", args.get("appointment_id")), Map.of(), headers);
                }
                case "get_my_appointments" -> get("/api/admin/ai/internal/appointments/my", Map.of("tenantId", tenantId), headers);
                case "manage_notifications" -> put("/api/admin/ai/internal/notifications/preferences", entity);
                case "get_report" -> post("/api/admin/ai/internal/reports", entity);
                case "search_knowledge" -> post("/api/admin/ai/internal/knowledge/search", entity);
                case "search_knowledge_rag" -> {
                    Map<String, Object> ragBody = new java.util.LinkedHashMap<>();
                    ragBody.put("tenantId", tenantId);
                    ragBody.put("query", args.get("query"));
                    ragBody.put("topK", args.getOrDefault("topK", 5));
                    String ragJson = mapper.writeValueAsString(ragBody);
                    yield post("/api/admin/ai/internal/knowledge/rag-search", new HttpEntity<>(ragJson, headers));
                }
                default -> "{\"error\":\"Unknown tool: " + name + "\"}";
            };
            long elapsed = System.currentTimeMillis() - startMs;
            log.info("executeTool done: name={}, elapsed={}ms, response_len={}", name, elapsed, result != null ? result.length() : 0);
            log.debug("executeTool response preview: {}", result != null ? result.substring(0, Math.min(200, result.length())) : "null");
            return result;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startMs;
            log.error("executeTool failed: name={}, elapsed={}ms, error={}", name, elapsed, e.getMessage(), e);
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    private String post(String path, HttpEntity<String> entity) {
        return rest.postForObject(backendUrl + path, entity, String.class);
    }

    private String get(String path, Map<String, Object> params, HttpHeaders headers) {
        var uri = backendUrl + path;
        if (!params.isEmpty()) {
            uri += "?" + params.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .reduce((a, b) -> a + "&" + b)
                .orElse("");
        }
        var entity = new HttpEntity<>(headers);
        return rest.exchange(uri, HttpMethod.GET, entity, String.class).getBody();
    }

    private String put(String path, HttpEntity<String> entity) {
        return rest.exchange(backendUrl + path, HttpMethod.PUT, entity, String.class).getBody();
    }

    private String delete(String path, Map<String, Object> params, HttpHeaders headers) {
        var uri = backendUrl + path;
        if (!params.isEmpty()) {
            uri += "?" + params.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .reduce((a, b) -> a + "&" + b)
                .orElse("");
        }
        var entity = new HttpEntity<>(headers);
        return rest.exchange(uri, HttpMethod.DELETE, entity, String.class).getBody();
    }

    public record ToolDef(String name, String description, Map<String, Object> parameters) {}

    public List<ToolDef> getToolDefinitions() {
        return getToolSchemas().stream().map(schema -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> func = (Map<String, Object>) schema.get("function");
            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) func.get("parameters");
            return new ToolDef((String) func.get("name"), (String) func.get("description"), params);
        }).toList();
    }

    public List<Map<String, Object>> getToolSchemas() {
        return List.of(
            Map.of("type", "function", "function", Map.of(
                "name", "search_contacts", "description", "Search contacts by name or phone or get all",
                "parameters", Map.of("type", "object", "properties", Map.of(
                    "query", Map.of("type", "string", "description", "Search query (name or phone). Empty string returns all.")
                ), "required", List.of("query")))),
            Map.of("type", "function", "function", Map.of(
                "name", "get_contact", "description", "Get CRM contact (client) details by numeric ID. NOT for branches or services.",
                "parameters", Map.of("type", "object", "properties", Map.of(
                    "contact_id", Map.of("type", "string", "description", "Contact ID obtained from search_contacts, NOT a name")
                ), "required", List.of("contact_id")))),
            Map.of("type", "function", "function", Map.of(
                "name", "create_contact", "description", "Create a new contact",
                "parameters", Map.of("type", "object", "properties", Map.of(
                    "name", Map.of("type", "string", "description", "Contact name"),
                    "phone", Map.of("type", "string", "description", "Phone number"),
                    "email", Map.of("type", "string", "description", "Email"),
                    "notes", Map.of("type", "string", "description", "Notes")
                ), "required", List.of("name")))),
            Map.of("type", "function", "function", Map.of(
                "name", "update_contact", "description", "Update contact details",
                "parameters", Map.of("type", "object", "properties", Map.of(
                    "contact_id", Map.of("type", "string", "description", "Contact ID"),
                    "name", Map.of("type", "string", "description", "New name"),
                    "phone", Map.of("type", "string", "description", "New phone"),
                    "email", Map.of("type", "string", "description", "New email"),
                    "notes", Map.of("type", "string", "description", "New notes")
                ), "required", List.of("contact_id")))),
            Map.of("type", "function", "function", Map.of(
                "name", "delete_contact", "description", "Delete a contact",
                "parameters", Map.of("type", "object", "properties", Map.of(
                    "contact_id", Map.of("type", "string", "description", "Contact ID")
                ), "required", List.of("contact_id")))),
            Map.of("type", "function", "function", Map.of(
                "name", "search_services", "description", "Search services by name or get all",
                "parameters", Map.of("type", "object", "properties", Map.of(
                    "query", Map.of("type", "string", "description", "Service name or empty for all")
                ), "required", List.of("query")))),
            Map.of("type", "function", "function", Map.of(
                "name", "add_service", "description", "Add a new service",
                "parameters", Map.of("type", "object", "properties", Map.of(
                    "name", Map.of("type", "string", "description", "Service name"),
                    "duration_minutes", Map.of("type", "integer", "description", "Duration in minutes"),
                    "price_min", Map.of("type", "number", "description", "Minimum price"),
                    "price_max", Map.of("type", "number", "description", "Maximum price")
                ), "required", List.of("name", "duration_minutes")))),
            Map.of("type", "function", "function", Map.of(
                "name", "update_service", "description", "Update a service",
                "parameters", Map.of("type", "object", "properties", Map.of(
                    "service_id", Map.of("type", "string", "description", "Service ID"),
                    "name", Map.of("type", "string", "description", "New name"),
                    "duration_minutes", Map.of("type", "integer", "description", "New duration"),
                    "price_min", Map.of("type", "number", "description", "New min price"),
                    "price_max", Map.of("type", "number", "description", "New max price")
                ), "required", List.of("service_id")))),
            Map.of("type", "function", "function", Map.of(
                "name", "delete_service", "description", "Delete a service",
                "parameters", Map.of("type", "object", "properties", Map.of(
                    "service_id", Map.of("type", "string", "description", "Service ID")
                ), "required", List.of("service_id")))),
            Map.of("type", "function", "function", Map.of(
                "name", "search_staff", "description", "Search staff members by name or by branch. Response contains 'id' (staff ID) and 'branchIds' (list of branches where this staff works).",
                "parameters", Map.of("type", "object", "properties", Map.of(
                    "query", Map.of("type", "string", "description", "Staff name or empty for all"),
                    "branch_id", Map.of("type", "string", "description", "Branch ID to filter staff working in this branch (optional)")
                ), "required", List.of("query")))),
            Map.of("type", "function", "function", Map.of(
                "name", "get_staff_schedule", "description", "Get staff schedule for a date",
                "parameters", Map.of("type", "object", "properties", Map.of(
                    "staff_id", Map.of("type", "string", "description", "Staff ID"),
                    "date", Map.of("type", "string", "description", "Date YYYY-MM-DD")
                ), "required", List.of("staff_id", "date")))),
            Map.of("type", "function", "function", Map.of(
                "name", "search_resources", "description", "Search resources (rooms, equipment) by name",
                "parameters", Map.of("type", "object", "properties", Map.of(
                    "query", Map.of("type", "string", "description", "Resource name query or empty for all")
                ), "required", List.of("query")))),
            Map.of("type", "function", "function", Map.of(
                "name", "get_branches", "description", "Search branches by name (substring). Returns list of {id, name, address}. Use the 'id' field (NOT the name) as branchId for search_staff and create_appointment.",
                "parameters", Map.of("type", "object", "properties", Map.of(
                    "query", Map.of("type", "string", "description", "Branch name substring (optional, empty returns all)")
                ), "required", List.of()))),
            Map.of("type", "function", "function", Map.of(
                "name", "get_instructions", "description", "Get step-by-step instructions for complex tasks",
                "parameters", Map.of("type", "object", "properties", Map.of(
                    "task", Map.of("type", "string", "description", "Task name e.g. create_appointment")
                ), "required", List.of("task")))),
            Map.of("type", "function", "function", Map.of(
                "name", "check_availability", "description", "Check if one specific time slot is available for a staff member. For finding free slots on a day use get_available_slots instead.",
                "parameters", Map.of("type", "object", "properties", Map.of(
                    "staff_id", Map.of("type", "string", "description", "Staff ID obtained from search_staff"),
                    "date", Map.of("type", "string", "description", "Date YYYY-MM-DD"),
                    "time", Map.of("type", "string", "description", "Start time HH:MM"),
                    "duration", Map.of("type", "integer", "description", "Duration in minutes"),
                    "resource_id", Map.of("type", "string", "description", "Resource ID (optional)")
                ), "required", List.of("staff_id", "date", "time", "duration")))),
            Map.of("type", "function", "function", Map.of(
                "name", "get_available_slots", "description", "Get free time slots for a staff member on a given day. Returns array of {startTime, endTime} in HH:mm format. USE THIS tool for questions like 'when is the staff free', do NOT call check_availability for each hour separately.",
                "parameters", Map.of("type", "object", "properties", Map.of(
                    "staff_id", Map.of("type", "string", "description", "Staff ID obtained from search_staff"),
                    "date", Map.of("type", "string", "description", "Date YYYY-MM-DD"),
                    "duration", Map.of("type", "integer", "description", "Appointment duration in minutes (default 60)")
                ), "required", List.of("staff_id", "date")))),
            Map.of("type", "function", "function", Map.of(
                "name", "create_appointment", "description", "Create an appointment. serviceName and staffName are matched by contains; for exact match use staffId.dateTime must be ISO with offset, e.g. 2026-07-10T14:00:00+03:00.",
                "parameters", Map.of("type", "object", "properties", Map.of(
                    "clientName", Map.of("type", "string", "description", "Client name"),
                    "clientPhone", Map.of("type", "string", "description", "Client phone"),
                    "serviceName", Map.of("type", "string", "description", "Service name (matched by contains)"),
                    "dateTime", Map.of("type", "string", "description", "ISO datetime e.g. 2026-06-20T14:00:00+03:00"),
                    "staffName", Map.of("type", "string", "description", "Staff name (optional, matched by contains)"),
                    "staffId", Map.of("type", "string", "description", "Staff ID for exact match (recommended over staffName)"),
                    "branchId", Map.of("type", "string", "description", "Branch ID obtained from get_branches (optional)"),
                    "resourceId", Map.of("type", "string", "description", "Resource ID (optional)"),
                    "durationMinutes", Map.of("type", "integer", "description", "Duration in minutes (default 60)")
                ), "required", List.of("clientName", "serviceName", "dateTime")))),
            Map.of("type", "function", "function", Map.of(
                "name", "get_appointment", "description", "Get appointment details by ID",
                "parameters", Map.of("type", "object", "properties", Map.of(
                    "appointment_id", Map.of("type", "string", "description", "Appointment ID")
                ), "required", List.of("appointment_id")))),
            Map.of("type", "function", "function", Map.of(
                "name", "update_appointment", "description", "Update appointment details.",
                "parameters", Map.of("type", "object", "properties", Map.of(
                    "appointment_id", Map.of("type", "string", "description", "Appointment ID"),
                    "date_time", Map.of("type", "string", "description", "New ISO datetime (optional)"),
                    "service_name", Map.of("type", "string", "description", "New service name (optional)"),
                    "staff_name", Map.of("type", "string", "description", "New staff name (optional)"),
                    "staff_id", Map.of("type", "string", "description", "New staff ID for exact match (optional)"),
                    "duration_minutes", Map.of("type", "integer", "description", "New duration (optional)")
                ), "required", List.of("appointment_id")))),
            Map.of("type", "function", "function", Map.of(
                "name", "cancel_appointment", "description", "Cancel an appointment",
                "parameters", Map.of("type", "object", "properties", Map.of(
                    "appointment_id", Map.of("type", "string", "description", "Appointment ID")
                ), "required", List.of("appointment_id")))),
            Map.of("type", "function", "function", Map.of(
                "name", "get_my_appointments", "description", "Get your own appointments",
                "parameters", Map.of("type", "object", "properties", Map.of(
                ), "required", List.of()))),
            Map.of("type", "function", "function", Map.of(
                "name", "manage_notifications", "description", "Manage notification preferences",
                "parameters", Map.of("type", "object", "properties", Map.of(
                    "enabled", Map.of("type", "boolean", "description", "Enable/disable notifications"),
                    "remind_before_minutes", Map.of("type", "integer", "description", "Minutes before appointment to remind")
                ), "required", List.of("enabled")))),
            Map.of("type", "function", "function", Map.of(
                "name", "get_report", "description", "Generate a report",
                "parameters", Map.of("type", "object", "properties", Map.of(
                    "type", Map.of("type", "string", "description", "Report type e.g. appointments_summary"),
                    "startDate", Map.of("type", "string", "description", "Start date YYYY-MM-DD"),
                    "endDate", Map.of("type", "string", "description", "End date YYYY-MM-DD")
                ), "required", List.of("type", "startDate", "endDate")))),
            Map.of("type", "function", "function", Map.of(
                "name", "search_knowledge", "description", "Search knowledge base",
                "parameters", Map.of("type", "object", "properties", Map.of(
                    "query", Map.of("type", "string", "description", "Search query")
                ), "required", List.of("query")))),
            Map.of("type", "function", "function", Map.of(
                "name", "search_knowledge_rag", "description", "Semantic search in knowledge base using AI",
                "parameters", Map.of("type", "object", "properties", Map.of(
                    "query", Map.of("type", "string", "description", "Search query"),
                    "topK", Map.of("type", "integer", "description", "Number of results (default 5)")
                ), "required", List.of("query"))))
        );
    }
}
