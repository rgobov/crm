package com.tryneuro.backend.service;

import com.tryneuro.backend.dto.WorkloadDto;
import com.tryneuro.backend.dto.AppointmentDto;
import com.tryneuro.backend.dto.DtoMapper;
import com.tryneuro.backend.model.Appointment;
import com.tryneuro.backend.model.AppointmentStatus;
import com.tryneuro.backend.model.Branch;
import com.tryneuro.backend.model.Contact;
import com.tryneuro.backend.model.StaffShift;
import com.tryneuro.backend.repository.AppointmentRepository;
import com.tryneuro.backend.repository.BranchRepository;
import com.tryneuro.backend.repository.StaffShiftRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ScheduleService {
    private static final Logger log = LoggerFactory.getLogger(ScheduleService.class);

    private final AppointmentRepository appointmentRepository;
    private final StaffShiftRepository staffShiftRepository;
    private final BranchRepository branchRepository;
    private final ContactService contactService;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public ScheduleService(AppointmentRepository appointmentRepository, 
                           StaffShiftRepository staffShiftRepository,
                           BranchRepository branchRepository,
                           ContactService contactService,
                           SimpMessagingTemplate messagingTemplate) {
        this.appointmentRepository = appointmentRepository;
        this.staffShiftRepository = staffShiftRepository;
        this.branchRepository = branchRepository;
        this.contactService = contactService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Обогащает запись визита телефоном из контакта, если он отсутствует.
     * Это решает проблему отображения для старых записей.
     */
    private void enrichAppointmentPhone(Appointment app) {
        if ((app.getClientPhone() == null || app.getClientPhone().isEmpty()) && app.getContactId() != null) {
            contactService.getContactById(app.getContactId()).ifPresent(contact -> {
                if (contact.getPhones() != null && !contact.getPhones().isEmpty()) {
                    app.setClientPhone(contact.getPhones().get(0));
                }
            });
        }
    }

    @Transactional
    public Appointment addAppointment(Appointment appointment) {
        return addAppointment(appointment, null, false);
    }

    @Transactional
    public Appointment addAppointment(Appointment appointment, java.util.List<String> staffMemberIds, boolean force) {
        log.info("🚀 [ADD] Creating appointment for client '{}' in branch '{}', force: {}", appointment.getClientName(), appointment.getBranchId(), force);
        
        enrichAppointmentPhone(appointment);
        
        if (staffMemberIds != null && staffMemberIds.size() > 1) {
            String groupId = java.util.UUID.randomUUID().toString();
            Appointment firstSaved = null;
            
            for (String staffId : staffMemberIds) {
                Appointment appCopy = copyAppointmentForStaff(appointment, staffId, groupId);
                
                if (!force) {
                    validateAvailability(appCopy);
                }
                
                Appointment saved = appointmentRepository.save(appCopy);
                if (firstSaved == null) {
                    firstSaved = saved;
                }
                
                notifyAppointmentChange(saved, "CREATED");
            }
            
            if (firstSaved != null && firstSaved.getContactId() != null && firstSaved.getReferenceTag() != null && !firstSaved.getReferenceTag().isEmpty()) {
                contactService.addTagIfMissing(firstSaved.getContactId(), firstSaved.getReferenceTag());
            }
            
            return firstSaved;
        } else {
            if (staffMemberIds != null && !staffMemberIds.isEmpty()) {
                appointment.setStaffMemberId(staffMemberIds.get(0));
            }
            
            if (!force) {
                validateAvailability(appointment);
            }
            
            Appointment saved = appointmentRepository.save(appointment);
            if (saved.getContactId() != null && saved.getReferenceTag() != null && !saved.getReferenceTag().isEmpty()) {
                contactService.addTagIfMissing(saved.getContactId(), saved.getReferenceTag());
            }
            notifyAppointmentChange(saved, "CREATED");
            return saved;
        }
    }

    private Appointment copyAppointmentForStaff(Appointment source, String staffId, String groupId) {
        Appointment copy = new Appointment();
        copy.setStartTime(source.getStartTime());
        copy.setDurationInMinutes(source.getDurationInMinutes());
        copy.setClientName(source.getClientName());
        copy.setClientPhone(source.getClientPhone());
        copy.setContactId(source.getContactId());
        copy.setService(source.getService());
        copy.setResourceId(source.getResourceId());
        copy.setStaffMemberId(staffId);
        copy.setGroupId(groupId);
        copy.setBranchId(source.getBranchId());
        copy.setStatus(source.getStatus());
        copy.setComment(source.getComment());
        copy.setReferenceTag(source.getReferenceTag());
        copy.setAllowReminder(source.isAllowReminder());
        copy.setReminderLeadTimeHours(source.getReminderLeadTimeHours());
        copy.setTenantId(source.getTenantId());
        return copy;
    }

    @Transactional
    public Appointment updateAppointment(String id, Appointment details) {
        return updateAppointment(id, details, null, "single", false);
    }

    @Transactional
    public Appointment updateAppointment(String id, Appointment details, java.util.List<String> staffMemberIds, String updateMode, boolean force) {
        log.info("🔄 [UPDATE] Saving changes for appointment ID: {}. Client: {}, mode: {}, force: {}", id, details.getClientName(), updateMode, force);
        Appointment appointment = appointmentRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Запись не найдена"));
        
        enrichAppointmentPhone(details);
        
        boolean isGroupUpdate = "all".equalsIgnoreCase(updateMode) || (staffMemberIds != null && staffMemberIds.size() > 1);
        
        if (isGroupUpdate) {
            String groupId = appointment.getGroupId();
            if (groupId == null) {
                groupId = java.util.UUID.randomUUID().toString();
                appointment.setGroupId(groupId);
                appointmentRepository.save(appointment);
            }
            
            List<Appointment> groupApps = appointmentRepository.findByGroupId(groupId);
            
            java.util.Set<String> existingStaffIds = groupApps.stream()
                    .map(Appointment::getStaffMemberId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
                    
            java.util.Set<String> targetStaffIds = staffMemberIds != null ? new java.util.HashSet<>(staffMemberIds) : new java.util.HashSet<>();
            if (targetStaffIds.isEmpty() && details.getStaffMemberId() != null) {
                targetStaffIds.add(details.getStaffMemberId());
            }
            
            // Удаляем записи для сотрудников, которых убрали из группы
            for (Appointment app : groupApps) {
                if (!targetStaffIds.contains(app.getStaffMemberId())) {
                    notifyAppointmentChange(app, "DELETED");
                    appointmentRepository.delete(app);
                }
            }
            
            Appointment updatedMain = null;
            // Обновляем существующие записи в группе
            for (Appointment app : groupApps) {
                if (targetStaffIds.contains(app.getStaffMemberId())) {
                    app.setStartTime(details.getStartTime());
                    app.setDurationInMinutes(details.getDurationInMinutes());
                    app.setClientName(details.getClientName());
                    app.setClientPhone(details.getClientPhone());
                    app.setContactId(details.getContactId());
                    app.setService(details.getService());
                    app.setResourceId(details.getResourceId());
                    app.setStatus(details.getStatus());
                    app.setComment(details.getComment());
                    app.setAllowReminder(details.isAllowReminder());
                    app.setReminderLeadTimeHours(details.getReminderLeadTimeHours());
                    app.setReferenceTag(details.getReferenceTag());
                    if (details.getBranchId() != null) {
                        app.setBranchId(details.getBranchId());
                    }
                    
                    if (!force) {
                        validateAvailability(app);
                    }
                    
                    Appointment saved = appointmentRepository.save(app);
                    if (app.getId().equals(id)) {
                        updatedMain = saved;
                    }
                    notifyAppointmentChange(saved, "UPDATED");
                }
            }
            
            // Добавляем новые записи для добавленных сотрудников
            for (String staffId : targetStaffIds) {
                if (!existingStaffIds.contains(staffId)) {
                    Appointment newApp = copyAppointmentForStaff(details, staffId, groupId);
                    if (!force) {
                        validateAvailability(newApp);
                    }
                    Appointment saved = appointmentRepository.save(newApp);
                    notifyAppointmentChange(saved, "CREATED");
                }
            }
            
            if (updatedMain == null && !groupApps.isEmpty()) {
                updatedMain = groupApps.get(0);
            }
            
            return updatedMain != null ? updatedMain : details;
            
        } else {
            if (staffMemberIds != null && !staffMemberIds.isEmpty()) {
                appointment.setStaffMemberId(staffMemberIds.get(0));
            } else {
                appointment.setStaffMemberId(details.getStaffMemberId());
            }
            
            appointment.setStartTime(details.getStartTime());
            appointment.setDurationInMinutes(details.getDurationInMinutes());
            appointment.setClientName(details.getClientName());
            appointment.setClientPhone(details.getClientPhone());
            appointment.setContactId(details.getContactId());
            appointment.setService(details.getService());
            appointment.setResourceId(details.getResourceId());
            appointment.setStatus(details.getStatus());
            appointment.setComment(details.getComment());
            appointment.setAllowReminder(details.isAllowReminder());
            appointment.setReminderLeadTimeHours(details.getReminderLeadTimeHours());
            appointment.setReferenceTag(details.getReferenceTag());
            if (details.getBranchId() != null) {
                appointment.setBranchId(details.getBranchId());
            }
            
            if (appointment.getGroupId() != null && !"all".equalsIgnoreCase(updateMode)) {
                appointment.setGroupId(null);
            }
            
            if (!force) {
                validateAvailability(appointment);
            }
            
            Appointment updated = appointmentRepository.save(appointment);
            if (updated.getContactId() != null && updated.getReferenceTag() != null && !updated.getReferenceTag().isEmpty()) {
                contactService.addTagIfMissing(updated.getContactId(), updated.getReferenceTag());
            }
            notifyAppointmentChange(updated, "UPDATED");
            return updated;
        }
    }

    public List<Appointment> getAppointmentsForDay(LocalDate date, String tenantId, String branchId) {
        List<Appointment> apps;
        if (isRentBranch(branchId)) {
            // RENT: многодневная аренда. Границы дня филиала в UTC, чтобы запись,
            // начавшаяся в первые часы дня, не терялась из-за UTC-смещения, и чтобы
            // аренда, покрывающая несколько дней, была видна на каждом из них.
            String tz = branchRepository.findById(branchId).map(Branch::getTimezone).orElse("Europe/Moscow");
            ZonedDateTime dayStart = date.atStartOfDay(ZoneId.of(tz));
            ZonedDateTime dayEnd = dayStart.plusDays(1);
            apps = appointmentRepository.findSpanningDay(tenantId, branchId,
                    dayStart.toOffsetDateTime(), dayEnd.toOffsetDateTime());
        } else {
            apps = appointmentRepository.findByDateAndTenantIdAndBranchId(date, tenantId, branchId);
        }
        // Обогащаем КАЖДУЮ запись телефоном перед отправкой на фронтенд
        apps.forEach(this::enrichAppointmentPhone);
        return apps;
    }

    public AppointmentDto convertToDtoWithGroupStaff(Appointment app) {
        AppointmentDto dto = DtoMapper.toDto(app);
        if (app != null) {
            if (app.getGroupId() != null) {
                List<Appointment> groupApps = appointmentRepository.findByGroupId(app.getGroupId());
                dto.setStaffMemberIds(groupApps.stream()
                        .map(Appointment::getStaffMemberId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
            } else {
                dto.setStaffMemberIds(app.getStaffMemberId() != null ? 
                        List.of(app.getStaffMemberId()) : List.of());
            }
        }
        return dto;
    }

    private void validateAvailability(Appointment app) {
        String timezone = branchRepository.findById(app.getBranchId())
                .map(b -> b.getTimezone())
                .orElse("Europe/Moscow");

        boolean rent = isRentBranch(app.getBranchId());
        String appId = (app.getId() == null || app.getId().equals("new")) ? null : app.getId();

        if (rent) {
            // RENT: аренда может длиться до 30 дней, ресурс доступен 24/7 без смен.
            int duration = app.getDurationInMinutes() != null ? app.getDurationInMinutes() : 0;
            if (duration < 15 || duration > 43200) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Длительность аренды должна быть от 15 минут до 30 дней");
            }
            if (app.getResourceId() != null && !isResourceAvailableSpan(app.getTenantId(), app.getResourceId(),
                    app.getStartTime(), app.getStartTime().plusMinutes(duration), appId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ресурс занят в это время");
            }
            return;
        }

        ZonedDateTime branchDateTime = app.getStartTime().atZoneSameInstant(ZoneId.of(timezone));
        LocalDate localDate = branchDateTime.toLocalDate();
        LocalTime localTime = branchDateTime.toLocalTime();

        // Проверяем доступность мастера
        if (app.getStaffMemberId() != null && !isStaffMemberAvailable(app.getTenantId(), app.getStaffMemberId(), localDate, localTime, app.getDurationInMinutes(), appId, app.getBranchId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Мастер занят или не работает в этом филиале в это время");
        }

        // Проверяем доступность ресурса
        if (app.getResourceId() != null && !isResourceAvailable(app.getTenantId(), app.getResourceId(), localDate, localTime, app.getDurationInMinutes(), appId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ресурс занят в это время");
        }
    }

    private boolean isRentBranch(String branchId) {
        if (branchId == null || branchId.isBlank()) return false;
        return "RENT".equals(branchRepository.findById(branchId).map(Branch::getNiche).orElse(null));
    }

    // RENT: проверка пересечения аренды [startUtc, endUtc) с другими записями ресурса
    public boolean isResourceAvailableSpan(String tenantId, String resourceId, OffsetDateTime startUtc, OffsetDateTime endUtc, String currentAppId) {
        List<Appointment> allResourceApps = appointmentRepository.findResourceSpan(tenantId, resourceId, startUtc, endUtc);
        for (Appointment existing : allResourceApps) {
            if (currentAppId != null && existing.getId().equals(currentAppId)) continue;
            return false;
        }
        return true;
    }

    public boolean isStaffMemberAvailable(String tenantId, String staffId, LocalDate date, LocalTime time, int duration, String currentAppId, String branchId) {
        Optional<StaffShift> shiftInBranch = staffShiftRepository.findByStaffIdAndDateAndBranchId(staffId, date, branchId);
        
        if (shiftInBranch.isEmpty() || shiftInBranch.get().isDayOff()) return false;

        StaffShift shift = shiftInBranch.get();
        LocalTime start = time.truncatedTo(ChronoUnit.MINUTES);
        LocalTime end = start.plusMinutes(duration);
        
        LocalTime sStart = shift.getWorkStartTime();
        LocalTime sEnd = shift.getWorkEndTime();

        boolean withinHours;
        if (sEnd.isAfter(sStart)) {
            withinHours = !start.isBefore(sStart) && !end.isAfter(sEnd);
        } else {
            withinHours = !start.isBefore(sStart) || !end.isAfter(sEnd);
        }

        if (!withinHours) return false;

        if (shift.getBreakStartTime() != null && shift.getBreakEndTime() != null) {
            if (start.isBefore(shift.getBreakEndTime()) && end.isAfter(shift.getBreakStartTime())) return false;
        }

        List<Appointment> allStaffApps = appointmentRepository.findByTenantIdAndStaffMemberIdAndDate(tenantId, staffId, date);
        for (Appointment existing : allStaffApps) {
            if (currentAppId != null && existing.getId().equals(currentAppId)) continue;
            if (existing.getStatus() == AppointmentStatus.CANCELLED) continue;
            
            LocalTime eStart = existing.getTime().truncatedTo(ChronoUnit.MINUTES);
            LocalTime eEnd = eStart.plusMinutes(existing.getDurationInMinutes());
            
            if (start.isBefore(eEnd) && end.isAfter(eStart)) return false;
        }
        return true;
    }

    public boolean isResourceAvailable(String tenantId, String resourceId, LocalDate date, LocalTime time, int duration, String currentAppId) {
        List<Appointment> allResourceApps = appointmentRepository.findByResourceIdAndDate(resourceId, date);
        
        LocalTime start = time.truncatedTo(ChronoUnit.MINUTES);
        LocalTime end = start.plusMinutes(duration);
        
        for (Appointment existing : allResourceApps) {
            if (currentAppId != null && existing.getId().equals(currentAppId)) continue;
            if (existing.getStatus() == AppointmentStatus.CANCELLED) continue;
            
            LocalTime eStart = existing.getTime().truncatedTo(ChronoUnit.MINUTES);
            LocalTime eEnd = eStart.plusMinutes(existing.getDurationInMinutes());
            
            if (start.isBefore(eEnd) && end.isAfter(eStart)) return false;
        }
        return true;
    }

    public record StaffSlotInfo(java.util.List<java.util.Map<String, String>> slots, String reason, boolean hasAvailability) {}

    public java.util.List<java.util.Map<String, String>> getAvailableSlots(String tenantId, String staffId, LocalDate date, int duration) {
        StaffSlotInfo info = getAvailableSlotsForBranch(tenantId, staffId, null, date, duration);
        return info.slots();
    }

    public StaffSlotInfo getAvailableSlotsForBranch(String tenantId, String staffId, String branchId, LocalDate date, int duration) {
        java.util.Optional<StaffShift> shiftOpt;
        if (branchId != null && !branchId.isBlank()) {
            shiftOpt = staffShiftRepository.findByStaffIdAndDateAndBranchId(staffId, date, branchId);
        } else {
            List<StaffShift> shifts = staffShiftRepository.findByStaffIdAndDate(staffId, date);
            shiftOpt = shifts.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(shifts.get(0));
        }
        if (shiftOpt.isEmpty()) {
            return new StaffSlotInfo(List.of(), "no_shifts", false);
        }
        StaffShift shift = shiftOpt.get();
        if (shift.isDayOff()) {
            return new StaffSlotInfo(List.of(), "day_off", false);
        }
        LocalTime workStart = shift.getWorkStartTime();
        LocalTime workEnd = shift.getWorkEndTime();
        if (workStart == null || workEnd == null) {
            return new StaffSlotInfo(List.of(), "no_shifts", false);
        }
        int ws = workStart.toSecondOfDay() / 60;
        int we = workEnd.toSecondOfDay() / 60;
        int dayEnd = 24 * 60;
        if (we <= ws) {
            we = dayEnd;
        }
        int bs = shift.getBreakStartTime() != null ? shift.getBreakStartTime().toSecondOfDay() / 60 : -1;
        int be = shift.getBreakEndTime() != null ? shift.getBreakEndTime().toSecondOfDay() / 60 : -1;
        List<Appointment> existing = getAppointmentsForStaff(tenantId, staffId, date);
        List<int[]> busy = new ArrayList<>();
        for (Appointment a : existing) {
            if (a.getStatus() == AppointmentStatus.CANCELLED) {
                continue;
            }
            int es = a.getTime().toSecondOfDay() / 60;
            int ee = es + a.getDurationInMinutes();
            busy.add(new int[]{es, ee});
        }
        int step = 15;
        List<Map<String, String>> slots = new ArrayList<>();
        for (int s = ws; s + duration <= we; s += step) {
            int e = s + duration;
            if (e > dayEnd) {
                break;
            }
            boolean overlapsBreak = bs >= 0 && s < be && e > bs;
            if (overlapsBreak) {
                continue;
            }
            boolean overlapsExisting = false;
            for (int[] b : busy) {
                if (s < b[1] && e > b[0]) {
                    overlapsExisting = true;
                    break;
                }
            }
            if (overlapsExisting) {
                continue;
            }
            slots.add(Map.of(
                    "startTime", LocalTime.of(s / 60, s % 60).toString(),
                    "endTime", LocalTime.of(e / 60, e % 60).toString()
            ));
        }
        boolean hasAvailability = !slots.isEmpty();
        String reason = hasAvailability ? "free" : "fully_booked";
        return new StaffSlotInfo(slots, reason, hasAvailability);
    }

    public List<Appointment> getAppointmentsForStaff(String tenantId, String staffId, LocalDate date) {
        List<Appointment> apps = appointmentRepository.findByTenantIdAndStaffMemberIdAndDate(tenantId, staffId, date);
        apps.forEach(this::enrichAppointmentPhone);
        return apps;
    }

    public List<WorkloadDto> getWorkloadForStaffAndMonth(String staffId, int year, int month) {
        return appointmentRepository.getWorkloadForStaffAndMonth(staffId, year, month);
    }

    public List<Appointment> getAppointmentsForContact(String contactId, String tenantId) {
        List<Appointment> apps = appointmentRepository.findByContactIdAndTenantIdOrderByDateDesc(contactId, tenantId);
        apps.forEach(this::enrichAppointmentPhone);
        return apps;
    }

    public List<Appointment> getAppointmentsForStaffAll(String tenantId, String staffId) {
        List<Appointment> apps = appointmentRepository.findByTenantIdAndStaffMemberId(tenantId, staffId);
        apps.forEach(this::enrichAppointmentPhone);
        return apps;
    }

    public List<Appointment> getAppointmentsByTenant(String tenantId) {
        List<Appointment> apps = appointmentRepository.findByTenantId(tenantId);
        apps.forEach(this::enrichAppointmentPhone);
        return apps;
    }

    public Optional<Appointment> getAppointmentById(String id) {
        return appointmentRepository.findById(id);
    }

    @Transactional
    public void deleteAppointment(String id) {
        deleteAppointment(id, "single");
    }

    @Transactional
    public void deleteAppointment(String id, String deleteMode) {
        appointmentRepository.findById(id).ifPresent(app -> {
            if ("all".equalsIgnoreCase(deleteMode) && app.getGroupId() != null) {
                List<Appointment> groupApps = appointmentRepository.findByGroupId(app.getGroupId());
                for (Appointment groupApp : groupApps) {
                    notifyAppointmentChange(groupApp, "DELETED");
                    appointmentRepository.deleteById(groupApp.getId());
                }
            } else {
                notifyAppointmentChange(app, "DELETED");
                appointmentRepository.deleteById(id);
            }
        });
    }

    private void notifyAppointmentChange(Appointment app, String changeType) {
        if (app != null && app.getTenantId() != null) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "APPOINTMENT_" + changeType);
            payload.put("appointmentId", app.getId());
            payload.put("branchId", app.getBranchId());
            payload.put("date", app.getDate().toString());
            payload.put("timestamp", System.currentTimeMillis());
            
            if (!"DELETED".equals(changeType)) {
                payload.put("staffId", app.getStaffMemberId());
                payload.put("status", app.getStatus());
            }

            // ✅ Отправляем ТОЛЬКО после коммита транзакции
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        messagingTemplate.convertAndSend("/topic/schedule/" + app.getTenantId(), payload);
                    }
                });
            } else {
                messagingTemplate.convertAndSend("/topic/schedule/" + app.getTenantId(), payload);
            }
        }
    }

    private void notifyChange(String tenantId, String type, String branchId, LocalDate date) {
        if (tenantId == null) return;

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", type != null ? type : "SCHEDULE_UPDATED");
        payload.put("timestamp", System.currentTimeMillis());
        if (branchId != null) payload.put("branchId", branchId);
        if (date != null) payload.put("date", date.toString());

        // ✅ Отправляем ТОЛЬКО после коммита транзакции
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    messagingTemplate.convertAndSend("/topic/schedule/" + tenantId, payload);
                }
            });
        } else {
            messagingTemplate.convertAndSend("/topic/schedule/" + tenantId, payload);
        }
    }

    private void notifyChange(String tenantId) {
        notifyChange(tenantId, "SCHEDULE_UPDATED", null, null);
    }

    public List<WorkloadDto> getWorkloadForMonth(String tenantId, int year, int month, String branchId) {
        if (branchId == null || branchId.isEmpty() || "null".equals(branchId)) {
            return appointmentRepository.getWorkloadForMonth(tenantId, year, month);
        } else {
            return appointmentRepository.getWorkloadForMonthAndBranch(tenantId, year, month, branchId);
        }
    }
}
