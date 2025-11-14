
import 'package:flutter/material.dart';
import 'package:try_neuro/features/schedule/domain/appointment_model.dart';

class ScheduleService {

  final List<Appointment> _appointments = [
    Appointment(id: '1', date: DateTime.now(), time: const TimeOfDay(hour: 10, minute: 0), durationInMinutes: 60, clientName: 'Иван Петров', service: 'Стрижка мужская', staffMemberId: '1'),
    Appointment(id: '2', date: DateTime.now(), time: const TimeOfDay(hour: 12, minute: 30), durationInMinutes: 90, clientName: 'Анна Сидорова', service: 'Маникюр', resourceId: '2', staffMemberId: '3'),
  ];

  Future<List<Appointment>> getAppointmentsForMonth(DateTime month) async {
    await Future.delayed(const Duration(milliseconds: 400));
    return _appointments.where((appointment) => appointment.date.year == month.year && appointment.date.month == month.month).toList();
  }

  Future<List<Appointment>> getAppointmentsForDay(DateTime day) async {
    await Future.delayed(const Duration(milliseconds: 300));
    final dayAppointments = _appointments.where((appointment) => appointment.date.year == day.year && appointment.date.month == day.month && appointment.date.day == day.day).toList();
    dayAppointments.sort((a, b) {
      final aDouble = a.time.hour + a.time.minute / 60.0;
      final bDouble = b.time.hour + b.time.minute / 60.0;
      return aDouble.compareTo(bDouble);
    });
    return dayAppointments;
  }

  bool _doIntervalsOverlap(TimeOfDay startA, int durationA, TimeOfDay startB, int durationB) {
    final endA = startA.hour * 60 + startA.minute + durationA;
    final startA_minutes = startA.hour * 60 + startA.minute;
    final endB = startB.hour * 60 + startB.minute + durationB;
    final startB_minutes = startB.hour * 60 + startB.minute;
    return startA_minutes < endB && startB_minutes < endA;
  }

  Future<bool> isResourceAvailable({required String resourceId, required DateTime date, required TimeOfDay time, required int duration, String? currentAppointmentId}) async {
    await Future.delayed(const Duration(milliseconds: 200));
    final otherAppointments = _appointments.where((a) => a.id != currentAppointmentId && a.resourceId == resourceId && a.date.year == date.year && a.date.month == date.month && a.date.day == date.day);
    for (final appointment in otherAppointments) {
      // ИСПРАВЛЕНИЕ: Используем реальную длительность существующей записи
      if (_doIntervalsOverlap(time, duration, appointment.time, appointment.durationInMinutes)) {
        return false;
      }
    }
    return true;
  }

  Future<bool> isStaffMemberAvailable({required String staffMemberId, required DateTime date, required TimeOfDay time, required int duration, String? currentAppointmentId}) async {
    await Future.delayed(const Duration(milliseconds: 200));
    final otherAppointments = _appointments.where((a) => a.id != currentAppointmentId && a.staffMemberId == staffMemberId && a.date.year == date.year && a.date.month == date.month && a.date.day == date.day);
    for (final appointment in otherAppointments) {
      // ИСПРАВЛЕНИЕ: Используем реальную длительность существующей записи
      if (_doIntervalsOverlap(time, duration, appointment.time, appointment.durationInMinutes)) {
        return false; 
      }
    }
    return true; 
  }
  
  Future<void> addAppointment({required DateTime date, required TimeOfDay time, required int durationInMinutes, required String clientName, required String service, String? resourceId, String? staffMemberId}) async {
    final newId = DateTime.now().millisecondsSinceEpoch.toString();
    _appointments.add(Appointment(id: newId, date: date, time: time, durationInMinutes: durationInMinutes, clientName: clientName, service: service, resourceId: resourceId, staffMemberId: staffMemberId));
  }

  Future<void> updateAppointment(Appointment appointment) async {
    final index = _appointments.indexWhere((a) => a.id == appointment.id);
    if (index != -1) {
      _appointments[index] = appointment;
    }
  }

  Future<void> deleteAppointment(String appointmentId) async {
    _appointments.removeWhere((a) => a.id == appointmentId);
  }
}
