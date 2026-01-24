/// Центральный конфигурационный файл приложения.
class AppConfig {
  static const bool isProduction = true; // Установили в true для работы на сервере

  // --- ТЕСТИРОВАНИЕ НА РЕАЛЬНОМ ТЕЛЕФОНЕ ---
  // true: использовать IP компьютера (для реального устройства)
  // false: использовать 10.0.2.2 (для эмулятора)
  static const bool isMobileTest = false;

  // Адрес вашего бэкенда в Easypanel
  static const String productionUrl = 'https://tryneuro-backend.t6xfbd.easypanel.host/api';
  
  // IP вашего компьютера в Wi-Fi сети
  static const String computerIp = '192.168.100.5';

  static const String developmentUrlAndroidEmulator = 'http://10.0.2.2:8080/api';
  static const String developmentUrlAndroidDevice = 'http://$computerIp:8080/api';

  static const String developmentUrlDefault = 'http://localhost:8080/api';
}
