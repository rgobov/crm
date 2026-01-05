import 'package:flutter/material.dart';
import 'package:try_neuro/core/session/session_service.dart';
import 'package:try_neuro/features/auth/domain/user_model.dart';
import 'package:try_neuro/features/schedule/appointment_detail_screen.dart';
import 'package:try_neuro/features/schedule/appointment_edit_screen.dart';
import 'package:try_neuro/features/schedule/data/schedule_service.dart';
import 'package:try_neuro/features/schedule/day_timeline.dart';
import 'package:try_neuro/features/schedule/domain/appointment_model.dart';
import 'package:try_neuro/features/schedule/horizontal_date_picker.dart';
import 'package:try_neuro/features/staff/data/staff_service.dart';
import 'package:try_neuro/features/staff/domain/staff_member_model.dart';
import 'package:try_neuro/service_locator.dart';

class ScheduleScreen extends StatefulWidget {
  const ScheduleScreen({super.key});

  @override
  State<ScheduleScreen> createState() => _ScheduleScreenState();
}

class _ScheduleScreenState extends State<ScheduleScreen> {
  final ScheduleService _scheduleService = sl<ScheduleService>();
  final StaffService _staffService = sl<StaffService>();
  final SessionService _sessionService = sl<SessionService>();

  DateTime _selectedDay = DateTime.now();
  List<Appointment> _appointmentsForDay = [];
  List<StaffMember> _staff = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadData(_selectedDay);
  }

  Future<void> _loadData(DateTime day) async {
    setState(() {
      _isLoading = true;
      _selectedDay = day;
    });
    
    // Запрашиваем всех сотрудников, чтобы получить их роли
    final allStaff = await _staffService.getStaff();
    final appointments = await _scheduleService.getAppointmentsForDay(day);

    if (mounted) {
      setState(() {
        _appointmentsForDay = appointments;
        // --- ИЗМЕНЕНИЕ ЗДЕСЬ: Фильтруем список БЕЗ УСЛОВИЙ ---
        // В этом расписании мы всегда хотим видеть только исполнителей (EMPLOYEE)
        _staff = allStaff.where((s) => s.role == 'EMPLOYEE').toList();
        
        _isLoading = false;
      });
    }
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
      _loadData(_selectedDay);
    }
  }

  void _navigateToDetail(Appointment appointment) async {
    final result = await Navigator.push<bool>(
      context,
      MaterialPageRoute(
        builder: (context) => AppointmentDetailScreen(
          appointment: appointment,
          appointmentsForDay: _appointmentsForDay,
        ),
      ),
    );
    if (result == true) {
      _loadData(_selectedDay);
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
            child: _isLoading
                ? const Center(child: CircularProgressIndicator())
                : DayTimeline(
                    day: _selectedDay,
                    appointments: _appointmentsForDay,
                    staff: _staff,
                    onAppointmentTap: _navigateToDetail,
                    onEmptySlotTap: _onEmptySlotTap,
                  ),
          ),
          const Divider(height: 1),
          HorizontalDatePicker(
            initialDate: _selectedDay,
            onDateSelected: (date) {
              _loadData(date);
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
