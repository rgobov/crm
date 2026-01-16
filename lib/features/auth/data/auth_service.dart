import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:telegram_web_app/telegram_web_app.dart'; // <<< ИМПОРТ
import 'package:try_neuro/core/config/app_config.dart';
import 'package:try_neuro/core/network/http_client.dart';
import 'package:try_neuro/features/auth/domain/user_model.dart';
import 'package:try_neuro/service_locator.dart';

class AuthService {
  final Dio _dio = sl<HttpClient>().dio;
  final _storage = const FlutterSecureStorage();

  Future<User?> login(String email, String password) async {
    try {
      // Пытаемся получить данные Telegram для авто-привязки
      final tg = TelegramWebApp.instance;
      final String? initData = (tg.isSupported && tg.initData.isNotEmpty) ? tg.initData : null;

      final response = await _dio.post(
        '${AppConfig.productionUrl}/auth/login',
        data: {
          'email': email,
          'password': password,
        },
        // Передаем данные Telegram в заголовке для привязки аккаунта
        options: Options(
          headers: initData != null ? {'X-Telegram-Init-Data': initData} : null,
        ),
      );

      if (response.statusCode == 200) {
        final token = response.data['token'];
        final tenantId = response.data['tenantId'];
        
        await saveToken(token);
        await saveTenantId(tenantId);
        
        return await getCurrentUser();
      }
    } catch (e) {
      print('Login error: $email - $e');
    }
    return null;
  }

  Future<User?> getCurrentUser() async {
    try {
      final response = await _dio.get('${AppConfig.productionUrl}/auth/me');
      if (response.statusCode == 200) {
        return User.fromJson(response.data);
      }
    } catch (e) {
      print('Get user error: $e');
    }
    return null;
  }

  Future<void> saveToken(String token) async {
    await _storage.write(key: 'jwt_token', value: token);
  }

  Future<void> saveTenantId(String tenantId) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('tenant_id', tenantId);
  }

  Future<void> logout() async {
    await _storage.delete(key: 'jwt_token');
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('tenant_id');
  }
}
