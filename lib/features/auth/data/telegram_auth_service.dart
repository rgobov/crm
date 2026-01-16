import 'dart:js' as js;
import 'package:dio/dio.dart';
import 'package:telegram_web_app/telegram_web_app.dart';
import 'package:try_neuro/core/config/app_config.dart';
import 'package:try_neuro/core/network/http_client.dart';
import 'package:try_neuro/features/auth/data/auth_service.dart';
import 'package:try_neuro/service_locator.dart';

class TelegramAuthService {
  final Dio _dio = sl<HttpClient>().dio;
  final AuthService _authService = sl<AuthService>();

  /// Тот самый надежный способ, который сработал для привязки
  String? get _tgInitDataRaw {
    try {
      if (js.context.hasProperty('Telegram')) {
        final dynamic webApp = js.context['Telegram']['WebApp'];
        final String? initData = webApp['initData'];
        if (initData != null && initData.isNotEmpty) {
          return initData;
        }
      }
    } catch (e) {
      print('Auto-Login: JS Error: $e');
    }
    return null;
  }

  /// Пытается выполнить автоматический вход через Telegram
  Future<bool> tryAutoLogin() async {
    // 1. Проверяем, в Telegram ли мы
    if (!TelegramWebApp.instance.isSupported) return false;

    final String? initData = _tgInitDataRaw;
    
    if (initData == null || initData.isEmpty) {
      print('Auto-Login: InitData not found');
      return false;
    }

    try {
      print('Auto-Login: Attempting with Telegram ID...');
      
      final response = await _dio.post(
        '${AppConfig.productionUrl}/auth/telegram',
        data: {'initData': initData},
      );

      if (response.statusCode == 200) {
        final token = response.data['token'];
        final tenantId = response.data['tenantId'];
        
        await _authService.saveToken(token);
        await _authService.saveTenantId(tenantId);
        
        print('Auto-Login: SUCCESS!');
        return true;
      }
    } catch (e) {
      // Это нормально, если аккаунт еще не привязан (сервер вернет 401)
      print('Auto-Login: Failed or not linked yet.');
    }

    return false;
  }
}
