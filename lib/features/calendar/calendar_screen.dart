import 'package:flutter/material.dart';
import 'package:table_calendar/table_calendar.dart';
import 'package:try_neuro/core/session/session_service.dart';
import 'package:try_neuro/features/auth/domain/user_model.dart';
import 'package:try_neuro/features/manager/data/manager_service.dart';
import 'package:try_neuro/features/schedule/domain/workload_model.dart';
import 'package:try_neuro/features/schedule/schedule_screen.dart';
import 'package:try_neuro/features/staff/data/employee_service.dart';
import 'package:try_neuro/service_locator.dart';

class CalendarScreen extends StatefulWidget {
  const CalendarScreen({super.key});

  @override
  State<CalendarScreen> createState() => _CalendarScreenState();
}

class _CalendarScreenState extends State<CalendarScreen> {
  final SessionService _sessionService = sl<SessionService>();
  final ManagerService _managerService = sl<ManagerService>();
  final EmployeeService _employeeService = sl<EmployeeService>();

  User? _currentUser;
  DateTime _focusedDay = DateTime.now();
  DateTime? _selectedDay;
  Map<int, int> _workloadData = {};
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _selectedDay = _focusedDay;
    _initialize();
  }

  Future<void> _initialize() async {
    _currentUser = await _sessionService.getCurrentUser();
    _loadWorkload(_focusedDay);
  }

  Future<void> _loadWorkload(DateTime month) async {
    setState(() => _isLoading = true);
    try {
      late final List<Workload> workload;
      if (_currentUser?.role == UserRole.employee) {
        workload = await _employeeService.getMyWorkloadForMonth(month.year, month.month);
      } else {
        workload = await _managerService.getWorkloadForMonth(month.year, month.month);
      }
      if (mounted) {
        setState(() {
          _workloadData = {for (var item in workload) item.day: item.appointmentCount};
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Color _getWorkloadColor(int appointmentCount) {
    if (appointmentCount == 0) return Colors.transparent;
    if (appointmentCount <= 2) return Colors.green.withOpacity(0.3);
    if (appointmentCount <= 5) return Colors.yellow.withOpacity(0.4);
    if (appointmentCount <= 8) return Colors.orange.withOpacity(0.5);
    return Colors.red.withOpacity(0.6);
  }

  void _onDaySelected(DateTime selectedDay, DateTime focusedDay) {
    if (!mounted) return;
    Navigator.of(context, rootNavigator: true).push(
      MaterialPageRoute(
        builder: (context) => ScheduleScreen(initialDate: selectedDay),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Календарь загрузки'),
      ),
      body: Column(
        children: [
          TableCalendar(
            locale: 'ru_RU',
            firstDay: DateTime.utc(2020, 1, 1),
            lastDay: DateTime.utc(2030, 12, 31),
            focusedDay: _focusedDay,
            selectedDayPredicate: (day) => isSameDay(_selectedDay, day),
            onDaySelected: _onDaySelected,
            onPageChanged: (focusedDay) {
              _focusedDay = focusedDay;
              _loadWorkload(focusedDay);
            },
            calendarBuilders: CalendarBuilders(
              selectedBuilder: (context, day, focusedDay) {
                final count = _workloadData[day.day] ?? 0;
                return Container(
                  margin: const EdgeInsets.all(4.0),
                  decoration: BoxDecoration(
                    color: _getWorkloadColor(count),
                    shape: BoxShape.circle,
                    border: Border.all(color: Colors.blueAccent, width: 2.0),
                  ),
                  child: Center(child: Text(day.day.toString())),
                );
              },
              todayBuilder: (context, day, focusedDay) {
                final count = _workloadData[day.day] ?? 0;
                return Container(
                  margin: const EdgeInsets.all(4.0),
                  decoration: BoxDecoration(
                    color: _getWorkloadColor(count),
                    shape: BoxShape.circle,
                    border: Border.all(color: Colors.blue.shade200, width: 1.5),
                  ),
                  child: Center(child: Text(day.day.toString())),
                );
              },
              defaultBuilder: (context, day, focusedDay) {
                final count = _workloadData[day.day] ?? 0;
                if (day.month == focusedDay.month) {
                  return Container(
                    margin: const EdgeInsets.all(4.0),
                    decoration: BoxDecoration(
                      color: _getWorkloadColor(count),
                      shape: BoxShape.circle,
                    ),
                    child: Center(child: Text(day.day.toString())),
                  );
                }
                return null;
              },
            ),
          ),
        ],
      ),
    );
  }
}
