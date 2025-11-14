
import 'package:flutter/material.dart';

class Appointment {
  final String id;
  final DateTime date;
  final TimeOfDay time;
  final int durationInMinutes; // <-- ДОБАВЛЕНО
  final String clientName;
  final String service;
  final String? resourceId;
  final String? staffMemberId;

  Appointment({
    required this.id,
    required this.date,
    required this.time,
    required this.durationInMinutes,
    required this.clientName,
    required this.service,
    this.resourceId,
    this.staffMemberId,
  });
}
