import 'package:dio/dio.dart';
import 'package:jwt_decoder/jwt_decoder.dart';
import 'package:try_neuro/core/network/http_client.dart';
import 'package:try_neuro/core/session/session_service.dart';
import 'package:try_neuro/features/auth/domain/user_model.dart';
import 'package:try_neuro/service_locator.dart';
import 'package:flutter/foundation.dart'; // Для debugPrint

class AuthService {
  final Dio _dio = sl<HttpClient>().dio;
  final SessionService _sessionService = sl<SessionService>();

  Future<User?> login(String email, String password) async {
    try {
      debugPrint('LOGIN: Sending login request for $email'); // ЛОГ НАЧАЛА
      final response = await _dio.post(
        '/auth/login',
        data: {'email': email, 'password': password},
      );
      
      debugPrint('LOGIN: Response status: ${response.statusCode}'); // ЛОГ ОТВЕТА
      debugPrint('LOGIN: Response data: ${response.data}');

      if (response.statusCode == 200) {
        final token = response.data['token'];
        if (token == null) {
          debugPrint('LOGIN ERROR: Token is null in response');
          return null;
        }
        await _sessionService.saveToken(token);
        
        // Декодируем токен, чтобы получить данные пользователя
        Map<String, dynamic> decodedToken = JwtDecoder.decode(token);
        debugPrint('LOGIN: Decoded token: $decodedToken');

        final userRole = (decodedToken['role'] as String).toLowerCase().contains('admin') 
            ? UserRole.admin 
            : (decodedToken['role'] as String).toLowerCase().contains('manager') 
            ? UserRole.manager 
            : UserRole.employee;

        return User(
          id: decodedToken['sub'], // sub = email
          email: decodedToken['sub'],
          role: userRole,
          tenantId: decodedToken['tenantId'],
          staffId: decodedToken['staffId'],
        );
      }
    } on DioException catch (e) {
      debugPrint('LOGIN ERROR: $e'); // ЛОГ ОШИБКИ
      debugPrint('LOGIN ERROR TYPE: ${e.type}');
      debugPrint('LOGIN ERROR RESPONSE: ${e.response}');
      
      // 401 - неверный логин/пароль
      if (e.response?.statusCode == 401) {
        return null;
      }
      // Другие ошибки сети
      rethrow;
    } catch (e) {
      debugPrint('LOGIN UNKNOWN ERROR: $e');
      rethrow;
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
      debugPrint('Sending registration request for $companyName to ${_dio.options.baseUrl}/companies/register');
      
      final response = await _dio.post(
        '/companies/register',
        data: {
          'companyName': companyName,
          'companyAddress': address,
          'adminEmail': adminEmail,
          'adminPassword': adminPassword,
        },
      );
      
      debugPrint('Registration response status: ${response.statusCode}');
      debugPrint('Registration response data: ${response.data}');

      return response.statusCode == 200;
    } catch (e) {
      debugPrint('REGISTRATION ERROR: $e');
      if (e is DioException) {
         debugPrint('Dio Error Type: ${e.type}');
         debugPrint('Dio Error Message: ${e.message}');
         debugPrint('Dio Error Response: ${e.response}');
      }
      // Возвращаем false, чтобы остановить загрузку в UI
      return false;
    }
  }

  Future<void> logout() async {
    await _sessionService.clearSession();
  }
}
