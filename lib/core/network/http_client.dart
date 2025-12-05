import 'package:dio/dio.dart';
import 'package:try_neuro/core/session/session_service.dart';
import 'package:try_neuro/service_locator.dart';

class HttpClient {
  final Dio dio;

  HttpClient() : dio = Dio(BaseOptions(baseUrl: 'http://10.0.2.2:8080/api')) { // 10.0.2.2 - для Android эмулятора
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) async {
          // Получаем tenantId из сессии
          final tenantId = await sl<SessionService>().getTenantId();
          if (tenantId != null) {
            options.headers['X-Tenant-ID'] = tenantId;
          }
          return handler.next(options); // Продолжаем запрос
        },
        onError: (DioException e, handler) {
          // Здесь можно обработать ошибки, например, 401 Unauthorized
          return handler.next(e);
        },
      ),
    );
  }
}
