import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:try_neuro/core/network/http_client.dart';
import 'package:try_neuro/features/auth/data/auth_service.dart';
import 'package:try_neuro/service_locator.dart';

// Условный импорт заглушки
import 'package:try_neuro/core/utils/js_stub.dart' if (dart.library.js) 'dart:js' as js;

class TelegramAuthService {
  final Dio _dio = sl<HttpClient>().dio;
  final AuthService _authService = sl<AuthService>();

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
      debugPrint('TelegramAuth: JS Error: $e');
    }
    return null;
  }

  Future<bool> tryAutoLogin() async {
    if (!kIsWeb) return false;

    final String? initData = _tgInitDataRaw;
    if (initData == null || initData.isEmpty) return false;

    try {
      // Используем относительный путь
      final response = await _dio.post(
        '/auth/telegram',
        data: {'initData': initData},
      );

      if (response.statusCode == 200) {
        final token = response.data['token'];
        final tenantId = response.data['tenantId'];
        
        await _authService.saveToken(token);
        await _authService.saveTenantId(tenantId);
        return true;
      }
    } catch (e) {
      debugPrint('TelegramAuth: Auto-Login failed');
    }

    return false;
  }
}
