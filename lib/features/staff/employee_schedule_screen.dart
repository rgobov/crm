import 'package:flutter/material.dart';
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
  const EmployeeScheduleScreen({super.key, required this.user});

  @override
  State<EmployeeScheduleScreen> createState() => _EmployeeScheduleScreenState();
}

class _EmployeeScheduleScreenState extends State<EmployeeScheduleScreen> {
  final EmployeeService _employeeService = sl<EmployeeService>();

  DateTime _selectedDay = DateTime.now();
  List<Appointment> _appointmentsForDay = [];
  List<StaffMember> _self = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);

    try {
      final results = await Future.wait([
        _employeeService.getMyAppointmentsForDay(_selectedDay),
        _employeeService.getMyProfile(),
      ]);

      if (mounted) {
        setState(() {
          _appointmentsForDay = results[0] as List<Appointment>;
          _self = [results[1] as StaffMember];
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Ошибка загрузки: ${e.toString()}')));
        setState(() => _isLoading = false);
      }
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
          staff: _self,
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
        title: const Text('Мое расписание'),
      ),
      body: Column(
        children: [
          Expanded(
            child: _isLoading
                ? const Center(child: CircularProgressIndicator())
                : DayTimeline(
                    day: _selectedDay,
                    appointments: _appointmentsForDay,
                    staff: _self,
                    onAppointmentTap: _navigateToDetail,
                    onEmptySlotTap: _onEmptySlotTap,
                    onRefresh: _loadData, // <<< ПЕРЕДАЕМ ФУНКЦИЮ
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
        heroTag: 'employee_schedule_fab',
        onPressed: () => _navigateToEdit(preselectedStaffId: widget.user.staffId),
        tooltip: 'Создать запись',
        child: const Icon(Icons.add),
      ),
    );
  }
}
