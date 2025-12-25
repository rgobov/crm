import 'package:try_neuro/core/session/token_storage.dart';
import 'package:try_neuro/service_locator.dart';

class SessionService {
  // Зависим от абстракции, а не от конкретной реализации
  final TokenStorage _storage = sl<TokenStorage>();

  String? _currentToken;

  Future<void> saveToken(String token) {
    _currentToken = token;
    return _storage.saveToken(token);
  }

  Future<String?> getToken() async {
    if (_currentToken != null) {
      return _currentToken;
    }
    _currentToken = await _storage.getToken();
    return _currentToken;
  }

  Future<void> clearSession() {
    _currentToken = null;
    return _storage.deleteToken();
  }
}
