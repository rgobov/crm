import 'package:dio/dio.dart';
import 'package:try_neuro/core/network/http_client.dart';
import 'package:try_neuro/features/contacts/domain/contact_model.dart';
import 'package:try_neuro/service_locator.dart';

class ContactService {
  final Dio _dio = sl<HttpClient>().dio;

  /// Получение списка контактов с пагинацией.
  /// Возвращает Map с 'contacts' (List) и 'isLast' (bool).
  Future<Map<String, dynamic>> getContactsPaged({String? query, int page = 0, int size = 20}) async {
    final response = await _dio.get('/contacts', queryParameters: {
      if (query != null && query.isNotEmpty) 'query': query,
      'page': page,
      'size': size,
    });

    final List<dynamic> content = response.data['content'];
    final bool isLast = response.data['last'] ?? true;

    return {
      'contacts': content.map((json) => Contact.fromJson(json)).toList(),
      'isLast': isLast,
    };
  }

  // Оставляем для совместимости (используется в других местах)
  Future<List<Contact>> getContacts({String? query}) async {
    final result = await getContactsPaged(query: query, size: 50);
    return result['contacts'] as List<Contact>;
  }

  Future<Contact?> getContactById(String id) async {
    try {
      final response = await _dio.get('/contacts/$id');
      return Contact.fromJson(response.data);
    } catch (e) {
      return null;
    }
  }

  Future<int> getContactsCount() async {
    final response = await _dio.get('/contacts/count');
    return response.data as int;
  }

  Future<Contact?> findContactByPhone(String phone) async {
    try {
      final response = await _dio.get('/contacts/by-phone', queryParameters: {'phone': phone});
      return Contact.fromJson(response.data);
    } catch (e) {
      return null;
    }
  }

  Future<Contact> addContact({required String name, required List<String> phones, String? email, String? notes}) async {
    final response = await _dio.post('/contacts', data: {
      'name': name,
      'phones': phones,
      'email': email,
      'notes': notes,
    });
    return Contact.fromJson(response.data);
  }

  Future<Contact> updateContact(Contact contact) async {
    final response = await _dio.put('/contacts/${contact.id}', data: contact.toJson());
    return Contact.fromJson(response.data);
  }

  Future<void> deleteContact(String id) async {
    await _dio.delete('/contacts/$id');
  }
}
