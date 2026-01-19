import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart'; // Добавлен пропущенный импорт
import 'package:try_neuro/core/network/http_client.dart';
import 'package:try_neuro/core/session/session_service.dart';
import 'package:try_neuro/features/auth/domain/user_model.dart';
import 'package:try_neuro/service_locator.dart';

// Условный импорт заглушки
import 'package:try_neuro/core/utils/js_stub.dart' if (dart.library.js) 'dart:js' as js;

class AuthService {
  final Dio _dio = sl<HttpClient>().dio;
  final SessionService _sessionService = sl<SessionService>();

  String? get _tgInitDataRaw {
    if (!kIsWeb) return null;
    try {
      final dynamic context = js.context;
      if (context.hasProperty('Telegram')) {
        final dynamic webApp = context['Telegram']['WebApp'];
        final String? initData = webApp['initData'];
        if (initData != null && initData.isNotEmpty) {
          return initData;
        }
      }
    } catch (e) {
      debugPrint('Error getting initData via JS: $e');
    }
    return null;
  }

  Future<User?> login(String email, String password) async {
    final String finalEmail = email.isEmpty ? 'forts1@e1.ru' : email;
    final String finalPassword = password.isEmpty ? 'qwerty' : password;
    
    try {
      final String? initData = _tgInitDataRaw;

      final response = await _dio.post(
        '/auth/login',
        data: {
          'email': finalEmail,
          'password': finalPassword,
        },
        options: Options(
          headers: initData != null ? {'X-Telegram-Init-Data': initData} : null,
        ),
      );

      if (response.statusCode == 200) {
        final token = response.data['token'];
        final tenantId = response.data['tenantId'];
        
        await _sessionService.saveToken(token);
        await saveTenantId(tenantId);
        
        final user = await getCurrentUser();
        if (user != null) {
          await _sessionService.saveUser(user);
        }
        return user;
      }
    } catch (e) {
      debugPrint('Login error: $e');
    }
    return null;
  }

  Future<User?> getCurrentUser() async {
    try {
      final response = await _dio.get('/auth/me');
      if (response.statusCode == 200) {
        return User.fromJson(response.data);
      }
    } catch (e) {
      debugPrint('Get current user error: $e');
    }
    return null;
  }

  Future<bool> registerCompany({
    required String companyName,
    required String adminEmail,
    required String adminPassword,
    String? address,
  }) async {
    try {
      final response = await _dio.post(
        '/companies/register',
        data: {
          'companyName': companyName,
          'adminEmail': adminEmail,
          'adminPassword': adminPassword,
          'address': address,
        },
      );
      return response.statusCode == 200 || response.statusCode == 201;
    } catch (e) {
      return false;
    }
  }

  Future<void> saveToken(String token) async {
    await _sessionService.saveToken(token);
  }

  Future<void> saveTenantId(String tenantId) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('tenant_id', tenantId);
  }

  Future<void> logout() async {
    await _sessionService.clearSession();
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('tenant_id');
  }
}
