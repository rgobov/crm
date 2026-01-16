import 'package:dio/dio.dart';
import 'package:telegram_web_app/telegram_web_app.dart';
import 'package:try_neuro/core/config/app_config.dart';
import 'package:try_neuro/core/network/http_client.dart';
import 'package:try_neuro/features/auth/data/auth_service.dart';
import 'package:try_neuro/service_locator.dart';

class TelegramAuthService {
  final Dio _dio = sl<HttpClient>().dio;
  final AuthService _authService = sl<AuthService>();

  /// Пытается выполнить автоматический вход через Telegram
  /// Возвращает true, если вход успешен.
  Future<bool> tryAutoLogin() async {
    final tg = TelegramWebApp.instance;
    
    // 1. Проверяем, запущено ли приложение в Telegram
    if (!tg.isSupported || tg.initData.isEmpty) {
      return false;
    }

    try {
      // 2. Отправляем initData на наш бэкенд
      final response = await _dio.post(
        '${AppConfig.productionUrl}/auth/telegram',
        data: {'initData': tg.initData},
      );

      if (response.statusCode == 200) {
        final token = response.data['token'];
        final tenantId = response.data['tenantId'];
        
        // 3. Сохраняем токен и tenantId (используем существующий AuthService)
        await _authService.saveToken(token);
        await _authService.saveTenantId(tenantId);
        
        print('Telegram Auto-Login Success!');
        return true;
      }
    } catch (e) {
      print('Telegram Auto-Login failed: $e');
    }

    return false;
  }
}
