class Contact {
  final String id;
  final String name;
  final List<String> phones; // --- ИЗМЕНЕНИЕ: Список строк ---
  final String? email;
  final String? notes;

  Contact({
    required this.id,
    required this.name,
    required this.phones,
    this.email,
    this.notes,
  });

  factory Contact.fromJson(Map<String, dynamic> json) {
    return Contact(
      id: json['id'] as String,
      name: json['name'] as String,
      // Приводим динамический список к списку строк
      phones: (json['phones'] as List<dynamic>).map((e) => e as String).toList(),
      email: json['email'] as String?,
      notes: json['notes'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'phones': phones, // Список автоматически сериализуется в JSON-массив
      'email': email,
      'notes': notes,
    };
  }

  // Вспомогательный геттер для отображения основного номера
  String get displayPhone => phones.isNotEmpty ? phones.first : 'Нет номера';
}
