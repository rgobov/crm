
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
}
