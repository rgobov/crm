/// Центральный конфигурационный файл приложения.
class AppConfig {
  // ВКЛЮЧАЕМ true для работы с реальным сервером
  static const bool isProduction = true; 

  // --- ТЕСТИРОВАНИЕ НА РЕАЛЬНОМ ТЕЛЕФОНЕ ---
  static const bool isMobileTest = true;

  // Адрес вашего бэкенда (Ваш VPS IP)
  static const String productionUrl = 'http://109.248.203.156:8080/api';
  
  // IP вашего компьютера в Wi-Fi сети (для локальных тестов)
  static const String computerIp = '192.168.100.5';

  static const String developmentUrlAndroidEmulator = 'http://10.0.2.2:8080/api';
  static const String developmentUrlAndroidDevice = 'http://$computerIp:8080/api';
  static const String developmentUrlDefault = 'http://localhost:8080/api';
}
