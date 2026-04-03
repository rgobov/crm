import 'dart:async';
import 'package:flutter/material.dart';
import 'package:try_neuro/core/network/time_service.dart';
import 'package:try_neuro/core/network/websocket_service.dart';
import 'package:try_neuro/features/auth/domain/user_model.dart';
import 'package:try_neuro/features/schedule/appointment_detail_screen.dart';
import 'package:try_neuro/features/schedule/appointment_edit_screen.dart';
import 'package:try_neuro/features/schedule/day_timeline.dart';
import 'package:try_neuro/features/schedule/domain/appointment_model.dart';
import 'package:try_neuro/features/schedule/horizontal_date_picker.dart';
import 'package:try_neuro/features/staff/data/employee_service.dart';
import 'package:try_neuro/features/staff/domain/staff_member_model.dart';
import 'package:try_neuro/service_locator.dart';

class EmployeeScheduleScreen extends StatefulWidget {
  final User user;
  final DateTime? initialDate; // Новое поле для установки даты извне

  const EmployeeScheduleScreen({super.key, required this.user, this.initialDate});

  @override
  State<EmployeeScheduleScreen> createState() => _EmployeeScheduleScreenState();
}

class _EmployeeScheduleScreenState extends State<EmployeeScheduleScreen> {
  final EmployeeService _employeeService = sl<EmployeeService>();
  final TimeService _timeService = sl<TimeService>();
  final WebSocketService _wsService = sl<WebSocketService>();

  late DateTime _selectedDay;
  List<Appointment> _appointmentsForDay = [];
  List<StaffMember> _selfAsList = [];
  bool _isLoading = true;
  StreamSubscription? _wsSubscription;

  @override
  void initState() {
    super.initState();
    _selectedDay = widget.initialDate ?? _timeService.now();
    _loadData();

    _wsSubscription = _wsService.scheduleUpdates.listen((event) {
      if (event == 'refresh' && mounted) {
        _loadData(silent: true);
      }
    });
  }

  // --- ВАЖНО: Следим за изменением даты извне (через Navigator/Tabs) ---
  @override
  void didUpdateWidget(EmployeeScheduleScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.initialDate != null && widget.initialDate != oldWidget.initialDate) {
      setState(() => _selectedDay = widget.initialDate!);
      _loadData();
    }
  }

  @override
  void dispose() {
    _wsSubscription?.cancel();
    super.dispose();
  }

  Future<void> _loadData({bool silent = false}) async {
    if (!mounted) return;
    if (!silent) setState(() => _isLoading = true);

    try {
      final results = await Future.wait([
        _employeeService.getMyAppointmentsForDay(_selectedDay),
        _employeeService.getMyProfile(date: _selectedDay),
      ]);

      if (mounted) {
        setState(() {
          _appointmentsForDay = results[0] as List<Appointment>;
          _selfAsList = [results[1] as StaffMember];
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isLoading = false);
        if (!silent) {
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Ошибка загрузки: $e')));
        }
      }
    }
  }

  void _onAppointmentUpdated(Appointment updatedAppointment) {
    setState(() {
      final index = _appointmentsForDay.indexWhere((a) => a.id == updatedAppointment.id);
      if (index != -1) {
        _appointmentsForDay[index] = updatedAppointment;
      }
    });
  }

  void _onEmptySlotTap(TimeOfDay time, String? staffId) {
    _navigateToEdit(preselectedTime: time, preselectedStaffId: staffId);
  }

  void _navigateToEdit({Appointment? appointment, TimeOfDay? preselectedTime, String? preselectedStaffId}) async {
    final result = await Navigator.push<bool>(
      context,
      MaterialPageRoute(
        builder: (context) => AppointmentEditScreen(
          selectedDate: _selectedDay,
          initialAppointment: appointment,
          preselectedTime: preselectedTime,
          preselectedStaffId: preselectedStaffId,
          appointmentsForDay: _appointmentsForDay,
        ),
      ),
    );
    if (result == true) {
      _loadData();
    }
  }

  void _navigateToDetail(Appointment appointment) async {
    final result = await Navigator.push<bool>(
      context,
      MaterialPageRoute(
        builder: (context) => AppointmentDetailScreen(
          appointment: appointment,
          appointmentsForDay: _appointmentsForDay,
          staff: _selfAsList,
        ),
      ),
    );
    if (result == true) {
      _loadData();
    }
  }

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Мое расписание'),
        centerTitle: true,
      ),
      body: Column(
        children: [
          Expanded(
            child: _isLoading && _appointmentsForDay.isEmpty
                ? const Center(child: CircularProgressIndicator())
                : DayTimeline(
                    day: _selectedDay,
                    appointments: _appointmentsForDay,
                    staff: _selfAsList,
                    onAppointmentTap: _navigateToDetail,
                    onEmptySlotTap: _onEmptySlotTap,
                    onAppointmentUpdated: _onAppointmentUpdated,
                  ),
          ),
          const Divider(height: 1),
          Container(
            padding: const EdgeInsets.symmetric(vertical: 8),
            color: colorScheme.surfaceVariant.withOpacity(0.2),
            child: HorizontalDatePicker(
              initialDate: _selectedDay,
              onDateSelected: (date) {
                setState(() => _selectedDay = date);
                _loadData();
              },
            ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        heroTag: 'employee_schedule_fab',
        onPressed: () => _navigateToEdit(preselectedStaffId: widget.user.staffId),
        child: const Icon(Icons.add),
      ),
    );
  }
}
