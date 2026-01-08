import 'package:dio/dio.dart';
import 'package:intl/intl.dart';
import 'package:try_neuro/core/network/http_client.dart';
import 'package:try_neuro/features/schedule/domain/appointment_comment_model.dart';
import 'package:try_neuro/features/schedule/domain/appointment_model.dart';
import 'package:try_neuro/features/schedule/domain/workload_model.dart';
import 'package:try_neuro/features/staff/domain/staff_member_model.dart';
import 'package:try_neuro/service_locator.dart';

class ManagerService {
  final Dio _dio = sl<HttpClient>().dio;

  Future<List<StaffMember>> getStaffForSchedule() async {
    final response = await _dio.get('/manager/schedule/staff');
    final List<dynamic> data = response.data;
    return data.map((json) => StaffMember.fromJson(json)).toList();
  }

  Future<List<Appointment>> getAppointmentsForDay(DateTime date) async {
    final dateStr = DateFormat('yyyy-MM-dd').format(date);
    final response = await _dio.get('/manager/appointments/day', queryParameters: {'date': dateStr});
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

  // --- ИЗМЕНЕНИЕ ЗДЕСЬ: Используем единый эндпоинт /api/comments ---
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
