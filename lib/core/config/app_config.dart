/// Центральный конфигурационный файл приложения.
class AppConfig {
  static const bool isProduction = true; 

  // --- ТЕСТИРОВАНИЕ НА РЕАЛЬНОМ ТЕЛЕФОНЕ!!! ---
  static const bool isMobileTest = true;

  // Исправленный адрес (точки вместо дефисов, чтобы совпадало с Nginx Proxy Manager)
  static const String productionUrl = 'https://api.109.248.203.156.sslip.io/api';
  
  static const String computerIp = '192.168.100.5';
  static const String developmentUrlAndroidEmulator = 'http://10.0.2.2:8080/api';
  static const String developmentUrlAndroidDevice = 'http://$computerIp:8080/api';
  static const String developmentUrlDefault = 'http://localhost:8080/api';
}
