
import 'dart:async';
import 'package:flutter/material.dart';
import 'package:try_neuro/features/schedule/domain/appointment_model.dart';
import 'package:try_neuro/features/staff/domain/staff_member_model.dart';

class DayTimeline extends StatefulWidget {
  final DateTime day;
  final List<Appointment> appointments;
  final List<StaffMember> staff;
  final Function(Appointment) onAppointmentTap;
  final Function(TimeOfDay, String?) onEmptySlotTap;

  const DayTimeline({
    super.key,
    required this.day,
    required this.appointments,
    required this.staff,
    required this.onAppointmentTap,
    required this.onEmptySlotTap,
  });

  @override
  State<DayTimeline> createState() => _DayTimelineState();
}

class _DayTimelineState extends State<DayTimeline> {
  Timer? _timer;

  final double hourHeight = 60.0;
  final int startHour = 8;
  final int endHour = 22;
  final double timeColumnWidth = 60.0;
  final double staffColumnWidth = 150.0;

  @override
  void initState() {
    super.initState();
    _timer = Timer.periodic(const Duration(minutes: 1), (timer) {
      if (mounted) {
        setState(() {});
      }
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  Color _getStatusColor(AppointmentStatus status) {
    switch (status) {
      case AppointmentStatus.scheduled:
        return Theme.of(context).primaryColor.withOpacity(0.9);
      case AppointmentStatus.completed:
        return Colors.green.withOpacity(0.9);
      case AppointmentStatus.cancelled:
        return Colors.red.withOpacity(0.9);
    }
  }

  @override
  Widget build(BuildContext context) {
    final totalHours = endHour - startHour;
    final totalHeight = totalHours * hourHeight;

    // Включаем "Без сотрудника" как отдельную колонку, если есть такие записи
    final hasUnassigned = widget.appointments.any((a) => a.staffMemberId == null);

    return Column(
      children: [
        // Заголовки сотрудников
        SizedBox(
          height: 50,
          child: Row(
            children: [
              SizedBox(width: timeColumnWidth, child: const Center(child: Icon(Icons.access_time, size: 16))),
              Expanded(
                child: ListView(
                  scrollDirection: Axis.horizontal,
                  physics: const ClampingScrollPhysics(),
                  children: [
                    ...widget.staff.map((s) => SizedBox(
                      width: staffColumnWidth,
                      child: Center(child: Text(s.name, style: const TextStyle(fontWeight: FontWeight.bold), textAlign: TextAlign.center)),
                    )),
                    if (hasUnassigned)
                      SizedBox(
                        width: staffColumnWidth,
                        child: const Center(child: Text('Не назначен', style: TextStyle(fontStyle: FontStyle.italic))),
                      ),
                  ],
                ),
              ),
            ],
          ),
        ),
        const Divider(height: 1),
        // Основная область таймлайна
        Expanded(
          child: SingleChildScrollView(
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Колонка времени
                SizedBox(
                  width: timeColumnWidth,
                  height: totalHeight,
                  child: _buildTimeColumn(totalHeight),
                ),
                // Колонки сотрудников (горизонтальный скролл)
                Expanded(
                  child: SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: Row(
                      children: [
                        ...widget.staff.map((s) => _buildStaffColumn(context, s.id, s.name, totalHeight)),
                        if (hasUnassigned)
                          _buildStaffColumn(context, null, 'Не назначен', totalHeight),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildTimeColumn(double totalHeight) {
    return Column(
      children: List.generate(endHour - startHour, (index) {
        final hour = startHour + index;
        return SizedBox(
          height: hourHeight,
          child: Text('$hour:00', style: const TextStyle(fontSize: 12, color: Colors.grey)),
        );
      }),
    );
  }

  Widget _buildStaffColumn(BuildContext context, String? staffId, String staffName, double totalHeight) {
    // Фильтруем записи для текущего сотрудника
    final columnAppointments = widget.appointments.where((a) => a.staffMemberId == staffId).toList();

    return Container(
      width: staffColumnWidth,
      height: totalHeight,
      decoration: BoxDecoration(
        border: Border(left: BorderSide(color: Colors.grey.shade300)),
      ),
      child: Stack(
        children: [
          // Сетка времени
          Column(
            children: List.generate(endHour - startHour, (index) {
              final hour = startHour + index;
              return InkWell(
                onTap: () => widget.onEmptySlotTap(TimeOfDay(hour: hour, minute: 0), staffId),
                child: Container(
                  height: hourHeight,
                  decoration: BoxDecoration(
                    border: Border(bottom: BorderSide(color: Colors.grey.shade100)),
                  ),
                ),
              );
            }),
          ),
          // Записи
          ...columnAppointments.map((appointment) {
            final minutesFromStart = (appointment.time.hour - startHour) * 60 + appointment.time.minute;
            final top = minutesFromStart * (hourHeight / 60);
            final height = appointment.durationInMinutes * (hourHeight / 60);

            return Positioned(
              top: top,
              left: 2,
              right: 2,
              height: height,
              child: GestureDetector(
                onTap: () => widget.onAppointmentTap(appointment),
                child: Card(
                  color: _getStatusColor(appointment.status),
                  margin: EdgeInsets.zero,
                  child: Padding(
                    padding: const EdgeInsets.all(4.0),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          appointment.clientName,
                          style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.bold),
                          overflow: TextOverflow.ellipsis,
                        ),
                        Text(
                          appointment.service,
                          style: const TextStyle(color: Colors.white70, fontSize: 10),
                          overflow: TextOverflow.ellipsis,
                        ),
                        if (appointment.comment != null && appointment.comment!.isNotEmpty)
                           Padding(
                             padding: const EdgeInsets.only(top: 2),
                             child: Icon(Icons.comment, size: 10, color: Colors.white.withOpacity(0.8)),
                           ),
                      ],
                    ),
                  ),
                ),
              ),
            );
          }),
          // Индикатор текущего времени (если сегодня)
          if (_isToday) _buildCurrentTimeIndicator(staffColumnWidth),
        ],
      ),
    );
  }
  
  bool get _isToday {
    final now = DateTime.now();
    return widget.day.year == now.year && widget.day.month == now.month && widget.day.day == now.day;
  }

  Widget _buildCurrentTimeIndicator(double width) {
    final now = DateTime.now();
    if (now.hour < startHour || now.hour >= endHour) {
      return const SizedBox.shrink();
    }
    final minutesFromStart = (now.hour - startHour) * 60 + now.minute;
    final top = minutesFromStart * (hourHeight / 60);

    return Positioned(
      top: top,
      left: 0,
      width: width,
      child: Container(
        height: 2,
        color: Colors.red.withOpacity(0.5),
      ),
    );
  }
}
