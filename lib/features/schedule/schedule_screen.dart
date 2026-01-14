import 'dart:async';
import 'package:flutter/material.dart';
import 'package:try_neuro/core/network/time_service.dart';
import 'package:try_neuro/core/network/websocket_service.dart'; // <<< ИМПОРТ
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
  final WebSocketService _wsService = sl<WebSocketService>(); // <<< СЕРВИС

  late DateTime _selectedDay;
  List<Appointment> _appointmentsForDay = [];
  List<StaffMember> _staff = [];
  bool _isLoading = true;
  
  // Подписка на обновления
  StreamSubscription? _wsSubscription;

  @override
  void initState() {
    super.initState();
    _selectedDay = widget.initialDate ?? _timeService.now();
    _loadData();
    
    // --- ПОДПИСКА НА ОБНОВЛЕНИЯ В РЕАЛЬНОМ ВРЕМЕНИ ---
    _wsSubscription = _wsService.scheduleUpdates.listen((event) {
      if (event == 'refresh' && mounted) {
        print('ScheduleScreen: Automatic refresh triggered by WebSocket');
        _loadData(silent: true); // Обновляем тихо, не показывая лоадер на весь экран
      }
    });
  }

  @override
  void dispose() {
    _wsSubscription?.cancel(); // Обязательно отписываемся
    super.dispose();
  }

  Future<void> _loadData({bool silent = false}) async {
    if (!silent) setState(() => _isLoading = true);
    
    try {
      final results = await Future.wait([
        _managerService.getAppointmentsForDay(_selectedDay),
        _managerService.getStaffForSchedule(), 
      ]);

      if (mounted) {
        setState(() {
          _appointmentsForDay = results[0] as List<Appointment>;
          final allStaff = results[1] as List<StaffMember>;
          _staff = allStaff.where((s) => s.role == 'EMPLOYEE').toList(); 
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isLoading = false);
        // Не показываем ошибки при тихом обновлении, чтобы не пугать пользователя
        if (!silent) {
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Ошибка: ${e.toString()}')));
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
    return Scaffold(
      appBar: AppBar(
        title: const Text('Расписание'),
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
          HorizontalDatePicker(
            initialDate: _selectedDay,
            onDateSelected: (date) {
              setState(() => _selectedDay = date);
              _loadData();
            },
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        heroTag: 'schedule_fab',
        onPressed: () => _navigateToEdit(),
        tooltip: 'Создать запись',
        child: const Icon(Icons.add),
      ),
    );
  }
}
