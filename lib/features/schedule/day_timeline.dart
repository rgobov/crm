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
  final Function(Appointment) onAppointmentUpdated;

  const DayTimeline({
    super.key,
    required this.day,
    required this.appointments,
    required this.staff,
    required this.onAppointmentTap,
    required this.onEmptySlotTap,
    required this.onAppointmentUpdated,
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
      final appointmentWithNewStatus = appointment.copyWith(status: newStatus);
      late final Appointment updatedAppointment;
      
      if (_currentUser?.role == UserRole.manager || _currentUser?.role == UserRole.admin) {
        updatedAppointment = await _managerService.updateAppointment(appointmentWithNewStatus);
      } else if (_currentUser?.role == UserRole.employee) {
        updatedAppointment = await _employeeService.updateAppointment(appointmentWithNewStatus);
      } else {
        return;
      }
      
      widget.onAppointmentUpdated(updatedAppointment);
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
    switch (status) {
      case AppointmentStatus.scheduled:
        return const Color(0xFF42A5F5); 
      case AppointmentStatus.confirmed:
        return const Color(0xFF26A69A); 
      case AppointmentStatus.needs_call:
        return const Color(0xFFFFA726); 
      case AppointmentStatus.completed:
        return const Color(0xFF90A4AE); 
      case AppointmentStatus.cancelled:
        return const Color(0xFFEF5350); 
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
            height: 60,
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
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  Text(
                                    s.name, 
                                    style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13), 
                                    textAlign: TextAlign.center,
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                  ),
                                  Text(
                                    s.specialty, 
                                    style: TextStyle(fontSize: 11, color: Colors.grey.shade600), 
                                    textAlign: TextAlign.center,
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                  ),
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
                    child: _buildTimeColumn(startHour, endHour),
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

  Widget _buildTimeColumn(int startHour, int endHour) {
    return Column(
      children: List.generate(endHour - startHour, (index) {
        final hour = startHour + index;
        return SizedBox(
          height: hourHeight,
          child: Align(
            alignment: Alignment.topCenter,
            child: Text(
              '$hour:00', 
              style: const TextStyle(fontSize: 12, color: Colors.grey, fontWeight: FontWeight.w500)
            ),
          ),
        );
      }),
    );
  }

  Widget _buildStaffColumn(BuildContext context, StaffMember? staffMember, double totalHeight, int startHour, int endHour) {
    final columnAppointments = widget.appointments.where((a) => a.staffMemberId == staffMember?.id).toList();
    
    final sortedAppointments = List<Appointment>.from(columnAppointments);
    sortedAppointments.sort((a, b) {
      if (a.id == _selectedAppointmentId) return 1;
      if (b.id == _selectedAppointmentId) return -1;
      return 0;
    });

    final double slotHeight = hourHeight / 4; 

    return Container(
      width: staffColumnWidth,
      height: totalHeight,
      decoration: BoxDecoration(border: Border(left: BorderSide(color: Colors.grey.shade300))),
      child: Stack(
        clipBehavior: Clip.none,
        children: [
          ..._buildWorkingHoursBackground(staffMember, totalHeight, startHour, endHour),
          
          Positioned.fill(
            child: Column(
              children: List.generate((endHour - startHour) * 4, (slotIndex) {
                final int hourOffset = slotIndex ~/ 4;
                final int minuteOffset = (slotIndex % 4) * 15;
                final int currentHour = startHour + hourOffset;

                return SizedBox(
                  height: slotHeight,
                  child: Material(
                    color: Colors.transparent,
                    child: InkWell(
                      onTap: () => widget.onEmptySlotTap(
                        TimeOfDay(hour: currentHour, minute: minuteOffset), 
                        staffMember?.id
                      ),
                      child: Container(
                        decoration: BoxDecoration(
                          border: Border(
                            top: BorderSide(
                              color: minuteOffset == 0 ? Colors.grey.shade300 : Colors.grey.shade100, 
                              width: minuteOffset == 0 ? 1.0 : 0.5,
                            ),
                          ),
                        ),
                      ),
                    ),
                  ),
                );
              }),
            ),
          ),

          ...sortedAppointments.map((appointment) => _buildAppointmentCard(appointment, startHour, endHour)),
          
          if (_isToday) _buildCurrentTimeIndicator(staffColumnWidth, startHour, endHour),
        ],
      ),
    );
  }

  Widget _buildAppointmentCard(Appointment appointment, int startHour, int endHour) {
    final minutesFromStart = (appointment.time.hour - startHour) * 60 + appointment.time.minute;
    final top = minutesFromStart * (hourHeight / 60);
    final double actualHeight = appointment.durationInMinutes * (hourHeight / 60);
    final isSelected = _selectedAppointmentId == appointment.id;
    
    // При выборе карточка расширяется до 95 пикселей
    final double displayHeight = isSelected ? max(actualHeight, 95.0) : actualHeight;

    // Условие: показывать ли текст внутри карточки в обычном состоянии?
    // Скрываем текст, если высота меньше 25 пикселей (типично для 15 мин и меньше)
    final bool showContent = isSelected || actualHeight >= 25;

    return AnimatedPositioned(
      duration: const Duration(milliseconds: 250),
      curve: Curves.easeInOut,
      top: top,
      left: isSelected ? -4 : 2, 
      right: isSelected ? -4 : 2,
      height: displayHeight,
      child: GestureDetector(
        onTap: () {
          setState(() {
            _selectedAppointmentId = isSelected ? null : appointment.id;
          });
        },
        child: Container(
          decoration: BoxDecoration(
            boxShadow: isSelected ? [BoxShadow(color: Colors.black.withOpacity(0.3), blurRadius: 10, spreadRadius: 2)] : [],
          ),
          child: ClipRRect(
            borderRadius: BorderRadius.circular(isSelected ? 8 : 4),
            child: Stack(
              fit: StackFit.expand,
              children: [
                Container(color: _getStatusColor(appointment.status)),
                
                // Отображаем контент только если он влезает или карточка развернута
                if (showContent)
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 4.0, vertical: 2.0),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Expanded(
                              child: Text(
                                appointment.clientName, 
                                style: const TextStyle(color: Colors.white, fontSize: 10, fontWeight: FontWeight.bold), 
                                overflow: TextOverflow.ellipsis,
                                maxLines: 1,
                              ),
                            ),
                            if (appointment.reminderSent)
                              const Icon(Icons.notifications_active, color: Colors.white, size: 9),
                          ],
                        ),
                        // Услугу показываем только на достаточно высоких карточках
                        if (displayHeight > 40)
                          Text(
                            appointment.service, 
                            style: const TextStyle(color: Colors.white70, fontSize: 9), 
                            overflow: TextOverflow.ellipsis,
                            maxLines: 1,
                          ),
                      ],
                    ),
                  ),

                // Оверлей с кнопками (только при выборе)
                if (isSelected)
                  Positioned(
                    bottom: 0,
                    left: 0,
                    right: 0,
                    child: Container(
                      height: 50,
                      decoration: BoxDecoration(
                        gradient: LinearGradient(
                          begin: Alignment.topCenter,
                          end: Alignment.bottomCenter,
                          colors: [Colors.black.withOpacity(0.0), Colors.black.withOpacity(0.8)],
                        ),
                      ),
                      padding: const EdgeInsets.only(bottom: 4),
                      child: FittedBox(
                        fit: BoxFit.scaleDown,
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: _buildStatusButtons(appointment),
                        ),
                      ),
                    ),
                  ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  List<Widget> _buildStatusButtons(Appointment appointment) {
    List<Widget> buttons = [];

    Widget statusBtn(IconData icon, Color color, String tooltip, VoidCallback onPressed) {
      return IconButton(
        icon: Icon(icon, color: color, size: 24), 
        onPressed: onPressed,
        tooltip: tooltip,
        padding: const EdgeInsets.symmetric(horizontal: 4),
      );
    }

    if (appointment.status != AppointmentStatus.confirmed) {
      buttons.add(statusBtn(Icons.check_circle, const Color(0xFF26A69A), 'Подтвердить', () => _updateStatus(appointment, AppointmentStatus.confirmed)));
    }
    if (appointment.status != AppointmentStatus.needs_call) {
      buttons.add(statusBtn(Icons.call, const Color(0xFFFFA726), 'Нужно позвонить', () => _updateStatus(appointment, AppointmentStatus.needs_call)));
    }
    if (appointment.status == AppointmentStatus.confirmed) {
      buttons.add(statusBtn(Icons.done_all, Colors.white, 'Завершить', () => _updateStatus(appointment, AppointmentStatus.completed)));
    }
    if (appointment.status != AppointmentStatus.cancelled) {
      buttons.add(statusBtn(Icons.cancel, const Color(0xFFEF5350), 'Отменить', () => _updateStatus(appointment, AppointmentStatus.cancelled)));
    }
    if (appointment.status != AppointmentStatus.scheduled) {
      buttons.add(statusBtn(Icons.restore, Colors.white70, 'В ожидание', () => _updateStatus(appointment, AppointmentStatus.scheduled)));
    }
    buttons.add(statusBtn(Icons.info_outline, Colors.white, 'Детали', () => widget.onAppointmentTap(appointment)));

    return buttons;
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
