import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:shared_preferences/shared_preferences.dart';

// Абстрактный класс, определяющий контракт для хранения токена
abstract class TokenStorage {
  Future<void> saveToken(String token);
  Future<String?> getToken();
  Future<void> deleteToken();
}

// Реализация для мобильных устройств
class SecureTokenStorage implements TokenStorage {
  final _storage = const FlutterSecureStorage();

  @override
  Future<void> saveToken(String token) => _storage.write(key: 'auth_token', value: token);

  @override
  Future<String?> getToken() => _storage.read(key: 'auth_token');

  @override
  Future<void> deleteToken() => _storage.delete(key: 'auth_token');
}

// Реализация для веба
class WebTokenStorage implements TokenStorage {
  Future<SharedPreferences> get _prefs => SharedPreferences.getInstance();

  @override
  Future<void> saveToken(String token) async => (await _prefs).setString('auth_token', token);

  @override
  Future<String?> getToken() async => (await _prefs).getString('auth_token');

  @override
  Future<void> deleteToken() async => (await _prefs).remove('auth_token');
}

// Фабрика, которая возвращает нужную реализацию в зависимости от платформы
TokenStorage get tokenStorage {
  if (kIsWeb) {
    return WebTokenStorage();
  }
  return SecureTokenStorage();
}
