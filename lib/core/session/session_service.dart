import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class SessionService {
  final _storage = const FlutterSecureStorage();

  static const _tenantIdKey = 'tenant_id';

  Future<void> saveSession(String tenantId) async {
    await _storage.write(key: _tenantIdKey, value: tenantId);
  }

  Future<String?> getTenantId() async {
    return await _storage.read(key: _tenantIdKey);
  }

  Future<void> clearSession() async {
    await _storage.delete(key: _tenantIdKey);
  }
}
