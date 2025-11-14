
import 'dart:async';

import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:try_neuro/core/offline/offline_queue_service.dart';
import 'package:try_neuro/features/contacts/data/contact_service.dart';
import 'package:try_neuro/service_locator.dart';

// Сервис, который отвечает за синхронизацию данных при появлении сети
class SyncService {
  final OfflineQueueService _offlineQueue = sl<OfflineQueueService>();
  final ContactService _contactService = sl<ContactService>();
  // TODO: Добавить другие сервисы (ScheduleService, ResourceService и т.д.)

  StreamSubscription? _connectivitySubscription;
  bool _isSyncing = false;

  void start() {
    // Подписываемся на изменения статуса сети
    _connectivitySubscription = Connectivity().onConnectivityChanged.listen((result) {
      if (result.contains(ConnectivityResult.mobile) || result.contains(ConnectivityResult.wifi)) {
        // Если появилось соединение, запускаем синхронизацию
        runSync();
      }
    });
    // Также запускаем синхронизацию при старте, на случай если очередь уже есть
    runSync();
  }

  void stop() {
    _connectivitySubscription?.cancel();
  }

  Future<void> runSync() async {
    if (_isSyncing) return; // Не запускать, если уже в процессе
    _isSyncing = true;

    final operations = await _offlineQueue.getQueue();
    if (operations.isEmpty) {
      _isSyncing = false;
      return; // Очередь пуста
    }

    print('Начинаю синхронизацию. Операций в очереди: ${operations.length}');

    // Устанавливаем флаг, что мы онлайн, для всех сервисов
    _contactService.isOnline = true;
    // TODO: Установить флаг для других сервисов

    for (final op in operations) {
      try {
        // В зависимости от типа операции вызываем нужный метод
        if (op.type == 'add_contact') {
          await _contactService.addContact(
            name: op.data['name'],
            phone: op.data['phone'],
            email: op.data['email'],
            notes: op.data['notes'],
          );
        }
        // TODO: Добавить обработку других типов операций (update_contact, add_appointment и т.д.)

        print('Операция "${op.type}" успешно синхронизирована.');
      } catch (e) {
        print('Ошибка синхронизации операции "${op.type}": $e');
        // Если одна операция не удалась, не прерываем остальные
      }
    }

    await _offlineQueue.clearQueue();
    print('Синхронизация завершена. Очередь очищена.');
    _isSyncing = false;
  }
}
