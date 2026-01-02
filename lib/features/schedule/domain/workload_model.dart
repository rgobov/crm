class Workload {
  final int day;
  final int appointmentCount;

  Workload({required this.day, required this.appointmentCount});

  factory Workload.fromJson(Map<String, dynamic> json) {
    return Workload(
      day: json['day'],
      appointmentCount: (json['appointmentCount'] as num).toInt(),
    );
  }
}
