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
      id: json['id'] as String,
      appointmentId: json['appointmentId'] as String,
      authorId: json['authorId'] as String,
      authorName: json['authorName'] as String,
      text: json['text'] as String,
      // DateTime.parse понимает формат ISO 8601 с часовым поясом (Z или +HH:mm)
      createdAt: DateTime.parse(json['createdAt'] as String),
    );
  }
}
