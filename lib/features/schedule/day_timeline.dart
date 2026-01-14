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

  Color _getStatusColor(AppointmentStatus status, ColorScheme colorScheme) {
    switch (status) {
      case AppointmentStatus.scheduled:
        return colorScheme.primary.withOpacity(0.8); 
      case AppointmentStatus.confirmed:
        return Colors.teal.shade300; 
      case AppointmentStatus.needs_call:
        return Colors.amber.shade400; 
      case AppointmentStatus.completed:
        return colorScheme.outline.withOpacity(0.6); 
      case AppointmentStatus.cancelled:
        return colorScheme.error.withOpacity(0.7); 
    }
  }

  bool get _isToday {
    final now = DateTime.now();
    return widget.day.year == now.year && widget.day.month == now.month && widget.day.day == now.day;
  }

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
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
          Container(
            height: 60,
            decoration: BoxDecoration(
              color: colorScheme.surface,
              border: Border(bottom: BorderSide(color: colorScheme.outlineVariant, width: 0.5)),
            ),
            child: Row(
              children: [
                SizedBox(width: timeColumnWidth, child: Icon(Icons.access_time, size: 18, color: colorScheme.outline)),
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
                                  Text(s.name, style: TextStyle(fontWeight: FontWeight.bold, color: colorScheme.onSurface, fontSize: 13)),
                                  Text(s.specialty, style: TextStyle(fontSize: 11, color: colorScheme.onSurfaceVariant)),
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
          
          Expanded(
            child: SingleChildScrollView(
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  SizedBox(
                    width: timeColumnWidth,
                    height: totalHeight,
                    child: _buildTimeColumn(startHour, endHour, colorScheme),
                  ),
                  Expanded(
                    child: SingleChildScrollView(
                      scrollDirection: Axis.horizontal,
                      child: Row(
                        children: [
                          ...widget.staff.map((s) => _buildStaffColumn(context, s, totalHeight, startHour, endHour, colorScheme)),
                          if (hasUnassigned)
                            _buildStaffColumn(context, null, totalHeight, startHour, endHour, colorScheme),
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

  Widget _buildTimeColumn(int startHour, int endHour, ColorScheme colorScheme) {
    return Column(
      children: List.generate(endHour - startHour, (index) {
        final hour = startHour + index;
        return SizedBox(
          height: hourHeight,
          child: Align(
            alignment: Alignment.topCenter,
            child: Padding(
              padding: const EdgeInsets.only(top: 4.0),
              child: Text(
                '$hour:00', 
                style: TextStyle(fontSize: 11, color: colorScheme.outline, fontWeight: FontWeight.w500)
              ),
            ),
          ),
        );
      }),
    );
  }

  Widget _buildStaffColumn(BuildContext context, StaffMember? staffMember, double totalHeight, int startHour, int endHour, ColorScheme colorScheme) {
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
      decoration: BoxDecoration(border: Border(left: BorderSide(color: colorScheme.outlineVariant, width: 0.5))),
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
                              color: minuteOffset == 0 ? colorScheme.outlineVariant : colorScheme.outlineVariant.withOpacity(0.3), 
                              width: 0.5,
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

          ...sortedAppointments.map((appointment) => _buildAppointmentCard(appointment, startHour, endHour, colorScheme)),
          
          if (_isToday) _buildCurrentTimeIndicator(staffColumnWidth, startHour, endHour, colorScheme),
        ],
      ),
    );
  }

  Widget _buildAppointmentCard(Appointment appointment, int startHour, int endHour, ColorScheme colorScheme) {
    final minutesFromStart = (appointment.time.hour - startHour) * 60 + appointment.time.minute;
    final top = minutesFromStart * (hourHeight / 60);
    final double actualHeight = appointment.durationInMinutes * (hourHeight / 60);
    final isSelected = _selectedAppointmentId == appointment.id;
    
    final double displayHeight = isSelected ? max(actualHeight, 95.0) : actualHeight;
    final bool showContent = isSelected || actualHeight >= 35;

    return AnimatedPositioned(
      duration: const Duration(milliseconds: 250),
      curve: Curves.easeInOut,
      top: top,
      left: isSelected ? -2 : 0, 
      right: isSelected ? -2 : 0,
      height: displayHeight,
      child: GestureDetector(
        onTap: () {
          setState(() {
            _selectedAppointmentId = isSelected ? null : appointment.id;
          });
        },
        child: Container(
          decoration: BoxDecoration(
            boxShadow: isSelected ? [BoxShadow(color: Colors.black.withOpacity(0.2), blurRadius: 12, spreadRadius: 2)] : [],
          ),
          child: ClipRRect(
            borderRadius: BorderRadius.circular(isSelected ? 12 : 0),
            child: Stack(
              fit: StackFit.expand,
              children: [
                // 1. ФОН (самый нижний слой)
                Container(color: _getStatusColor(appointment.status, colorScheme)),
                
                // 2. КОНТЕНТ (только если есть место)
                if (showContent)
                  IgnorePointer( // Делаем текст прозрачным для кликов, чтобы не мешать кнопкам
                    child: Padding(
                      padding: const EdgeInsets.all(6.0),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Expanded(
                                child: FittedBox(
                                  fit: BoxFit.scaleDown,
                                  alignment: Alignment.centerLeft,
                                  child: Text(
                                    appointment.clientName, 
                                    style: const TextStyle(color: Colors.white, fontSize: 11, fontWeight: FontWeight.bold), 
                                  ),
                                ),
                              ),
                              if (appointment.reminderSent == true)
                                const Icon(Icons.notifications_active, color: Colors.white, size: 10),
                            ],
                          ),
                          if (displayHeight > 45)
                            Text(
                              appointment.service, 
                              style: const TextStyle(color: Colors.white70, fontSize: 10), 
                              overflow: TextOverflow.ellipsis,
                              maxLines: 1,
                            ),
                        ],
                      ),
                    ),
                  ),

                // 3. ОВЕРЛЕЙ С КНОПКАМИ (должен быть ВЫШЕ контента для обработки нажатий)
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
                          colors: [Colors.black.withOpacity(0.0), Colors.black.withOpacity(0.75)],
                        ),
                      ),
                      child: Center(
                        child: FittedBox(
                          fit: BoxFit.scaleDown,
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: _buildStatusButtons(appointment),
                          ),
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
      return Material( // Добавляем Material для визуального отклика нажатия
        color: Colors.transparent,
        child: IconButton(
          icon: Icon(icon, color: color, size: 26), // Слегка увеличил размер
          onPressed: onPressed,
          tooltip: tooltip,
          padding: const EdgeInsets.symmetric(horizontal: 8),
        ),
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
      return [Positioned.fill(child: StripedBackground(backgroundColor: Colors.black.withOpacity(0.05)))];
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
          child: StripedBackground(backgroundColor: Colors.black.withOpacity(0.05)),
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
            child: const StripedBackground(backgroundColor: Color(0xFFF5F5F5), stripeColor: Colors.black12),
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
          child: StripedBackground(backgroundColor: Colors.black.withOpacity(0.05)),
        ),
      );
    }

    return backgroundBlocks;
  }

  Widget _buildCurrentTimeIndicator(double width, int startHour, int endHour, ColorScheme colorScheme) {
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
      child: Stack(
        alignment: Alignment.centerLeft,
        children: [
          Container(
            height: 2,
            color: colorScheme.error.withOpacity(0.5),
          ),
          Container(
            width: 6,
            height: 6,
            decoration: BoxDecoration(color: colorScheme.error, shape: BoxShape.circle),
          ),
        ],
      ),
    );
  }
}
