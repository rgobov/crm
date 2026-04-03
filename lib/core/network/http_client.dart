import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:try_neuro/core/config/app_config.dart';
import 'package:try_neuro/core/session/session_service.dart';
import 'package:try_neuro/service_locator.dart';

class HttpClient {
  final Dio dio;

  static String getBaseUrl() {
    if (AppConfig.isProduction) {
      return AppConfig.productionUrl;
    }

    if (kIsWeb) {
      return AppConfig.developmentUrlDefault;
    }

    if (defaultTargetPlatform == TargetPlatform.android) {
      // Выбираем URL в зависимости от того, тестируем на реальном устройстве или эмуляторе
      return AppConfig.isMobileTest 
          ? AppConfig.developmentUrlAndroidDevice 
          : AppConfig.developmentUrlAndroidEmulator;
    }

    return AppConfig.developmentUrlDefault;
  }

  HttpClient() : dio = Dio(BaseOptions(
    baseUrl: getBaseUrl(),
    connectTimeout: const Duration(seconds: 10),
    receiveTimeout: const Duration(seconds: 30),
  )) {
    if (kDebugMode) {
      dio.interceptors.add(LogInterceptor(
        request: true,
        requestHeader: true,
        requestBody: true,
        responseHeader: true,
        responseBody: true,
        error: true,
        logPrint: (object) => debugPrint(object.toString()),
      ));
    }

    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) async {
          if (options.path.contains('/auth/login') || options.path.contains('/companies/register')) {
             return handler.next(options);
          }
          
          final token = await sl<SessionService>().getToken();
          if (token != null) {
            options.headers['Authorization'] = 'Bearer $token';
          }
          return handler.next(options);
        },
        onError: (DioException e, handler) {
          return handler.next(e);
        },
      ),
    );
  }
}
