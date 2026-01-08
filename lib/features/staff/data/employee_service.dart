import 'package:dio/dio.dart';
import 'package:intl/intl.dart';
import 'package:try_neuro/core/network/http_client.dart';
import 'package:try_neuro/features/schedule/domain/appointment_model.dart';
import 'package:try_neuro/features/schedule/domain/workload_model.dart';
import 'package:try_neuro/features/staff/domain/staff_member_model.dart';
import 'package:try_neuro/service_locator.dart';

class EmployeeService {
  final Dio _dio = sl<HttpClient>().dio;

  // --- НОВЫЙ МЕТОД ---
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

  Future<List<Workload>> getMyWorkloadForMonth(int year, int month) async {
    final response = await _dio.get('/employee/workload', queryParameters: {
      'year': year,
      'month': month,
    });
    final List<dynamic> data = response.data;
    return data.map((json) => Workload.fromJson(json)).toList();
  }
}
