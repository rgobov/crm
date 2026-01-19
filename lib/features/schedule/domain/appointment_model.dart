import 'package:flutter/material.dart';

enum AppointmentStatus { scheduled, confirmed, needs_call, completed, cancelled }

class Appointment {
  final String id;
  final DateTime startTime;
  final int durationInMinutes;
  final String clientName;
  final String? contactId;
  final String service;
  final String? resourceId;
  final String? staffMemberId;
  final AppointmentStatus status;
  final String? tenantId;
  final DateTime? createdAt;
  final bool reminderSent;

  Appointment({
    required this.id,
    required this.startTime,
    required this.durationInMinutes,
    required this.clientName,
    this.contactId,
    required this.service,
    this.resourceId,
    this.staffMemberId,
    required this.status,
    this.tenantId,
    this.createdAt,
    this.reminderSent = false,
  });

  DateTime get date => DateTime(startTime.year, startTime.month, startTime.day);
  TimeOfDay get time => TimeOfDay(hour: startTime.hour, minute: startTime.minute);

  factory Appointment.fromJson(Map<String, dynamic> json) {
    return Appointment(
      id: json['id'],
      startTime: DateTime.parse(json['startTime'] as String).toLocal(),
      durationInMinutes: json['durationInMinutes'],
      clientName: json['clientName'],
      contactId: json['contactId'],
      service: json['service'],
      resourceId: json['resourceId'],
      staffMemberId: json['staffMemberId'],
      status: AppointmentStatus.values.firstWhere(
        (e) => e.name.toUpperCase() == (json['status'] as String?)?.toUpperCase(),
        orElse: () => AppointmentStatus.scheduled,
      ),
      tenantId: json['tenantId'],
      createdAt: json['createdAt'] != null ? DateTime.parse(json['createdAt'] as String).toLocal() : null,
      reminderSent: json['reminderSent'] ?? false,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      // --- ИЗМЕНЕНИЕ: Принудительно добавляем смещение часового пояса ---
      'startTime': startTime.toUtc().toIso8601String(),
      'durationInMinutes': durationInMinutes,
      'clientName': clientName,
      'contactId': contactId,
      'service': service,
      'resourceId': resourceId,
      'staffMemberId': staffMemberId,
      'status': status.name.toUpperCase(),
      'tenantId': tenantId,
      'reminderSent': reminderSent,
    };
  }

  Appointment copyWith({
    DateTime? startTime,
    AppointmentStatus? status,
    bool? reminderSent,
  }) {
    return Appointment(
      id: id,
      startTime: startTime ?? this.startTime,
      durationInMinutes: durationInMinutes,
      clientName: clientName,
      contactId: contactId,
      service: service,
      resourceId: resourceId,
      staffMemberId: staffMemberId,
      status: status ?? this.status,
      tenantId: tenantId,
      createdAt: createdAt,
      reminderSent: reminderSent ?? this.reminderSent,
    );
  }
}
