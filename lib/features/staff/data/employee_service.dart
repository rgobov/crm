import 'package:dio/dio.dart';
import 'package:flutter/material.dart'; // Добавлен пропущенный импорт
import 'package:intl/intl.dart';
import 'package:try_neuro/core/network/http_client.dart';
import 'package:try_neuro/features/schedule/domain/appointment_comment_model.dart';
import 'package:try_neuro/features/schedule/domain/appointment_model.dart';
import 'package:try_neuro/features/schedule/domain/workload_model.dart';
import 'package:try_neuro/features/staff/domain/staff_member_model.dart';
import 'package:try_neuro/service_locator.dart';

class EmployeeService {
  final Dio _dio = sl<HttpClient>().dio;

  Future<StaffMember> getMyProfile({DateTime? date}) async {
    final queryParams = date != null ? {'date': DateFormat('yyyy-MM-dd').format(date)} : null;
    final response = await _dio.get('/employee/profile', queryParameters: queryParams);
    return StaffMember.fromJson(response.data);
  }

  Future<void> updateMyShift({
    required DateTime date,
    required bool isDayOff,
    TimeOfDay? workStart,
    TimeOfDay? workEnd,
    TimeOfDay? breakStart,
    TimeOfDay? breakEnd,
  }) async {
    String? format(TimeOfDay? t) => t == null ? null : '${t.hour.toString().padLeft(2, '0')}:${t.minute.toString().padLeft(2, '0')}:00';

    await _dio.put(
      '/employee/profile/shift',
      data: {
        'date': DateFormat('yyyy-MM-dd').format(date),
        'isDayOff': isDayOff,
        'workStartTime': format(workStart),
        'workEndTime': format(workEnd),
        'breakStartTime': format(breakStart),
        'breakEndTime': format(breakEnd),
      },
    );
  }

  Future<void> repeatSchedule({
    required DateTime sourceDate,
    required bool isDayOff,
    TimeOfDay? workStart,
    TimeOfDay? workEnd,
    TimeOfDay? breakStart,
    TimeOfDay? breakEnd,
    required int days,
  }) async {
    String? format(TimeOfDay? t) => t == null ? null : '${t.hour.toString().padLeft(2, '0')}:${t.minute.toString().padLeft(2, '0')}:00';

    await _dio.post(
      '/employee/profile/shift/copy',
      queryParameters: {'days': days},
      data: {
        'date': DateFormat('yyyy-MM-dd').format(sourceDate),
        'isDayOff': isDayOff,
        'workStartTime': format(workStart),
        'workEndTime': format(workEnd),
        'breakStartTime': format(breakStart),
        'breakEndTime': format(breakEnd),
      },
    );
  }

  Future<List<Appointment>> getMyAppointmentsForDay(DateTime date) async {
    final dateStr = DateFormat('yyyy-MM-dd').format(date);
    final response = await _dio.get('/employee/appointments', queryParameters: {'date': dateStr});
    final List<dynamic> data = response.data;
    return data.map((json) => Appointment.fromJson(json)).toList();
  }

  // --- ВОССТАНОВЛЕНО: Метод для получения истории записей контакта ---
  Future<List<Appointment>> getContactAppointments(String contactId) async {
    final response = await _dio.get('/employee/contacts/$contactId/appointments');
    final List<dynamic> data = response.data;
    return data.map((json) => Appointment.fromJson(json)).toList();
  }

  Future<Appointment> updateAppointment(Appointment appointment) async {
    final response = await _dio.put(
      '/employee/appointments/${appointment.id}',
      data: appointment.toJson(),
    );
    return Appointment.fromJson(response.data);
  }

  Future<void> deleteAppointment(String id) async {
    await _dio.delete('/employee/appointments/$id');
  }

  // --- ВОССТАНОВЛЕНО: Метод для добавления записи ---
  Future<void> addAppointment(Appointment appointment) async {
    await _dio.post('/employee/appointments', data: appointment.toJson());
  }

  Future<List<Workload>> getMyWorkloadForMonth(int year, int month) async {
    final response = await _dio.get('/employee/workload', queryParameters: {
      'year': year,
      'month': month,
    });
    final List<dynamic> data = response.data;
    return data.map((json) => Workload.fromJson(json)).toList();
  }

  Future<List<AppointmentComment>> getComments(String appointmentId) async {
    final response = await _dio.get('/comments/appointment/$appointmentId');
    final List<dynamic> data = response.data;
    return data.map((json) => AppointmentComment.fromJson(json)).toList();
  }

  Future<AppointmentComment> addComment(String appointmentId, String text) async {
    final response = await _dio.post(
      '/comments/appointment/$appointmentId',
      data: {'text': text},
    );
    return AppointmentComment.fromJson(response.data);
  }
}
