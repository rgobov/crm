import 'dart:async';
import 'package:flutter/material.dart';
import 'package:try_neuro/core/session/session_service.dart';
import 'package:try_neuro/features/auth/domain/user_model.dart';
import 'package:try_neuro/features/manager/data/manager_service.dart';
import 'package:try_neuro/features/schedule/domain/appointment_model.dart';
import 'package:try_neuro/features/staff/data/employee_service.dart';
import 'package:try_neuro/features/staff/domain/staff_member_model.dart';
import 'package:try_neuro/service_locator.dart';
import 'dart:math';
import 'striped_background_painter.dart';

class DayTimeline extends StatefulWidget {
  final DateTime day;
  final List<Appointment> appointments;
  final List<StaffMember> staff;
  final Function(Appointment) onAppointmentTap;
  final Function(TimeOfDay, String?) onEmptySlotTap;
  final Future<void> Function() onRefresh;

  const DayTimeline({
    super.key,
    required this.day,
    required this.appointments,
    required this.staff,
    required this.onAppointmentTap,
    required this.onEmptySlotTap,
    required this.onRefresh,
  });

  @override
  State<DayTimeline> createState() => _DayTimelineState();
}

class _DayTimelineState extends State<DayTimeline> {
  final _sessionService = sl<SessionService>();
  final _managerService = sl<ManagerService>();
  final _employeeService = sl<EmployeeService>();

  Timer? _timer;
  String? _selectedAppointmentId;
  User? _currentUser;
  bool _isUpdatingStatus = false;

  final double hourHeight = 80.0;
  final double timeColumnWidth = 60.0;
  final double staffColumnWidth = 150.0;

  @override
  void initState() {
    super.initState();
    _sessionService.getCurrentUser().then((user) {
      if (mounted) setState(() => _currentUser = user);
    });
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

  Future<void> _updateStatus(Appointment appointment, AppointmentStatus newStatus) async {
    if (_isUpdatingStatus) return;
    setState(() => _isUpdatingStatus = true);

    try {
      final updatedAppointment = appointment.copyWith(status: newStatus);
      
      if (_currentUser?.role == UserRole.manager) {
        await _managerService.updateAppointment(updatedAppointment);
      } else if (_currentUser?.role == UserRole.employee) {
        await _employeeService.updateAppointment(updatedAppointment);
      }
      
      await widget.onRefresh();
      if (mounted) {
        setState(() => _selectedAppointmentId = null);
      }

    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Ошибка: ${e.toString()}')));
      }
    } finally {
      if (mounted) {
        setState(() => _isUpdatingStatus = false);
      }
    }
  }

  Color _getStatusColor(AppointmentStatus status) {
    final primaryColor = Theme.of(context).primaryColor;
    switch (status) {
      case AppointmentStatus.scheduled:
        return primaryColor.withOpacity(0.9);
      case AppointmentStatus.completed:
        return Colors.green.withOpacity(0.9);
      case AppointmentStatus.cancelled:
        return Colors.red.withOpacity(0.9);
    }
  }

  bool get _isToday {
    final now = DateTime.now();
    return widget.day.year == now.year && widget.day.month == now.month && widget.day.day == now.day;
  }

  @override
  Widget build(BuildContext context) {
    int startHour = 23;
    int endHour = 0;

    if (widget.staff.isEmpty && widget.appointments.isEmpty) {
      startHour = 8;
      endHour = 22;
    } else {
      for (var staffMember in widget.staff) {
        if (staffMember.workStartTime != null) {
          startHour = min(startHour, staffMember.workStartTime!.hour);
        }
        if (staffMember.workEndTime != null) {
          endHour = max(endHour, staffMember.workEndTime!.hour);
        }
      }
      for (var appointment in widget.appointments) {
        startHour = min(startHour, appointment.time.hour);
        final appointmentEndHour = (appointment.time.hour * 60 + appointment.time.minute + appointment.durationInMinutes) / 60;
        endHour = max(endHour, appointmentEndHour.ceil());
      }

      if (startHour == 23) startHour = 8;
      if (endHour == 0) endHour = 22;
    }
    endHour = min(endHour + 1, 24);

    final totalHours = endHour - startHour;
    final totalHeight = totalHours * hourHeight;
    final hasUnassigned = widget.appointments.any((a) => a.staffMemberId == null);

    return GestureDetector(
      onTap: () => setState(() => _selectedAppointmentId = null),
      child: Column(
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
                          ...widget.staff.map((s) => _buildStaffColumn(context, s, totalHeight, startHour, endHour)),
                          if (hasUnassigned)
                            _buildStaffColumn(context, null, totalHeight, startHour, endHour),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
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

  Widget _buildStaffColumn(BuildContext context, StaffMember? staffMember, double totalHeight, int startHour, int endHour) {
    final columnAppointments = widget.appointments.where((a) => a.staffMemberId == staffMember?.id).toList();

    return Container(
      width: staffColumnWidth,
      height: totalHeight,
      decoration: BoxDecoration(border: Border(left: BorderSide(color: Colors.grey.shade300))),
      child: Stack(
        children: [
          ..._buildWorkingHoursBackground(staffMember, totalHeight, startHour, endHour),
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
                        onTap: () => widget.onEmptySlotTap(TimeOfDay(hour: hour, minute: minute), staffMember?.id),
                        child: Container(decoration: BoxDecoration(border: Border(top: BorderSide(color: minute == 0 ? Colors.grey.shade200 : Colors.grey.shade100, width: 0.5)))),
                      ),
                    );
                  }),
                ),
              );
            }),
          ),
          ...columnAppointments.map((appointment) => _buildAppointmentCard(appointment, startHour, endHour)),
          if (_isToday) _buildCurrentTimeIndicator(staffColumnWidth, startHour, endHour),
        ],
      ),
    );
  }

  Widget _buildAppointmentCard(Appointment appointment, int startHour, int endHour) {
    final minutesFromStart = (appointment.time.hour - startHour) * 60 + appointment.time.minute;
    final top = minutesFromStart * (hourHeight / 60);
    final height = appointment.durationInMinutes * (hourHeight / 60);
    final isSelected = _selectedAppointmentId == appointment.id;

    return Positioned(
      top: top,
      left: 2,
      right: 2,
      height: height > 0 ? height : 1,
      child: GestureDetector(
        onTap: () {
          setState(() {
            if (isSelected) {
              _selectedAppointmentId = null; 
            } else {
              _selectedAppointmentId = appointment.id;
            }
          });
        },
        child: Stack(
          fit: StackFit.expand,
          children: [
            Card(
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
                  ],
                ),
              ),
            ),
            if (isSelected)
              AnimatedOpacity(
                opacity: isSelected ? 1.0 : 0.0,
                duration: const Duration(milliseconds: 200),
                child: Container(
                  decoration: BoxDecoration(color: Colors.black.withOpacity(0.5), borderRadius: BorderRadius.circular(4)),
                  child: _isUpdatingStatus && _selectedAppointmentId == appointment.id
                      ? const Center(child: SizedBox(width: 24, height: 24, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2)))
                      : Row(
                          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                          children: [
                            IconButton(icon: const Icon(Icons.check_circle, color: Colors.greenAccent), onPressed: () => _updateStatus(appointment, AppointmentStatus.completed), tooltip: 'Выполнено'),
                            IconButton(icon: const Icon(Icons.cancel, color: Colors.redAccent), onPressed: () => _updateStatus(appointment, AppointmentStatus.cancelled), tooltip: 'Клиент не пришел'),
                            IconButton(icon: const Icon(Icons.info_outline, color: Colors.white), onPressed: () => widget.onAppointmentTap(appointment), tooltip: 'Детали'),
                          ],
                        ),
                ),
              ),
          ],
        ),
      ),
    );
  }

  List<Widget> _buildWorkingHoursBackground(StaffMember? staff, double totalHeight, int startHour, int endHour) {
    double timeToY(TimeOfDay time) {
      final double minutesFromTimelineStart = ((time.hour - startHour) * 60 + time.minute).toDouble();
      return minutesFromTimelineStart * (hourHeight / 60);
    }

    if (staff == null || staff.workStartTime == null || staff.workEndTime == null) {
      return [Positioned.fill(child: StripedBackground(backgroundColor: Colors.black.withOpacity(0.08)))];
    }

    final List<Widget> backgroundBlocks = [];

    final workStartPos = timeToY(staff.workStartTime!);
    final workEndPos = timeToY(staff.workEndTime!);

    if (workStartPos > 0) {
      backgroundBlocks.add(
        Positioned(
          top: 0,
          left: 0,
          right: 0,
          height: workStartPos,
          child: StripedBackground(backgroundColor: Colors.black.withOpacity(0.08)),
        ),
      );
    }

    if (staff.breakStartTime != null && staff.breakEndTime != null) {
      final breakStartPos = timeToY(staff.breakStartTime!);
      final breakEndPos = timeToY(staff.breakEndTime!);
      if (breakEndPos > breakStartPos) {
        backgroundBlocks.add(
          Positioned(
            top: breakStartPos,
            left: 0,
            right: 0,
            height: breakEndPos - breakStartPos,
            child: const StripedBackground(backgroundColor: Color(0xFFE0E0E0), stripeColor: Colors.black26),
          ),
        );
      }
    }

    if (workEndPos < totalHeight) {
      backgroundBlocks.add(
        Positioned(
          top: workEndPos,
          left: 0,
          right: 0,
          bottom: 0,
          child: StripedBackground(backgroundColor: Colors.black.withOpacity(0.08)),
        ),
      );
    }

    return backgroundBlocks;
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
