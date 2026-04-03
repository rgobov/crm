import 'package:flutter/material.dart';

enum AppointmentStatus { scheduled, confirmed, arrived, needs_call, completed, cancelled }

class Appointment {
  final String id;
  final DateTime startTime;
  final int durationInMinutes;
  final String clientName;
  final String? clientPhone;
  final String? contactId;
  final String service;
  final String? resourceId;
  final String? staffMemberId;
  final String? branchId;
  final AppointmentStatus status;
  final String? comment;
  final String? referenceTag;
  final String? tenantId;
  final DateTime? createdAt;
  final bool reminderSent;
  final bool allowReminder;
  final int reminderLeadTimeHours;

  Appointment({
    required this.id,
    required this.startTime,
    required this.durationInMinutes,
    required this.clientName,
    this.clientPhone,
    this.contactId,
    required this.service,
    this.resourceId,
    this.staffMemberId,
    this.branchId,
    required this.status,
    this.comment,
    this.referenceTag,
    this.tenantId,
    this.createdAt,
    this.reminderSent = false,
    this.allowReminder = true,
    this.reminderLeadTimeHours = 24,
  });

  factory Appointment.fromJson(Map<String, dynamic> json) {
    return Appointment(
      id: json['id'] ?? '',
      startTime: DateTime.parse(json['startTime'] as String).toLocal(),
      durationInMinutes: json['durationInMinutes'] ?? 60,
      clientName: json['clientName'] ?? 'Без имени',
      clientPhone: json['clientPhone'],
      contactId: json['contactId'],
      service: json['service'] ?? 'Услуга',
      resourceId: json['resourceId'],
      staffMemberId: json['staffMemberId'],
      branchId: json['branchId'],
      status: AppointmentStatus.values.firstWhere(
        (e) => e.name.toUpperCase() == (json['status'] as String?)?.toUpperCase(),
        orElse: () => AppointmentStatus.scheduled,
      ),
      comment: json['comment'],
      referenceTag: json['referenceTag'],
      tenantId: json['tenantId'],
      createdAt: json['createdAt'] != null ? DateTime.parse(json['createdAt'] as String).toLocal() : null,
      reminderSent: json['reminderSent'] ?? false,
      allowReminder: json['allowReminder'] ?? true,
      reminderLeadTimeHours: json['reminderLeadTimeHours'] ?? 24,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'startTime': startTime.toUtc().toIso8601String(),
      'durationInMinutes': durationInMinutes,
      'clientName': clientName,
      'clientPhone': clientPhone,
      'contactId': contactId,
      'service': service,
      'resourceId': resourceId,
      'staffMemberId': staffMemberId,
      'branchId': branchId,
      'status': status.name.toUpperCase(),
      'comment': comment,
      'referenceTag': referenceTag,
      'allowReminder': allowReminder,
      'reminderLeadTimeHours': reminderLeadTimeHours,
    };
  }
}
