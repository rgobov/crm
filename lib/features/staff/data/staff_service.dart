import 'package:dio/dio.dart';
import 'package:try_neuro/core/network/http_client.dart';
import 'package:try_neuro/features/staff/domain/staff_member_model.dart';
import 'package:try_neuro/service_locator.dart';

class StaffService {
  final Dio _dio = sl<HttpClient>().dio;

  Future<List<StaffMember>> getStaff() async {
    final response = await _dio.get('/admin/staff');
    final List<dynamic> data = response.data;
    return data.map((json) => StaffMember.fromJson(json)).toList();
  }

  Future<void> addStaffMember({
    required String name,
    required String specialty,
    String? phone, // Добавлено поле
    String? email,
    String? password,
    required String role,
    required bool available,
    String? workStartTime,
    String? workEndTime,
    String? breakStartTime,
    String? breakEndTime,
  }) async {
    await _dio.post('/admin/staff', data: {
      'name': name,
      'specialty': specialty,
      'phone': phone, // Отправляем на сервер
      'email': email,
      'password': password,
      'role': role,
      'available': available,
      'workStartTime': workStartTime,
      'workEndTime': workEndTime,
      'breakStartTime': breakStartTime,
      'breakEndTime': breakEndTime,
    });
  }

  Future<void> updateStaffMember({
    required String id,
    required String name,
    required String specialty,
    String? phone, // Добавлено поле
    required String role,
    required bool available,
    String? workStartTime,
    String? workEndTime,
    String? breakStartTime,
    String? breakEndTime,
    String? email,
    String? password,
  }) async {
    await _dio.put('/admin/staff/$id', data: {
      'name': name,
      'specialty': specialty,
      'phone': phone, // Отправляем на сервер
      'role': role,
      'available': available,
      'workStartTime': workStartTime,
      'workEndTime': workEndTime,
      'breakStartTime': breakStartTime,
      'breakEndTime': breakEndTime,
      'email': email,
      'password': password,
    });
  }

  Future<void> deleteStaffMember(String staffMemberId) async {
    await _dio.delete('/admin/staff/$staffMemberId');
  }
}
