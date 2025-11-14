
class StaffMember {
  final String id;
  final String name;
  final String? specialty; // e.g., 'Парикмахер-стилист', 'Автомеханик'

  StaffMember({
    required this.id,
    required this.name,
    this.specialty,
  });
}
