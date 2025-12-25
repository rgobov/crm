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
  });
}
