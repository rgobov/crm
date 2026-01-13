import 'package:dio/dio.dart';
import 'package:try_neuro/core/network/http_client.dart';
import 'package:try_neuro/features/contacts/domain/contact_model.dart';
import 'package:try_neuro/service_locator.dart';

class ContactService {
  final Dio _dio = sl<HttpClient>().dio;

  Future<List<Contact>> getContacts({String? query}) async {
    final response = await _dio.get('/contacts', queryParameters: query != null ? {'query': query} : null);
    final List<dynamic> data = response.data;
    return data.map((json) => Contact.fromJson(json)).toList();
  }

  // --- НОВОЕ: Экономный запрос количества клиентов ---
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
