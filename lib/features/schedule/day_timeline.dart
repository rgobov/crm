import 'dart:async';
import 'package:flutter/material.dart';
import 'package:try_neuro/features/schedule/domain/appointment_model.dart';
import 'package:try_neuro/features/staff/domain/staff_member_model.dart';
import 'dart:math';

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

  final double hourHeight = 80.0;
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
    int startHour = 23;
    int endHour = 0;

    if (widget.staff.isEmpty) {
      startHour = 8;
      endHour = 22;
    } else {
      for (var staffMember in widget.staff) {
        if (staffMember.workStartTime != null) {
          startHour = min(startHour, staffMember.workStartTime!.hour);
        }
        if (staffMember.workEndTime != null) {
          endHour = max(endHour, staffMember.workEndTime!.hour + 1);
        }
      }
      if (startHour == 23) startHour = 8;
      if (endHour == 0) endHour = 22;
    }

    final totalHours = endHour - startHour;
    final totalHeight = totalHours * hourHeight;
    final hasUnassigned = widget.appointments.any((a) => a.staffMemberId == null);

    return Column(
      children: [
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
                          child: Center(
                            child: Column(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                Text(s.name, style: const TextStyle(fontWeight: FontWeight.bold), textAlign: TextAlign.center),
                                Text(s.specialty, style: TextStyle(fontSize: 12, color: Colors.grey.shade600), textAlign: TextAlign.center),
                              ],
                            ),
                          ))),
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
        Expanded(
          child: SingleChildScrollView(
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                SizedBox(
                  width: timeColumnWidth,
                  height: totalHeight,
                  child: _buildTimeColumn(totalHeight, startHour, endHour),
                ),
                Expanded(
                  child: SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: Row(
                      children: [
                        ...widget.staff.map((s) => _buildStaffColumn(context, s.id, s.name, totalHeight, startHour, endHour)),
                        if (hasUnassigned)
                          _buildStaffColumn(context, null, 'Не назначен', totalHeight, startHour, endHour),
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

  Widget _buildTimeColumn(double totalHeight, int startHour, int endHour) {
    return Column(
      children: List.generate(endHour - startHour, (index) {
        final hour = startHour + index;
        return SizedBox(
          height: hourHeight,
          child: Center(child: Text('$hour:00', style: const TextStyle(fontSize: 12, color: Colors.grey))),
        );
      }),
    );
  }

  Widget _buildStaffColumn(BuildContext context, String? staffId, String staffName, double totalHeight, int startHour, int endHour) {
    final columnAppointments = widget.appointments.where((a) => a.staffMemberId == staffId).toList();

    return Container(
      width: staffColumnWidth,
      height: totalHeight,
      decoration: BoxDecoration(
        border: Border(left: BorderSide(color: Colors.grey.shade300)),
      ),
      child: Stack(
        children: [
          Column(
            children: List.generate(endHour - startHour, (hourIndex) {
              final hour = startHour + hourIndex;
              return SizedBox(
                height: hourHeight,
                child: Column(
                  children: List.generate(4, (minuteIndex) {
                    final minute = minuteIndex * 15;
                    return Expanded(
                      child: InkWell(
                        onTap: () => widget.onEmptySlotTap(TimeOfDay(hour: hour, minute: minute), staffId),
                        child: Container(
                          decoration: BoxDecoration(
                            border: Border(top: BorderSide(color: minute == 0 ? Colors.grey.shade200 : Colors.grey.shade100, width: 0.5)),
                          ),
                        ),
                      ),
                    );
                  }),
                ),
              );
            }),
          ),
          ...columnAppointments.map((appointment) {
            final minutesFromStart = (appointment.time.hour - startHour) * 60 + appointment.time.minute;
            final top = minutesFromStart * (hourHeight / 60);
            final height = appointment.durationInMinutes * (hourHeight / 60);

            return Positioned(
              top: top,
              left: 2,
              right: 2,
              height: height > 0 ? height : 1,
              child: GestureDetector(
                onTap: () => widget.onAppointmentTap(appointment),
                child: Card(
                  color: _getStatusColor(appointment.status),
                  margin: EdgeInsets.zero,
                  elevation: 2,
                  child: Padding(
                    padding: const EdgeInsets.all(4.0),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisAlignment: MainAxisAlignment.start,
                      children: [
                        Text(appointment.clientName, style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.bold), overflow: TextOverflow.ellipsis),
                        Text(appointment.service, style: const TextStyle(color: Colors.white70, fontSize: 10), overflow: TextOverflow.ellipsis),
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
          // --- ИСПРАВЛЕНИЕ ЗДЕСЬ ---
          if (_isToday) _buildCurrentTimeIndicator(staffColumnWidth, startHour, endHour),
        ],
      ),
    );
  }
  
  bool get _isToday {
    final now = DateTime.now();
    return widget.day.year == now.year && widget.day.month == now.month && widget.day.day == now.day;
  }

  Widget _buildCurrentTimeIndicator(double width, int startHour, int endHour) {
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
