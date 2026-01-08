class Contact {
  final String id;
  final String name;
  final String phone;
  final String? email;
  final String? notes;

  Contact({
    required this.id,
    required this.name,
    required this.phone,
    this.email,
    this.notes,
  });

  factory Contact.fromJson(Map<String, dynamic> json) {
    return Contact(
      id: json['id'] as String,
      name: json['name'] as String,
      phone: json['phone'] as String,
      email: json['email'] as String?,
      notes: json['notes'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'phone': phone,
      'email': email,
      'notes': notes,
    };
  }
}
