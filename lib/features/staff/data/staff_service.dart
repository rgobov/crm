import 'package:dio/dio.dart';
import 'package:try_neuro/core/network/http_client.dart';
import 'package:try_neuro/features/staff/domain/staff_member_model.dart';
import 'package:try_neuro/service_locator.dart';

class StaffService {
  final Dio _dio = sl<HttpClient>().dio;

  Future<List<StaffMember>> getStaff() async {
    final response = await _dio.get('/staff');
    final List<dynamic> data = response.data;
    return data.map((json) => _fromJson(json)).toList();
  }

  Future<void> addStaffMember({
    required String name,
    String? specialty,
    String? email,
    String? password,
    String? role,
  }) async {
    await _dio.post('/staff', data: {
      'name': name,
      'specialty': specialty,
      'email': email,
      'password': password,
      'role': role,
    });
  }

  Future<void> updateStaffMember(StaffMember staffMember, {String? role}) async {
    // Используем PUT для обновления
    await _dio.put('/staff/${staffMember.id}', data: {
      'name': staffMember.name,
      'specialty': staffMember.specialty,
      'role': role, // Передаем роль при обновлении
    });
  }

  Future<void> deleteStaffMember(String staffMemberId) async {
    await _dio.delete('/staff/$staffMemberId');
  }

  StaffMember _fromJson(Map<String, dynamic> json) {
    return StaffMember(
      id: json['id'],
      name: json['name'],
      specialty: json['specialty'],
      role: json['role'], // Читаем роль из JSON
    );
  }
}
