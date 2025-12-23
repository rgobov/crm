class StaffMember {
  final String id;
  final String name;
  final String specialty;
  final String? role; // Добавили поле role

  StaffMember({
    required this.id,
    required this.name,
    required this.specialty,
    this.role,
  });

  // Если вы используете freezed или json_serializable, нужно перегенерировать код.
  // Но судя по коду, вы парсите вручную в сервисе.
}
