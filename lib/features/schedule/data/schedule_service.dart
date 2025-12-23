import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:try_neuro/core/network/http_client.dart';
import 'package:try_neuro/features/schedule/domain/appointment_model.dart';
import 'package:try_neuro/service_locator.dart';

class ScheduleService {
  final Dio _dio = sl<HttpClient>().dio;

  Future<List<Appointment>> getAppointmentsForMonth(DateTime month) async {
    // В реальном API лучше иметь метод range, но пока будем фильтровать на клиенте или запрашивать все
    // Для простоты запрашиваем все, но это ПЛОХО для продакшена.
    // В будущем нужно добавить эндпоинт /appointments?start=...&end=...
    final response = await _dio.get('/appointments');
    final List<dynamic> data = response.data;
    final allAppointments = data.map((json) => _fromJson(json)).toList();
    
    return allAppointments.where((appointment) => appointment.date.year == month.year && appointment.date.month == month.month).toList();
  }

  Future<List<Appointment>> getAppointmentsForDay(DateTime day) async {
    final dateStr = DateFormat('yyyy-MM-dd').format(day);
    final response = await _dio.get('/appointments/day', queryParameters: {'date': dateStr});
    
    final List<dynamic> data = response.data;
    return data.map((json) => _fromJson(json)).toList();
  }

  Future<List<Appointment>> getAppointmentsForStaff(String staffId, DateTime day) async {
    final dateStr = DateFormat('yyyy-MM-dd').format(day);
    final response = await _dio.get('/appointments/staff/$staffId', queryParameters: {'date': dateStr});
    
    final List<dynamic> data = response.data;
    return data.map((json) => _fromJson(json)).toList();
  }

  // Эти методы проверки доступности пока оставим локальными для скорости UI, 
  // но в идеале их тоже нужно делать через backend.
  // Пока просто сделаем запрос всех записей на день и проверим пересечения локально.
  Future<bool> isResourceAvailable({required String resourceId, required DateTime date, required TimeOfDay time, required int duration, String? currentAppointmentId}) async {
    // Запрашиваем все записи на этот день
    final dayAppointments = await getAppointmentsForDay(date);
    
    final otherAppointments = dayAppointments.where((a) => a.id != currentAppointmentId && a.resourceId == resourceId);
    for (final appointment in otherAppointments) {
      if (_doIntervalsOverlap(time, duration, appointment.time, appointment.durationInMinutes)) {
        return false;
      }
    }
    return true;
  }

  Future<bool> isStaffMemberAvailable({required String staffMemberId, required DateTime date, required TimeOfDay time, required int duration, String? currentAppointmentId}) async {
     final dayAppointments = await getAppointmentsForDay(date);
     
    final otherAppointments = dayAppointments.where((a) => a.id != currentAppointmentId && a.staffMemberId == staffMemberId);
    for (final appointment in otherAppointments) {
      if (_doIntervalsOverlap(time, duration, appointment.time, appointment.durationInMinutes)) {
        return false; 
      }
    }
    return true; 
  }
  
  bool _doIntervalsOverlap(TimeOfDay startA, int durationA, TimeOfDay startB, int durationB) {
    final endA = startA.hour * 60 + startA.minute + durationA;
    final startA_minutes = startA.hour * 60 + startA.minute;
    final endB = startB.hour * 60 + startB.minute + durationB;
    final startB_minutes = startB.hour * 60 + startB.minute;
    return startA_minutes < endB && startB_minutes < endA;
  }
  
  Future<void> addAppointment({
    required DateTime date, 
    required TimeOfDay time, 
    required int durationInMinutes, 
    required String clientName, 
    required String service, 
    String? resourceId, 
    String? staffMemberId,
    AppointmentStatus status = AppointmentStatus.scheduled,
    String? comment,
  }) async {
    final appointmentData = {
      'date': DateFormat('yyyy-MM-dd').format(date),
      'time': '${time.hour.toString().padLeft(2, '0')}:${time.minute.toString().padLeft(2, '0')}:00',
      'durationInMinutes': durationInMinutes,
      'clientName': clientName,
      'service': service,
      'resourceId': resourceId,
      'staffMemberId': staffMemberId,
      'status': status.name.toUpperCase(), // Передаем как строку
      'comment': comment,
    };
    
    await _dio.post('/appointments', data: appointmentData);
  }

  Future<void> updateAppointment(Appointment appointment) async {
    final appointmentData = {
      'date': DateFormat('yyyy-MM-dd').format(appointment.date),
      'time': '${appointment.time.hour.toString().padLeft(2, '0')}:${appointment.time.minute.toString().padLeft(2, '0')}:00',
      'durationInMinutes': appointment.durationInMinutes,
      'clientName': appointment.clientName,
      'service': appointment.service,
      'resourceId': appointment.resourceId,
      'staffMemberId': appointment.staffMemberId,
      'status': appointment.status.name.toUpperCase(),
      'comment': appointment.comment,
    };

    await _dio.put('/appointments/${appointment.id}', data: appointmentData);
  }

  Future<void> deleteAppointment(String appointmentId) async {
    await _dio.delete('/appointments/$appointmentId');
  }
  
  Appointment _fromJson(Map<String, dynamic> json) {
    final timeParts = (json['time'] as String).split(':');
    final time = TimeOfDay(hour: int.parse(timeParts[0]), minute: int.parse(timeParts[1]));
    
    return Appointment(
      id: json['id'],
      date: DateTime.parse(json['date']),
      time: time,
      durationInMinutes: json['durationInMinutes'],
      clientName: json['clientName'],
      service: json['service'],
      resourceId: json['resourceId'],
      staffMemberId: json['staffMemberId'],
      status: AppointmentStatus.values.firstWhere((e) => e.name.toUpperCase() == json['status'], orElse: () => AppointmentStatus.scheduled),
      comment: json['comment'],
    );
  }
}
