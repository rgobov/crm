import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

enum AppointmentStatus { scheduled, completed, cancelled }

class Appointment {
  final String id;
  final DateTime date;
  final TimeOfDay time;
  final int durationInMinutes;
  final String clientName;
  final String service;
  final String? resourceId;
  final String? staffMemberId;
  final AppointmentStatus status;
  final String? comment;
  String? tenantId;

  Appointment({
    required this.id,
    required this.date,
    required this.time,
    required this.durationInMinutes,
    required this.clientName,
    required this.service,
    this.resourceId,
    this.staffMemberId,
    this.status = AppointmentStatus.scheduled,
    this.comment,
    this.tenantId,
  });

  // --- Фабричный конструктор для создания из JSON ---
  factory Appointment.fromJson(Map<String, dynamic> json) {
    final timeParts = (json['time'] as String).split(':');
    return Appointment(
      id: json['id'],
      date: DateTime.parse(json['date']),
      time: TimeOfDay(hour: int.parse(timeParts[0]), minute: int.parse(timeParts[1])),
      durationInMinutes: json['durationInMinutes'],
      clientName: json['clientName'],
      service: json['service'],
      resourceId: json['resourceId'],
      staffMemberId: json['staffMemberId'],
      status: AppointmentStatus.values.firstWhere((e) => e.name.toUpperCase() == json['status'].toUpperCase(), orElse: () => AppointmentStatus.scheduled),
      comment: json['comment'],
      tenantId: json['tenantId'],
    );
  }

  // --- Метод для преобразования в JSON ---
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'date': DateFormat('yyyy-MM-dd').format(date),
      'time': '${time.hour.toString().padLeft(2, '0')}:${time.minute.toString().padLeft(2, '0')}',
      'durationInMinutes': durationInMinutes,
      'clientName': clientName,
      'service': service,
      'resourceId': resourceId,
      'staffMemberId': staffMemberId,
      'status': status.name.toUpperCase(),
      'comment': comment,
      'tenantId': tenantId,
    };
  }
}
