import 'package:dio/dio.dart';
import 'package:try_neuro/core/network/http_client.dart';
import 'package:try_neuro/features/contacts/domain/contact_model.dart';
import 'package:try_neuro/features/schedule/domain/appointment_model.dart';
import 'package:try_neuro/service_locator.dart';

class AdminService {
  final Dio _dio = sl<HttpClient>().dio;

  Future<List<Appointment>> getContactAppointments(String contactId) async {
    final response = await _dio.get('/admin/clients/$contactId/appointments');
    final List<dynamic> data = response.data;
    return data.map((json) => Appointment.fromJson(json)).toList();
  }

  // НОВОЕ: Редактирование клиента через админский эндпоинт
  Future<Contact> updateContact(Contact contact) async {
    final response = await _dio.put('/admin/clients/${contact.id}', data: contact.toJson());
    return Contact.fromJson(response.data);
  }
}
