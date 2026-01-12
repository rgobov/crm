import 'package:dio/dio.dart';
import 'package:try_neuro/core/network/http_client.dart';
import 'package:try_neuro/service_locator.dart';

class TimeService {
  final Dio _dio = sl<HttpClient>().dio;
  
  // Разница между временем сервера и временем устройства в миллисекундах
  int _offset = 0;
  bool _isSynced = false;

  /// Инициализация сервиса: получаем время с сервера
  Future<void> sync() async {
    try {
      final startTime = DateTime.now().millisecondsSinceEpoch;
      final response = await _dio.get('/system/time');
      final serverTimestamp = response.data['timestamp'] as int;
      final endTime = DateTime.now().millisecondsSinceEpoch;

      // Учитываем время на сетевой запрос (делим пополам)
      final networkLatency = (endTime - startTime) ~/ 2;
      
      final adjustedServerTime = serverTimestamp + networkLatency;
      _offset = adjustedServerTime - endTime;
      _isSynced = true;
      
      print('Time synced. Server offset: $_offset ms');
    } catch (e) {
      print('Failed to sync time: $e. Using device time.');
      _offset = 0; // В случае ошибки доверяем устройству
    }
  }

  /// Возвращает текущее время с учетом серверного смещения
  DateTime now() {
    return DateTime.now().add(Duration(milliseconds: _offset));
  }

  bool get isSynced => _isSynced;
}
