import 'package:dio/dio.dart';
import 'package:try_neuro/core/network/http_client.dart';
import 'package:try_neuro/features/resources/domain/resource_model.dart';
import 'package:try_neuro/service_locator.dart';

class ResourceService {
  final Dio _dio = sl<HttpClient>().dio;

  Future<List<Resource>> getResources() async {
    final response = await _dio.get('/resources');
    final List<dynamic> data = response.data;
    return data.map((json) => _fromJson(json)).toList();
  }

  Future<void> addResource({
    required String name,
    String? description,
  }) async {
    await _dio.post('/resources', data: {
      'name': name,
      'description': description,
    });
  }

  Future<void> updateResource(Resource resource) async {
     // Аналогично StaffService, используем POST (save) для обновления, передавая ID
    await _dio.post('/resources', data: {
      'id': resource.id,
      'name': resource.name,
      'description': resource.description,
    });
  }

  Future<void> deleteResource(String resourceId) async {
    await _dio.delete('/resources/$resourceId');
  }

  Resource _fromJson(Map<String, dynamic> json) {
    return Resource(
      id: json['id'],
      name: json['name'],
      description: json['description'],
    );
  }
}
