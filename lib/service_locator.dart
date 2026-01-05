import 'package:get_it/get_it.dart';
import 'package:try_neuro/core/network/http_client.dart';
import 'package:try_neuro/core/session/session_service.dart';
import 'package:try_neuro/core/session/token_storage.dart';
import 'package:try_neuro/core/offline/offline_queue_service.dart';
import 'package:try_neuro/core/offline/sync_service.dart';
import 'package:try_neuro/features/auth/data/auth_service.dart';
import 'package:try_neuro/features/contacts/data/contact_service.dart';
import 'package:try_neuro/features/manager/data/manager_service.dart'; // Импортируем
import 'package:try_neuro/features/resources/data/resource_service.dart';
import 'package:try_neuro/features/schedule/data/schedule_service.dart';
import 'package:try_neuro/features/services/data/app_service.dart';
import 'package:try_neuro/features/staff/data/staff_service.dart';

final sl = GetIt.instance;

void setupServiceLocator() {
  sl.registerLazySingleton<TokenStorage>(() => tokenStorage);

  // Core сервисы
  sl.registerLazySingleton(() => SessionService());
  sl.registerLazySingleton(() => HttpClient());

  // Feature сервисы
  sl.registerLazySingleton(() => AuthService());
  sl.registerLazySingleton(() => ContactService());
  sl.registerLazySingleton(() => ScheduleService());
  sl.registerLazySingleton(() => ResourceService());
  sl.registerLazySingleton(() => StaffService());
  sl.registerLazySingleton(() => AppService());
  sl.registerLazySingleton(() => ManagerService()); // Регистрируем
  
  // Offline
  sl.registerLazySingleton(() => OfflineQueueService());
  sl.registerLazySingleton(() => SyncService());
}
