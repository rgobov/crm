package com.tryneuro.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ContactService contactService;
    private final StaffMemberService staffMemberService;
    private final ScheduleService scheduleService;
    private final ResourceService resourceService;
    private final ReturnReminderService returnReminderService;

    /**
     * Собирает общую статистику для дашборда
     */
    public Map<String, Object> getAdminStats(String tenantId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalClients", contactService.countContacts(tenantId));
        stats.put("totalStaff", staffMemberService.getAllStaff(tenantId).size());

        // Сегодняшние записи по всей компании
        stats.put("todayAppointments", scheduleService.getAppointmentsForDay(LocalDate.now(), tenantId, null).size());

        // Количество ресурсов
        stats.put("totalResources", resourceService.getResources(tenantId, null).size());

        // Клиенты, не бывшие 30+ дней
        stats.put("returnReminderCount", returnReminderService.getCount(tenantId, 30));

        return stats;
    }
}
