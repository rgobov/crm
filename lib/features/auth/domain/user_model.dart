
enum UserRole { admin, manager, employee }

class User {
  final String id;
  final String email;
  final UserRole role;
  final String? staffId; // ID сотрудника, если роль employee
  final String? tenantId; // ID компании

  const User({
    required this.id,
    required this.email,
    required this.role,
    this.staffId,
    this.tenantId,
  });

  // Методы для сохранения/загрузки пользователя в/из памяти
  factory User.fromJson(Map<String, dynamic> json) {
    return User(
      id: json['id'],
      email: json['email'],
      role: UserRole.values.firstWhere((e) => e.name == json['role']),
      staffId: json['staffId'],
      tenantId: json['tenantId'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'email': email,
      'role': role.name,
      'staffId': staffId,
      'tenantId': tenantId,
    };
  }
}
