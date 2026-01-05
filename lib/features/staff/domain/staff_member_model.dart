import 'package:flutter/material.dart';

class StaffMember {
  final String id;
  final String name;
  final String specialty;
  final String? role;
  final bool available;
  final TimeOfDay? workStartTime;
  final TimeOfDay? workEndTime;
  final TimeOfDay? breakStartTime;
  final TimeOfDay? breakEndTime;
  String? email; // Делаем поле изменяемым

  StaffMember({
    required this.id,
    required this.name,
    required this.specialty,
    this.role,
    this.available = true,
    this.workStartTime,
    this.workEndTime,
    this.breakStartTime,
    this.breakEndTime,
    this.email,
  });

  factory StaffMember.fromJson(Map<String, dynamic> json) {
    TimeOfDay? parseTime(String? time) {
      if (time == null) return null;
      final parts = time.split(':');
      return TimeOfDay(hour: int.parse(parts[0]), minute: int.parse(parts[1]));
    }

    return StaffMember(
      id: json['id'],
      name: json['name'],
      specialty: json['specialty'],
      role: json['role'],
      available: json['available'] ?? true,
      workStartTime: parseTime(json['workStartTime']),
      workEndTime: parseTime(json['workEndTime']),
      breakStartTime: parseTime(json['breakStartTime']),
      breakEndTime: parseTime(json['breakEndTime']),
      email: json['email'], // Принимаем email с бэкенда
    );
  }
}
