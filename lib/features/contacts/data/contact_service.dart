
import 'dart:math';

import 'package:try_neuro/core/offline/offline_queue_service.dart';
import 'package:try_neuro/features/contacts/domain/contact_model.dart';
import 'package:try_neuro/service_locator.dart';

class ContactService {
  final OfflineQueueService _offlineQueue = sl<OfflineQueueService>();
  bool isOnline = true;

  final List<Contact> _contacts = [
    Contact(id: '1', name: 'Тестовый Клиент', phone: '+7 999 123-45-67', email: 'test@client.com'),
    Contact(id: '2', name: 'Анна Сидорова', phone: '+7 921 987-65-43'),
    Contact(id: '3', name: 'Сергей Воронов', phone: '+7 911 555-44-33'),
    Contact(id: '4', name: 'Иван Петров', phone: '+7 952 111-22-00'),
  ];
  
  Future<List<Contact>> getContacts({String? query}) async {
    await Future.delayed(const Duration(milliseconds: 300));
    
    if (!isOnline) {
       throw Exception('Нет подключения к сети');
    }

    if (query == null || query.isEmpty) {
      return List.unmodifiable(_contacts);
    }

    final lowerCaseQuery = query.toLowerCase();
    return _contacts.where((contact) {
      final nameMatch = contact.name.toLowerCase().contains(lowerCaseQuery);
      final phoneMatch = contact.phone.replaceAll(RegExp(r'[^0-9]'), '').contains(lowerCaseQuery.replaceAll(RegExp(r'[^0-9]'), ''));
      return nameMatch || phoneMatch;
    }).toList();
  }

  Future<void> addContact({
    required String name,
    required String phone,
    String? email,
    String? notes,
  }) async {
    final newId = DateTime.now().millisecondsSinceEpoch.toString(); // ИСПРАВЛЕНИЕ
    final newContact = Contact(
      id: newId, 
      name: name,
      phone: phone,
      email: email,
      notes: notes,
    );

    if (isOnline) {
      await Future.delayed(const Duration(milliseconds: 500));
      _contacts.add(newContact);
    } else {
      _contacts.add(newContact);
      await _offlineQueue.addToQueue(OfflineOperation(
        type: 'add_contact',
        data: {'name': name, 'phone': phone, 'email': email, 'notes': notes},
      ));
    }
  }

  Future<void> updateContact(Contact contact) async {
    await Future.delayed(const Duration(milliseconds: 500));
    final index = _contacts.indexWhere((c) => c.id == contact.id);
    if (index != -1) {
      _contacts[index] = contact;
    }
  }
}
