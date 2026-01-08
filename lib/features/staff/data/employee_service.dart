import 'package:dio/dio.dart';
import 'package:intl/intl.dart';
import 'package:try_neuro/core/network/http_client.dart';
import 'package:try_neuro/features/schedule/domain/appointment_comment_model.dart';
import 'package:try_neuro/features/schedule/domain/appointment_model.dart';
import 'package:try_neuro/features/schedule/domain/workload_model.dart';
import 'package:try_neuro/features/staff/domain/staff_member_model.dart';
import 'package:try_neuro/service_locator.dart';

class EmployeeService {
  final Dio _dio = sl<HttpClient>().dio;

  Future<StaffMember> getMyProfile() async {
    final response = await _dio.get('/employee/profile');
    return StaffMember.fromJson(response.data);
  }

  Future<List<Appointment>> getMyAppointmentsForDay(DateTime date) async {
    final dateStr = DateFormat('yyyy-MM-dd').format(date);
    final response = await _dio.get('/employee/appointments', queryParameters: {'date': dateStr});
    final List<dynamic> data = response.data;
    return data.map((json) => Appointment.fromJson(json)).toList();
  }

  // --- НОВЫЙ МЕТОД ---
  Future<Appointment> updateAppointment(Appointment appointment) async {
    final response = await _dio.put(
      '/employee/appointments/${appointment.id}',
      data: appointment.toJson(),
    );
    return Appointment.fromJson(response.data);
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
