
import 'package:try_neuro/features/auth/domain/user_model.dart';

class AuthService {
  // Имитация списка пользователей в базе данных
  static final List<User> _users = [
    const User(id: '1', email: 'admin@demo.com', role: UserRole.admin, tenantId: 'tenant1'),
    const User(id: '2', email: 'manager@demo.com', role: UserRole.manager, tenantId: 'tenant1'),
    const User(id: '3', email: 'staff@demo.com', role: UserRole.employee, staffId: '1', tenantId: 'tenant1'),
  ];

  // Имитация паролей
  static final Map<String, String> _passwords = {
    'admin@demo.com': 'adminpass',
    'manager@demo.com': 'managerpass',
    'staff@demo.com': 'staffpass',
  };

  Future<User?> login(String email, String password) async {
    // Имитация задержки сети
    await Future.delayed(const Duration(seconds: 1));

    if (_passwords.containsKey(email) && _passwords[email] == password) {
      return _users.firstWhere((user) => user.email == email);
    }

    return null; // Если пользователь не найден или пароль неверный
  }

  Future<bool> registerCompany({
    required String companyName,
    required String address,
    required String adminEmail,
    required String adminPassword,
  }) async {
    await Future.delayed(const Duration(seconds: 1));

    if (_passwords.containsKey(adminEmail)) {
      return false; // Email занят
    }

    final newTenantId = DateTime.now().millisecondsSinceEpoch.toString();
    final newUser = User(
      id: DateTime.now().millisecondsSinceEpoch.toString(),
      email: adminEmail,
      role: UserRole.admin,
      tenantId: newTenantId,
    );

    _users.add(newUser);
    _passwords[adminEmail] = adminPassword;

    return true;
  }
}
