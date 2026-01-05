import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:try_neuro/core/session/token_storage.dart';
import 'package:try_neuro/features/auth/domain/user_model.dart';
import 'package:try_neuro/service_locator.dart';

class SessionService {
  final TokenStorage _storage = sl<TokenStorage>();
  User? _currentUser;

  Future<void> saveUser(User user) async {
    _currentUser = user;
    if (kIsWeb) {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('current_user', jsonEncode(user.toJson()));
    }
  }

  Future<User?> getCurrentUser() async {
    if (_currentUser != null) return _currentUser;

    if (kIsWeb) {
      final prefs = await SharedPreferences.getInstance();
      final userJson = prefs.getString('current_user');
      if (userJson != null) {
        try {
          _currentUser = User.fromJson(jsonDecode(userJson));
          return _currentUser;
        } catch(e) {
          await prefs.remove('current_user');
          return null;
        }
      }
    }
    return null;
  }

  Future<void> saveToken(String token) => _storage.saveToken(token);

  Future<String?> getToken() => _storage.getToken();

  Future<void> clearSession() async {
    _currentUser = null;
    if (kIsWeb) {
      final prefs = await SharedPreferences.getInstance();
      await prefs.remove('current_user');
    }
    await _storage.deleteToken();
  }
}
