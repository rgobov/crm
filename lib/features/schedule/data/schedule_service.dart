import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:try_neuro/core/network/http_client.dart';
import 'package:try_neuro/features/schedule/domain/appointment_model.dart';
import 'package:try_neuro/features/schedule/domain/workload_model.dart';
import 'package:try_neuro/service_locator.dart';

class ScheduleService {
  final Dio _dio = sl<HttpClient>().dio;

  Future<List<Workload>> getWorkloadForMonth(int year, int month) async {
    final response = await _dio.get('/appointments/workload', queryParameters: {
      'year': year,
      'month': month,
    });
    final List<dynamic> data = response.data;
    return data.map((json) => Workload.fromJson(json)).toList();
  }

  Future<List<Appointment>> getAppointmentsForDay(DateTime date) async {
    final dateStr = DateFormat('yyyy-MM-dd').format(date);
    final response = await _dio.get('/appointments/day', queryParameters: {'date': dateStr});
    
    final List<dynamic> data = response.data;
    return data.map((json) => Appointment.fromJson(json)).toList();
  }

  Future<List<Appointment>> getAppointmentsForStaff(String staffId, DateTime day) async {
    final dateStr = DateFormat('yyyy-MM-dd').format(day);
    final response = await _dio.get('/appointments/staff/$staffId', queryParameters: {'date': dateStr});
    
    final List<dynamic> data = response.data;
    return data.map((json) => Appointment.fromJson(json)).toList();
  }

  Future<bool> isResourceAvailable({required String resourceId, required DateTime date, required TimeOfDay time, required int duration, String? currentAppointmentId}) async {
    final response = await _dio.get('/resources/$resourceId/availability', queryParameters: {
      'date': DateFormat('yyyy-MM-dd').format(date),
      'time': '${time.hour.toString().padLeft(2, '0')}:${time.minute.toString().padLeft(2, '0')}',
      'duration': duration,
      'currentAppointmentId': currentAppointmentId,
    });
    return response.data as bool;
  }

  Future<bool> isStaffMemberAvailable({required String staffMemberId, required DateTime date, required TimeOfDay time, required int duration, String? currentAppointmentId}) async {
    final response = await _dio.get('/staff/$staffMemberId/availability', queryParameters: {
      'date': DateFormat('yyyy-MM-dd').format(date),
      'time': '${time.hour.toString().padLeft(2, '0')}:${time.minute.toString().padLeft(2, '0')}',
      'duration': duration,
      'currentAppointmentId': currentAppointmentId,
    });
    return response.data as bool;
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
    // Создаем объект Appointment и используем toJson
    final newAppointment = Appointment(
      id: 'new', // id будет присвоен на бэкенде
      date: date,
      time: time,
      durationInMinutes: durationInMinutes,
      clientName: clientName,
      service: service,
      resourceId: resourceId,
      staffMemberId: staffMemberId,
      status: status,
      comment: comment,
    );
    await _dio.post('/appointments', data: newAppointment.toJson());
  }

  Future<void> updateAppointment(Appointment appointment) async {
    await _dio.put('/appointments/${appointment.id}', data: appointment.toJson());
  }

  Future<void> deleteAppointment(String appointmentId) async {
    await _dio.delete('/appointments/$appointmentId');
  }
}
