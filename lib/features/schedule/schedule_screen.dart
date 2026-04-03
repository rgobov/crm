import 'dart:async';
import 'package:flutter/material.dart';
import 'package:try_neuro/core/network/time_service.dart';
import 'package:try_neuro/core/network/websocket_service.dart';
import 'package:try_neuro/features/auth/data/auth_service.dart';
import 'package:try_neuro/features/auth/login_screen.dart';
import 'package:try_neuro/features/manager/data/manager_service.dart';
import 'package:try_neuro/features/schedule/appointment_detail_screen.dart';
import 'package:try_neuro/features/schedule/appointment_edit_screen.dart';
import 'package:try_neuro/features/schedule/day_timeline.dart';
import 'package:try_neuro/features/schedule/domain/appointment_model.dart';
import 'package:try_neuro/features/schedule/horizontal_date_picker.dart';
import 'package:try_neuro/features/staff/domain/staff_member_model.dart';
import 'package:try_neuro/service_locator.dart';

class ScheduleScreen extends StatefulWidget {
  final DateTime? initialDate;
  const ScheduleScreen({super.key, this.initialDate});

  @override
  State<ScheduleScreen> createState() => _ScheduleScreenState();
}

class _ScheduleScreenState extends State<ScheduleScreen> {
  final ManagerService _managerService = sl<ManagerService>();
  final TimeService _timeService = sl<TimeService>();
  final WebSocketService _wsService = sl<WebSocketService>();
  final AuthService _authService = sl<AuthService>();

  late DateTime _selectedDay;
  List<Appointment> _appointmentsForDay = [];
  List<StaffMember> _staff = [];
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

  @override
  void dispose() {
    _wsSubscription?.cancel();
    super.dispose();
  }

  Future<void> _loadData({bool silent = false}) async {
    if (!silent) setState(() => _isLoading = true);
    
    try {
      final results = await Future.wait([
        _managerService.getAppointmentsForDay(_selectedDay),
        // ПЕРЕДАЕМ ДАТУ: Чтобы получить смены на конкретный день
        _managerService.getStaffForSchedule(_selectedDay), 
      ]);

      if (mounted) {
        setState(() {
          _appointmentsForDay = results[0] as List<Appointment>;
          final allStaff = results[1] as List<StaffMember>;
          // Фильтруем только сотрудников (EMPLOYEE)
          _staff = allStaff.where((s) => s.role == 'EMPLOYEE').toList(); 
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isLoading = false);
        if (!silent) {
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Ошибка: ${e.toString()}')));
        }
      }
    }
  }

  void _handleLogout() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Выход из системы'),
        content: const Text('Вы действительно хотите выйти из своей учетной записи?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('ОТМЕНА'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('ВЫЙТИ', style: TextStyle(color: Colors.red, fontWeight: FontWeight.bold)),
          ),
        ],
      ),
    );

    if (confirmed == true && mounted) {
      await _authService.logout();
      if (mounted) {
        Navigator.of(context).pushAndRemoveUntil(
          MaterialPageRoute(builder: (context) => const LoginScreen()),
          (route) => false,
        );
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
          staff: _staff,
        ),
      ),
    );
    if (result == true) {
      _loadData();
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    return Scaffold(
      backgroundColor: colorScheme.surface,
      appBar: AppBar(
        title: const Text('Расписание'),
        centerTitle: true,
        elevation: 0,
        backgroundColor: Colors.transparent,
        leading: IconButton(
          icon: const Icon(Icons.logout, color: Colors.redAccent),
          onPressed: _handleLogout,
          tooltip: 'Выйти',
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () => _loadData(),
            tooltip: 'Обновить',
          ),
        ],
      ),
      body: Column(
        children: [
          Expanded(
            child: _isLoading && _appointmentsForDay.isEmpty
                ? const Center(child: CircularProgressIndicator())
                : DayTimeline(
                    day: _selectedDay,
                    appointments: _appointmentsForDay,
                    staff: _staff,
                    onAppointmentTap: _navigateToDetail,
                    onEmptySlotTap: _onEmptySlotTap,
                    onAppointmentUpdated: _onAppointmentUpdated,
                  ),
          ),
          const Divider(height: 1),
          Container(
            padding: const EdgeInsets.symmetric(vertical: 12),
            decoration: BoxDecoration(
              color: colorScheme.surfaceVariant.withOpacity(0.3),
            ),
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
      floatingActionButton: FloatingActionButton.extended(
        heroTag: 'schedule_fab',
        onPressed: () => _navigateToEdit(),
        icon: const Icon(Icons.add),
        label: const Text('Записать'),
        backgroundColor: colorScheme.primaryContainer,
        foregroundColor: colorScheme.onPrimaryContainer,
        elevation: 2,
      ),
    );
  }
}
