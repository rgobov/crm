import 'package:dio/dio.dart';
import 'package:try_neuro/core/network/http_client.dart';
import 'package:try_neuro/features/staff/domain/staff_member_model.dart';
import 'package:try_neuro/service_locator.dart';

class StaffService {
  final Dio _dio = sl<HttpClient>().dio;

  // ОБНОВЛЕНО: Поддержка поиска и пагинации
  Future<Map<String, dynamic>> getStaffPaged({String? query, int page = 0, int size = 100}) async {
    final response = await _dio.get('/admin/staff', queryParameters: {
      'query': query,
      'page': page,
      'size': size,
    });

    final List<dynamic> content = response.data['content'];
    final List<StaffMember> members = content.map((json) => StaffMember.fromJson(json)).toList();

    return {
      'members': members,
      'totalPages': response.data['totalPages'],
      'totalElements': response.data['totalElements'],
    };
  }

  // Оставляем для совместимости (берет первую страницу)
  Future<List<StaffMember>> getStaff() async {
    final result = await getStaffPaged();
    return result['members'] as List<StaffMember>;
  }

  Future<void> addStaffMember({
    required String name,
    required String specialty,
    String? phone,
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
      'phone': phone,
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
    String? phone,
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
      'phone': phone,
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
