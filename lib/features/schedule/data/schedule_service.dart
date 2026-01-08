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
    final response = await _dio.get('/manager/workload', queryParameters: {
      'year': year,
      'month': month,
    });
    final List<dynamic> data = response.data;
    return data.map((json) => Workload.fromJson(json)).toList();
  }

  Future<List<Appointment>> getAppointmentsForDay(DateTime date) async {
    final dateStr = DateFormat('yyyy-MM-dd').format(date);
    final response = await _dio.get('/manager/appointments/day', queryParameters: {'date': dateStr});
    final List<dynamic> data = response.data;
    return data.map((json) => Appointment.fromJson(json)).toList();
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
  }) async {
    final newAppointment = Appointment(
      id: 'new',
      date: date,
      time: time,
      durationInMinutes: durationInMinutes,
      clientName: clientName,
      service: service,
      resourceId: resourceId,
      staffMemberId: staffMemberId,
      status: status,
    );
    await _dio.post('/manager/appointments', data: newAppointment.toJson());
  }

  Future<void> updateAppointment(Appointment appointment) async {
    await _dio.put('/appointments/${appointment.id}', data: appointment.toJson());
  }

  Future<void> deleteAppointment(String appointmentId) async {
    await _dio.delete('/appointments/$appointmentId');
  }
}
