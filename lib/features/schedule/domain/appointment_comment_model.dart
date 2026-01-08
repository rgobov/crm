import 'package:intl/intl.dart';

class AppointmentComment {
  final String id;
  final String appointmentId;
  final String authorId;
  final String authorName;
  final String text;
  final DateTime createdAt;

  AppointmentComment({
    required this.id,
    required this.appointmentId,
    required this.authorId,
    required this.authorName,
    required this.text,
    required this.createdAt,
  });

  factory AppointmentComment.fromJson(Map<String, dynamic> json) {
    return AppointmentComment(
      id: json['id'],
      appointmentId: json['appointmentId'],
      authorId: json['authorId'],
      authorName: json['authorName'],
      text: json['text'],
      // Парсим дату из строки ISO 8601
      createdAt: DateTime.parse(json['createdAt'] as String),
    );
  }
}
