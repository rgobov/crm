
import 'package:try_neuro/features/auth/domain/user_model.dart';

class AuthService {
  // Имитация списка пользователей в базе данных
  static final List<User> _users = [
    const User(id: '1', email: 'admin@demo.com', role: UserRole.admin),
    const User(id: '2', email: 'manager@demo.com', role: UserRole.manager),
  ];

  // Имитация паролей
  static final Map<String, String> _passwords = {
    'admin@demo.com': 'adminpass',
    'manager@demo.com': 'managerpass',
  };

  Future<User?> login(String email, String password) async {
    // Имитация задержки сети
    await Future.delayed(const Duration(seconds: 1));

    if (_passwords.containsKey(email) && _passwords[email] == password) {
      return _users.firstWhere((user) => user.email == email);
    }

    return null; // Если пользователь не найден или пароль неверный
  }
}
