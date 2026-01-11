import 'package:dio/dio.dart';
import 'package:try_neuro/core/network/http_client.dart';
import 'package:try_neuro/features/contacts/domain/contact_model.dart';
import 'package:try_neuro/service_locator.dart';

class ContactService {
  final Dio _dio = sl<HttpClient>().dio;
  
  bool isOnline = true;

  Future<List<Contact>> getContacts({String? query}) async {
    final response = await _dio.get('/contacts', queryParameters: {
      if (query != null && query.isNotEmpty) 'query': query,
    });
    final List<dynamic> data = response.data;
    return data.map((json) => Contact.fromJson(json as Map<String, dynamic>)).toList();
  }

  Future<Contact?> findContactByPhone(String phone) async {
    try {
      final response = await _dio.get('/contacts/by-phone', queryParameters: {'phone': phone});
      return Contact.fromJson(response.data as Map<String, dynamic>);
    } on DioException catch (e) {
      if (e.response?.statusCode == 404) return null;
      rethrow;
    }
  }

  Future<Contact> addContact({
    required String name,
    required List<String> phones, // --- ИЗМЕНЕНИЕ: Список ---
    String? email,
    String? notes,
  }) async {
    final response = await _dio.post('/contacts', data: {
      'name': name,
      'phones': phones, // Отправляем как массив
      'email': email,
      'notes': notes,
    });
    return Contact.fromJson(response.data as Map<String, dynamic>);
  }

  Future<void> updateContact(Contact contact) async {
    await _dio.put('/contacts/${contact.id}', data: contact.toJson());
  }

  Future<void> deleteContact(String id) async {
    await _dio.delete('/contacts/$id');
  }
}
