
import 'package:flutter/material.dart';

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
  });
}
