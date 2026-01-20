import 'package:flutter/material.dart';

TimeOfDay? _parseTime(String? timeString) {
  if (timeString == null || timeString.isEmpty) return null;
  try {
    final parts = timeString.split(':');
    final hour = int.parse(parts[0]);
    final minute = int.parse(parts[1]);
    return TimeOfDay(hour: hour, minute: minute);
  } catch (e) {
    return null;
  }
}

String? _formatTime(TimeOfDay? time) {
  if (time == null) return null;
  return '${time.hour.toString().padLeft(2, '0')}:${time.minute.toString().padLeft(2, '0')}:00';
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
  final bool available; // Это глобальный статус (активен/нет)
  final bool isDayOff; // Это статус КОНКРЕТНОЙ смены
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
    this.isDayOff = false,
    this.role,
    this.email,
  });

  factory StaffMember.fromJson(Map<String, dynamic> json) {
    return StaffMember(
      id: json['id'],
      name: json['name'],
      specialty: json['specialty'],
      tenantId: json['tenantId'],
      available: json['available'] ?? json['active'] ?? false,
      isDayOff: json['dayOff'] ?? false,
      role: json['role'],
      email: json['email'],
      workStartTime: _parseTime(json['workStartTime']),
      workEndTime: _parseTime(json['workEndTime']),
      breakStartTime: _parseTime(json['breakStartTime']),
      breakEndTime: _parseTime(json['breakEndTime']),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'specialty': specialty,
      'tenantId': tenantId,
      'active': available,
      'dayOff': isDayOff,
      'workStartTime': _formatTime(workStartTime),
      'workEndTime': _formatTime(workEndTime),
      'breakStartTime': _formatTime(breakStartTime),
      'breakEndTime': _formatTime(breakEndTime),
    };
  }

  StaffMember copyWith({
    TimeOfDay? workStartTime,
    TimeOfDay? workEndTime,
    TimeOfDay? breakStartTime,
    TimeOfDay? breakEndTime,
    bool? available,
    bool? isDayOff,
  }) {
    return StaffMember(
      id: id,
      name: name,
      specialty: specialty,
      tenantId: tenantId,
      workStartTime: workStartTime ?? this.workStartTime,
      workEndTime: workEndTime ?? this.workEndTime,
      breakStartTime: breakStartTime ?? this.breakStartTime,
      breakEndTime: breakEndTime ?? this.breakEndTime,
      available: available ?? this.available,
      isDayOff: isDayOff ?? this.isDayOff,
      role: role,
      email: email,
    );
  }
}
