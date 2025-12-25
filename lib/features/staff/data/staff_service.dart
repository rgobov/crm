import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:try_neuro/core/network/http_client.dart';
import 'package:try_neuro/features/staff/domain/staff_member_model.dart';
import 'package:try_neuro/service_locator.dart';

class StaffService {
  final Dio _dio = sl<HttpClient>().dio;

  TimeOfDay? _parseTime(String? time) {
    if (time == null) return null;
    final parts = time.split(':');
    return TimeOfDay(hour: int.parse(parts[0]), minute: int.parse(parts[1]));
  }

  String? _formatTime(TimeOfDay? time) {
    if (time == null) return null;
    return '${time.hour.toString().padLeft(2, '0')}:${time.minute.toString().padLeft(2, '0')}';
  }

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
    required bool available,
    TimeOfDay? workStartTime,
    TimeOfDay? workEndTime,
    TimeOfDay? breakStartTime,
    TimeOfDay? breakEndTime,
  }) async {
    await _dio.post('/staff', data: {
      'name': name,
      'specialty': specialty,
      'email': email,
      'password': password,
      'role': role,
      'available': available,
      'workStartTime': _formatTime(workStartTime),
      'workEndTime': _formatTime(workEndTime),
      'breakStartTime': _formatTime(breakStartTime),
      'breakEndTime': _formatTime(breakEndTime),
    });
  }

  Future<void> updateStaffMember(StaffMember staffMember, {String? role}) async {
    await _dio.put('/staff/${staffMember.id}', data: {
      'name': staffMember.name,
      'specialty': staffMember.specialty,
      'role': role,
      'available': staffMember.available,
      'workStartTime': _formatTime(staffMember.workStartTime),
      'workEndTime': _formatTime(staffMember.workEndTime),
      'breakStartTime': _formatTime(staffMember.breakStartTime),
      'breakEndTime': _formatTime(staffMember.breakEndTime),
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
      role: json['role'],
      available: json['available'] ?? true,
      workStartTime: _parseTime(json['workStartTime']),
      workEndTime: _parseTime(json['workEndTime']),
      breakStartTime: _parseTime(json['breakStartTime']),
      breakEndTime: _parseTime(json['breakEndTime']),
    );
  }
}
