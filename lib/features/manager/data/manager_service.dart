import 'package:dio/dio.dart';
import 'package:intl/intl.dart';
import 'package:try_neuro/core/network/http_client.dart';
import 'package:try_neuro/features/manager/domain/wappi_settings_model.dart';
import 'package:try_neuro/features/schedule/domain/appointment_comment_model.dart';
import 'package:try_neuro/features/schedule/domain/appointment_model.dart';
import 'package:try_neuro/features/schedule/domain/workload_model.dart';
import 'package:try_neuro/features/staff/domain/staff_member_model.dart';
import 'package:try_neuro/service_locator.dart';

class ManagerService {
  final Dio _dio = sl<HttpClient>().dio;

  // Обновлено: теперь принимает дату для получения актуальных смен мастеров
  Future<List<StaffMember>> getStaffForSchedule(DateTime date) async {
    final dateStr = DateFormat('yyyy-MM-dd').format(date);
    final response = await _dio.get('/manager/schedule/staff', queryParameters: {'date': dateStr});
    final List<dynamic> data = response.data;
    return data.map((json) => StaffMember.fromJson(json)).toList();
  }

  Future<List<Appointment>> getAppointmentsForDay(DateTime date) async {
    final dateStr = DateFormat('yyyy-MM-dd').format(date);
    final response = await _dio.get('/manager/appointments/day', queryParameters: {'date': dateStr});
    final List<dynamic> data = response.data;
    return data.map((json) => Appointment.fromJson(json)).toList();
  }

  Future<List<Appointment>> getContactAppointments(String contactId) async {
    final response = await _dio.get('/manager/contacts/$contactId/appointments');
    final List<dynamic> data = response.data;
    return data.map((json) => Appointment.fromJson(json)).toList();
  }

  Future<List<Workload>> getWorkloadForMonth(int year, int month) async {
    final response = await _dio.get('/manager/workload', queryParameters: {
      'year': year,
      'month': month,
    });
    final List<dynamic> data = response.data;
    return data.map((json) => Workload.fromJson(json)).toList();
  }

  Future<void> addAppointment(Appointment appointment) async {
    await _dio.post('/manager/appointments', data: appointment.toJson());
  }

  Future<Appointment> updateAppointment(Appointment appointment) async {
    final response = await _dio.put(
      '/manager/appointments/${appointment.id}',
      data: appointment.toJson(),
    );
    return Appointment.fromJson(response.data);
  }

  Future<void> deleteAppointment(String id) async {
    await _dio.delete('/manager/appointments/$id');
  }

  Future<WappiSettings> getWappiSettings() async {
    final response = await _dio.get('/manager/settings/wappi');
    return WappiSettings.fromJson(response.data);
  }

  Future<WappiSettings> updateWappiSettings(WappiSettings settings) async {
    final response = await _dio.put('/manager/settings/wappi', data: settings.toJson());
    return WappiSettings.fromJson(response.data);
  }

  Future<void> sendTestWappiMessage(String phone) async {
    await _dio.post('/manager/settings/wappi/test', queryParameters: {'phone': phone});
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
