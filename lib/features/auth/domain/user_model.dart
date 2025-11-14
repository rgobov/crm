
enum UserRole { admin, manager }

class User {
  final String id;
  final String email;
  final UserRole role;

  const User({
    required this.id,
    required this.email,
    required this.role,
  });
}
