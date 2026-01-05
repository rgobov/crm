import 'package:dio/dio.dart';
import 'package:jwt_decoder/jwt_decoder.dart';
import 'package:try_neuro/core/network/http_client.dart';
import 'package:try_neuro/core/session/session_service.dart';
import 'package:try_neuro/features/auth/domain/user_model.dart';
import 'package:try_neuro/service_locator.dart';
import 'package:flutter/foundation.dart';

class AuthService {
  final Dio _dio = sl<HttpClient>().dio;
  final SessionService _sessionService = sl<SessionService>();

  Future<User?> login(String email, String password) async {
    try {
      final response = await _dio.post(
        '/auth/login',
        data: {'email': email, 'password': password},
      );

      if (response.statusCode == 200) {
        final token = response.data['token'];
        if (token == null) {
          return null;
        }
        await _sessionService.saveToken(token);
        
        Map<String, dynamic> decodedToken = JwtDecoder.decode(token);

        final userRoleString = (decodedToken['role'] as String? ?? '').toUpperCase();
        UserRole userRole;
        if (userRoleString.contains('ADMIN')) {
          userRole = UserRole.admin;
        } else if (userRoleString.contains('MANAGER')) {
          userRole = UserRole.manager;
        } else {
          userRole = UserRole.employee;
        }

        final user = User(
          id: decodedToken['sub'],
          email: decodedToken['sub'],
          role: userRole,
          tenantId: decodedToken['tenantId'],
          staffId: decodedToken['staffId'],
        );

        // --- КЛЮЧЕВОЕ ИСПРАВЛЕНИЕ: Сохраняем пользователя в сессию ---
        await _sessionService.saveUser(user);

        return user;
      }
    } on DioException catch (e) {
      if (kDebugMode) print('AuthService login error: $e');
      return null; // Просто возвращаем null при любой ошибке входа
    } catch (e) {
      if (kDebugMode) print('AuthService generic error: $e');
      return null;
    }
    return null;
  }

  Future<bool> registerCompany({
    required String companyName,
    required String address,
    required String adminEmail,
    required String adminPassword,
  }) async {
    try {
      final response = await _dio.post(
        '/companies/register',
        data: {
          'companyName': companyName,
          'companyAddress': address,
          'adminEmail': adminEmail,
          'adminPassword': adminPassword,
        },
      );
      return response.statusCode == 200;
    } catch (e) {
      return false;
    }
  }

  Future<void> logout() async {
    await _sessionService.clearSession();
  }
}
