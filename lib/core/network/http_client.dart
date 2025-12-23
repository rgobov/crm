import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:try_neuro/core/session/session_service.dart';
import 'package:try_neuro/service_locator.dart';

class HttpClient {
  final Dio dio;

  // Определяем базовый URL
  static String get _baseUrl {
    // В режиме отладки (на эмуляторе) можно раскомментировать нужную строку, 
    // если хотите тестировать локально.
    // Но для продакшена (Release) всегда должен быть реальный сервер.
    
    const String prodUrl = 'http://738629.cloud4box.ru:8080/api';
    
    if (kReleaseMode) {
      return prodUrl;
    }
    
    // В Debug режиме:
    // Если хотите тестировать подключение к удаленному серверу из дома - верните prodUrl
    // return prodUrl; 
    
    // Стандартная логика для локальной разработки:
    if (kIsWeb) {
      return 'http://localhost:8080/api'; 
    } else if (defaultTargetPlatform == TargetPlatform.android) {
      return 'http://10.0.2.2:8080/api'; 
    } else {
      return 'http://localhost:8080/api'; 
    }
  }

  // ВАЖНО: Сейчас я оставлю жестко прописанный URL сервера для тестов, 
  // чтобы вы могли проверить работу сразу. 
  // Когда захотите вернуться к локальной разработке - закомментируйте эту строку и раскомментируйте _baseUrl выше.
  // static const String activeUrl = 'http://738629.cloud4box.ru:8080/api';
  
  // А пока используем умную логику:
  // Если вы хотите сейчас проверить сервер - раскомментируйте prodUrl в блоке Debug выше или используйте Release сборку.
  // Но я сделаю проще для вас сейчас:
  
  static String get activeUrl => 'http://738629.cloud4box.ru:8080/api'; 

  HttpClient() : dio = Dio(BaseOptions(
    baseUrl: activeUrl, // Используем адрес вашего сервера
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
