import 'package:dio/dio.dart';
import 'package:try_neuro/core/network/http_client.dart';
import 'package:try_neuro/features/services/domain/service_model.dart';
import 'package:try_neuro/service_locator.dart';

class AppService {
  final Dio _dio = sl<HttpClient>().dio;

  // ОБНОВЛЕНО: Используем админский эндпоинт для изоляции данных
  Future<List<Service>> getServices() async {
    final response = await _dio.get('/admin/services');
    final List<dynamic> data = response.data;
    return data.map((json) => _fromJson(json)).toList();
  }

  Future<void> addService({
    required String name,
    required int durationInMinutes,
  }) async {
    await _dio.post('/admin/services', data: {
      'name': name,
      'durationInMinutes': durationInMinutes,
    });
  }

  Future<void> updateService(Service service) async {
    await _dio.post('/admin/services', data: {
      'id': service.id,
      'name': service.name,
      'durationInMinutes': service.durationInMinutes,
    });
  }

  Future<void> deleteService(String serviceId) async {
    await _dio.delete('/admin/services/$serviceId');
  }

  Service _fromJson(Map<String, dynamic> json) {
    return Service(
      id: json['id'],
      name: json['name'],
      durationInMinutes: json['durationInMinutes'],
    );
  }
}
