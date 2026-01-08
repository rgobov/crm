import 'package:flutter/material.dart';

// Вспомогательная функция для безопасного парсинга времени
TimeOfDay? _parseTime(String? timeString) {
  if (timeString == null) return null;
  try {
    final parts = timeString.split(':');
    final hour = int.parse(parts[0]);
    final minute = int.parse(parts[1]);
    return TimeOfDay(hour: hour, minute: minute);
  } catch (e) {
    return null;
  }
}

class StaffMember {
  final String id;
  final String name;
  final String specialty;
  final String tenantId;
  final TimeOfDay? workStartTime;
  final TimeOfDay? workEndTime;
  final TimeOfDay? breakStartTime;
  final TimeOfDay? breakEndTime;
  final bool available;
  final String? role;
  final String? email;

  StaffMember({
    required this.id,
    required this.name,
    required this.specialty,
    required this.tenantId,
    this.workStartTime,
    this.workEndTime,
    this.breakStartTime,
    this.breakEndTime,
    required this.available,
    this.role,
    this.email,
  });

  factory StaffMember.fromJson(Map<String, dynamic> json) {
    return StaffMember(
      id: json['id'],
      name: json['name'],
      specialty: json['specialty'],
      tenantId: json['tenantId'],
      available: json['available'] ?? false,
      role: json['role'],
      email: json['email'],
      // --- ИЗМЕНЕНИЕ ЗДЕСЬ: Парсим строки во время ---
      workStartTime: _parseTime(json['workStartTime']),
      workEndTime: _parseTime(json['workEndTime']),
      breakStartTime: _parseTime(json['breakStartTime']),
      breakEndTime: _parseTime(json['breakEndTime']),
    );
  }
}
