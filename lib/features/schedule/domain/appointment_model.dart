import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

TimeOfDay _parseTime(String timeString) {
  final parts = timeString.split(':');
  return TimeOfDay(hour: int.parse(parts[0]), minute: int.parse(parts[1]));
}

DateTime _parseDate(String dateString) {
  return DateTime.parse(dateString);
}

enum AppointmentStatus { scheduled, confirmed, needs_call, completed, cancelled }

class Appointment {
  final String id;
  final DateTime date;
  final TimeOfDay time;
  final int durationInMinutes;
  final String clientName;
  final String? contactId;
  final String service;
  final String? resourceId;
  final String? staffMemberId;
  final AppointmentStatus status;
  final String? tenantId;
  final DateTime? createdAt;
  // --- НОВОЕ ПОЛЕ ---
  final bool reminderSent;

  Appointment({
    required this.id,
    required this.date,
    required this.time,
    required this.durationInMinutes,
    required this.clientName,
    this.contactId,
    required this.service,
    this.resourceId,
    this.staffMemberId,
    required this.status,
    this.tenantId,
    this.createdAt,
    this.reminderSent = false, // По умолчанию false
  });

  factory Appointment.fromJson(Map<String, dynamic> json) {
    return Appointment(
      id: json['id'],
      date: _parseDate(json['date'] as String),
      time: _parseTime(json['time'] as String),
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
      createdAt: json['createdAt'] != null ? DateTime.parse(json['createdAt'] as String) : null,
      // Читаем из JSON
      reminderSent: json['reminderSent'] ?? false,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'date': DateFormat('yyyy-MM-dd').format(date),
      'time': '${time.hour.toString().padLeft(2, '0')}:${time.minute.toString().padLeft(2, '0')}:00',
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
    AppointmentStatus? status,
    bool? reminderSent,
  }) {
    return Appointment(
      id: id,
      date: date,
      time: time,
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
