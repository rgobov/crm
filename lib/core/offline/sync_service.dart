import 'dart:async';

import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:try_neuro/core/offline/offline_queue_service.dart';
import 'package:try_neuro/features/contacts/data/contact_service.dart';
import 'package:try_neuro/service_locator.dart';

// Сервис, который отвечает за синхронизацию данных при появлении сети
class SyncService {
  final OfflineQueueService _offlineQueue = sl<OfflineQueueService>();
  final ContactService _contactService = sl<ContactService>();

  StreamSubscription? _connectivitySubscription;
  bool _isSyncing = false;

  void start() {
    _connectivitySubscription = Connectivity().onConnectivityChanged.listen((result) {
      if (result.contains(ConnectivityResult.mobile) || result.contains(ConnectivityResult.wifi)) {
        runSync();
      }
    });
    runSync();
  }

  void stop() {
    _connectivitySubscription?.cancel();
  }

  Future<void> runSync() async {
    if (_isSyncing) return;
    _isSyncing = true;

    final operations = await _offlineQueue.getQueue();
    if (operations.isEmpty) {
      _isSyncing = false;
      return;
    }

    print('Начинаю синхронизацию. Операций в очереди: ${operations.length}');

    // --- УДАЛЕНО: _contactService.isOnline = true; (Метод отсутствует в ContactService) ---

    for (final op in operations) {
      try {
        if (op.type == 'add_contact') {
          // Гарантируем, что телефоны передаются как список
          final dynamic phoneData = op.data['phones'] ?? [op.data['phone']];
          final List<String> phones = (phoneData is List) 
              ? List<String>.from(phoneData) 
              : [phoneData.toString()];

          await _contactService.addContact(
            name: op.data['name'],
            phones: phones,
            email: op.data['email'],
            notes: op.data['notes'],
          );
        }

        print('Операция "${op.type}" успешно синхронизирована.');
      } catch (e) {
        print('Ошибка синхронизации операции "${op.type}": $e');
      }
    }

    await _offlineQueue.clearQueue();
    print('Синхронизация завершена. Очередь очищена.');
    _isSyncing = false;
  }
}
