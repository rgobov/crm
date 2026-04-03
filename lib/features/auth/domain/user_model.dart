
enum UserRole { admin, manager, employee }

class User {
  final String id;
  final String email;
  final UserRole role;
  final String? staffId;
  final String? tenantId;
  final int? telegramId; // Оставляем одну строку

  const User({
    required this.id,
    required this.email,
    required this.role,
    this.staffId,
    this.tenantId,
    this.telegramId,
  });

  factory User.fromJson(Map<String, dynamic> json) {
    // Безопасно определяем роль (игнорируя регистр букв)
    UserRole detectedRole = UserRole.employee;
    final String incomingRole = (json['role'] ?? '').toString().toUpperCase();
    
    if (incomingRole == 'ADMIN') {
      detectedRole = UserRole.admin;
    } else if (incomingRole == 'MANAGER') {
      detectedRole = UserRole.manager;
    }

    return User(
      id: json['id'] ?? '',
      email: json['email'] ?? '',
      role: detectedRole,
      staffId: json['staffId'],
      tenantId: json['tenantId'],
      telegramId: json['telegramId'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'email': email,
      'role': role.name.toUpperCase(), // Отправляем на бэкенд в верхнем регистре
      'staffId': staffId,
      'tenantId': tenantId,
      'telegramId': telegramId,
    };
  }
}
