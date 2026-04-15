package com.tryneuro.backend.dto;

import com.tryneuro.backend.model.*;
import java.util.Base64;
import java.util.stream.Collectors;
import java.util.Collections;

public class DtoMapper {

    public static AppointmentDto toDto(Appointment entity) {
        if (entity == null) return null;
        return AppointmentDto.builder()
                .id(entity.getId())
                .startTime(entity.getStartTime())
                .durationInMinutes(entity.getDurationInMinutes())
                .clientName(entity.getClientName())
                .clientPhone(entity.getClientPhone())
                .contactId(entity.getContactId())
                .service(entity.getService())
                .resourceId(entity.getResourceId())
                .staffMemberId(entity.getStaffMemberId())
                .branchId(entity.getBranchId())
                .status(entity.getStatus())
                .comment(entity.getComment())
                .referenceTag(entity.getReferenceTag())
                .reminderSent(entity.isReminderSent())
                .allowReminder(entity.isAllowReminder())
                .reminderLeadTimeHours(entity.getReminderLeadTimeHours())
                .build();
    }

    public static Appointment toEntity(AppointmentDto dto, String tenantId) {
        if (dto == null) return null;
        Appointment entity = new Appointment();
        entity.setId(dto.getId());
        entity.setStartTime(dto.getStartTime());
        entity.setDurationInMinutes(dto.getDurationInMinutes());
        entity.setClientName(dto.getClientName());
        entity.setClientPhone(dto.getClientPhone());
        entity.setContactId(dto.getContactId());
        entity.setService(dto.getService());
        entity.setResourceId(dto.getResourceId());
        entity.setStaffMemberId(dto.getStaffMemberId());
        entity.setBranchId(dto.getBranchId());
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : AppointmentStatus.SCHEDULED);
        entity.setComment(dto.getComment());
        entity.setReferenceTag(dto.getReferenceTag());
        entity.setAllowReminder(dto.isAllowReminder());
        entity.setReminderLeadTimeHours(dto.getReminderLeadTimeHours() != null ? dto.getReminderLeadTimeHours() : 24);
        entity.setTenantId(tenantId);
        return entity;
    }

    public static ContactDto toDto(Contact entity) {
        if (entity == null) return null;
        return ContactDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .phones(entity.getPhones())
                .email(entity.getEmail())
                .notes(entity.getNotes())
                .tags(entity.getTags())
                .build();
    }

    public static Contact toEntity(ContactDto dto, String tenantId) {
        if (dto == null) return null;
        Contact entity = new Contact();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setPhones(dto.getPhones());
        entity.setEmail(dto.getEmail());
        entity.setNotes(dto.getNotes());
        entity.setTags(dto.getTags());
        entity.setTenantId(tenantId);
        return entity;
    }

    public static BranchDto toDto(Branch entity) {
        if (entity == null) return null;
        return BranchDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .address(entity.getAddress())
                .timezone(entity.getTimezone())
                .build();
    }

    public static Branch toEntity(BranchDto dto, String tenantId) {
        if (dto == null) return null;
        Branch entity = new Branch();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setAddress(dto.getAddress());
        entity.setTimezone(dto.getTimezone());
        entity.setTenantId(tenantId);
        return entity;
    }

    public static ServiceDto toDto(Service entity) {
        if (entity == null) return null;
        return ServiceDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .durationInMinutes(entity.getDurationInMinutes())
                .build();
    }

    public static Service toEntity(ServiceDto dto, String tenantId) {
        if (dto == null) return null;
        Service entity = new Service();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setDurationInMinutes(dto.getDurationInMinutes());
        entity.setTenantId(tenantId);
        return entity;
    }

    public static ResourceDto toDto(Resource entity) {
        if (entity == null) return null;
        return ResourceDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .branchId(entity.getBranchId())
                .build();
    }

    public static Resource toEntity(ResourceDto dto, String tenantId) {
        if (dto == null) return null;
        Resource entity = new Resource();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setBranchId(dto.getBranchId());
        entity.setTenantId(tenantId);
        return entity;
    }

    public static StaffMemberDto toDto(StaffMember entity) {
        if (entity == null) return null;
        
        java.util.List<String> branchIds = Collections.emptyList();
        if (entity.getBranches() != null) {
            branchIds = entity.getBranches().stream()
                    .map(Branch::getId)
                    .collect(Collectors.toList());
        }

        String photoDataBase64 = null;
        if (entity.getPhotoData() != null && entity.getPhotoData().length > 0) {
            photoDataBase64 = Base64.getEncoder().encodeToString(entity.getPhotoData());
        }

        return StaffMemberDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .specialty(entity.getSpecialty())
                .phone(entity.getPhone())
                .photoUrl(entity.getPhotoUrl())
                .photoData(photoDataBase64)
                .photoUpdatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toInstant(ZoneOffset.UTC).toEpochMilli() : null)
                .active(entity.isActive())
                .role(entity.getRole())
                .email(entity.getEmail())
                .branchIds(branchIds)
                .workStartTime(entity.getWorkStartTime())
                .workEndTime(entity.getWorkEndTime())
                .breakStartTime(entity.getBreakStartTime())
                .breakEndTime(entity.getBreakEndTime())
                .isDayOff(entity.isDayOff())
                .build();
    }
}
