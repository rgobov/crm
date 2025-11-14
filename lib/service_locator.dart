
import 'package:get_it/get_it.dart';
import 'package:try_neuro/core/offline/offline_queue_service.dart';
import 'package:try_neuro/core/offline/sync_service.dart';
import 'package:try_neuro/features/auth/data/auth_service.dart';
import 'package:try_neuro/features/contacts/data/contact_service.dart';
import 'package:try_neuro/features/resources/data/resource_service.dart';
import 'package:try_neuro/features/schedule/data/schedule_service.dart';
import 'package:try_neuro/features/services/data/app_service.dart';
import 'package:try_neuro/features/staff/data/staff_service.dart';

// Создаем глобальный экземпляр Service Locator
final sl = GetIt.instance;

void setupServiceLocator() {
  // Регистрируем сервисы как "ленивые синглтоны".
  sl.registerLazySingleton(() => AuthService());
  sl.registerLazySingleton(() => ContactService());
  sl.registerLazySingleton(() => ScheduleService());
  sl.registerLazySingleton(() => ResourceService());
  sl.registerLazySingleton(() => StaffService());
  sl.registerLazySingleton(() => AppService()); // <-- ДОБАВЛЕНО
  sl.registerLazySingleton(() => OfflineQueueService());
  sl.registerLazySingleton(() => SyncService());
}
