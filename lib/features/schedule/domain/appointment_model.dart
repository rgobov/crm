import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

TimeOfDay _parseTime(String timeString) {
  final parts = timeString.split(':');
  return TimeOfDay(hour: int.parse(parts[0]), minute: int.parse(parts[1]));
}

DateTime _parseDate(String dateString) {
  return DateTime.parse(dateString);
}

enum AppointmentStatus { scheduled, completed, cancelled }

class Appointment {
  final String id;
  final DateTime date;
  final TimeOfDay time;
  final int durationInMinutes;
  final String clientName;
  final String? contactId; // --- НОВОЕ ПОЛЕ ---
  final String service;
  final String? resourceId;
  final String? staffMemberId;
  final AppointmentStatus status;
  final String? tenantId;

  Appointment({
    required this.id,
    required this.date,
    required this.time,
    required this.durationInMinutes,
    required this.clientName,
    this.contactId, // --- НОВОЕ ПОЛЕ ---
    required this.service,
    this.resourceId,
    this.staffMemberId,
    required this.status,
    this.tenantId,
  });

  factory Appointment.fromJson(Map<String, dynamic> json) {
    return Appointment(
      id: json['id'],
      date: _parseDate(json['date'] as String),
      time: _parseTime(json['time'] as String),
      durationInMinutes: json['durationInMinutes'],
      clientName: json['clientName'],
      contactId: json['contactId'], // --- НОВОЕ ПОЛЕ ---
      service: json['service'],
      resourceId: json['resourceId'],
      staffMemberId: json['staffMemberId'],
      status: AppointmentStatus.values.firstWhere(
        (e) => e.name == (json['status'] as String?)?.toLowerCase(),
        orElse: () => AppointmentStatus.scheduled,
      ),
      tenantId: json['tenantId'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'date': DateFormat('yyyy-MM-dd').format(date),
      'time': '${time.hour.toString().padLeft(2, '0')}:${time.minute.toString().padLeft(2, '0')}:00',
      'durationInMinutes': durationInMinutes,
      'clientName': clientName,
      'contactId': contactId, // --- НОВОЕ ПОЛЕ ---
      'service': service,
      'resourceId': resourceId,
      'staffMemberId': staffMemberId,
      'status': status.name.toUpperCase(),
      'tenantId': tenantId,
    };
  }

  Appointment copyWith({AppointmentStatus? status}) {
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
    );
  }
}
