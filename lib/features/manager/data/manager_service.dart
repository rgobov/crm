import 'package:dio/dio.dart';
import 'package:try_neuro/core/network/http_client.dart';
import 'package:try_neuro/features/schedule/domain/appointment_model.dart';
import 'package:try_neuro/features/staff/domain/staff_member_model.dart';
import 'package:try_neuro/service_locator.dart';

class ManagerService {
  final Dio _dio = sl<HttpClient>().dio;

  Future<List<StaffMember>> getStaffForSchedule() async {
    final response = await _dio.get('/manager/schedule/staff');
    final List<dynamic> data = response.data;
    return data.map((json) => StaffMember.fromJson(json)).toList();
  }

  // Новый метод для создания записи
  Future<void> addAppointment(Appointment appointment) async {
    await _dio.post('/manager/appointments', data: appointment.toJson());
  }
}
