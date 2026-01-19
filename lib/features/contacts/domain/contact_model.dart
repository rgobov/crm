import 'package:try_neuro/core/utils/phone_utils.dart';

class Contact {
  final String id;
  final String name;
  final List<String> phones;
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
      phones: (json['phones'] as List<dynamic>).map((e) => e as String).toList(),
      email: json['email'] as String?,
      notes: json['notes'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'phones': phones,
      'email': email,
      'notes': notes,
    };
  }

  String get displayPhone => phones.isNotEmpty ? PhoneUtils.format(phones.first) : 'Нет номера';

  // Переопределяем сравнение, чтобы Flutter понимал, что клиенты с одинаковыми id - это один и тот же объект
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is Contact && runtimeType == other.runtimeType && id == other.id;

  @override
  int get hashCode => id.hashCode;
}
