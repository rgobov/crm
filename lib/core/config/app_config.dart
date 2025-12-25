/// Центральный конфигурационный файл приложения.
class AppConfig {
  // --- ГЛАВНЫЙ ПЕРЕКЛЮЧАТЕЛЬ ОКРУЖЕНИЯ ---
  //
  // true: приложение будет подключаться к удаленному серверу (продакшен).
  // false: приложение будет подключаться к локальному бэкенду (разработка).
  //
  static const bool isProduction = false; 

  // --- АДРЕСА СЕРВЕРОВ ---
  static const String productionUrl = 'http://738629.cloud4box.ru:8080/api';
  
  // Адрес для локальной разработки на Android эмуляторе
  static const String developmentUrlAndroid = 'http://10.0.2.2:8080/api';
  // Адрес для локальной разработки в браузере (Chrome) или iOS симуляторе
  static const String developmentUrlDefault = 'http://localhost:8080/api';
}
