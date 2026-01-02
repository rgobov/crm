
import 'package:flutter/material.dart';
import 'package:try_neuro/features/auth/domain/user_model.dart';
import 'package:try_neuro/features/calendar/calendar_screen.dart';
import 'package:try_neuro/features/schedule/appointment_detail_screen.dart';
import 'package:try_neuro/features/schedule/data/schedule_service.dart';
import 'package:try_neuro/features/schedule/domain/appointment_model.dart';
import 'package:try_neuro/features/schedule/horizontal_date_picker.dart';
import 'package:try_neuro/service_locator.dart';

class EmployeeHomeScreen extends StatefulWidget {
  final User user;

  const EmployeeHomeScreen({super.key, required this.user});

  @override
  State<EmployeeHomeScreen> createState() => _EmployeeHomeScreenState();
}

class _EmployeeHomeScreenState extends State<EmployeeHomeScreen> {
  int _selectedIndex = 0;

  late final List<Widget> _widgetOptions;

  @override
  void initState() {
    super.initState();
    _widgetOptions = <Widget>[
      MyScheduleTab(user: widget.user), // Создали отдельный виджет для вкладки расписания
      const CalendarScreen(),
    ];
  }

  void _onItemTapped(int index) {
    setState(() {
      _selectedIndex = index;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_selectedIndex == 0 ? 'Моё расписание' : 'Календарь загрузки'),
      ),
      body: IndexedStack(
        index: _selectedIndex,
        children: _widgetOptions,
      ),
      bottomNavigationBar: BottomNavigationBar(
        items: const <BottomNavigationBarItem>[
          BottomNavigationBarItem(icon: Icon(Icons.today), label: 'Сегодня'),
          BottomNavigationBarItem(icon: Icon(Icons.calendar_view_month), label: 'Календарь'),
        ],
        currentIndex: _selectedIndex,
        onTap: _onItemTapped,
      ),
    );
  }
}

// Виджет для вкладки "Мое расписание", чтобы инкапсулировать логику
class MyScheduleTab extends StatefulWidget {
  final User user;
  const MyScheduleTab({super.key, required this.user});

  @override
  State<MyScheduleTab> createState() => _MyScheduleTabState();
}

class _MyScheduleTabState extends State<MyScheduleTab> {
  final ScheduleService _scheduleService = sl<ScheduleService>();

  DateTime _selectedDay = DateTime.now();
  List<Appointment> _allAppointmentsForDay = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadAppointments(_selectedDay);
  }

  Future<void> _loadAppointments(DateTime day) async {
    if (widget.user.staffId == null) return;
    setState(() => _isLoading = true);
    final appointments = await _scheduleService.getAppointmentsForDay(day);
    if (mounted) {
      setState(() {
        _allAppointmentsForDay = appointments;
        _selectedDay = day;
        _isLoading = false;
      });
    }
  }

  void _navigateToDetail(Appointment appointment) async {
    final result = await Navigator.push(context, MaterialPageRoute(builder: (context) => AppointmentDetailScreen(appointment: appointment, appointmentsForDay: _allAppointmentsForDay)));
    if (result == true) {
      _loadAppointments(_selectedDay);
    }
  }

  @override
  Widget build(BuildContext context) {
    final myAppointments = _allAppointmentsForDay.where((a) => a.staffMemberId == widget.user.staffId).toList();

    return Column(
      children: [
        Expanded(
          child: _isLoading
              ? const Center(child: CircularProgressIndicator())
              : myAppointments.isEmpty
                  ? const Center(child: Text('На этот день записей нет'))
                  : ListView.builder(
                      padding: const EdgeInsets.all(16),
                      itemCount: myAppointments.length,
                      itemBuilder: (context, index) {
                        final appointment = myAppointments[index];
                        return Card(
                          margin: const EdgeInsets.only(bottom: 12),
                          child: ListTile(
                            title: Text(appointment.clientName, style: const TextStyle(fontWeight: FontWeight.bold)),
                            subtitle: Text('${appointment.time.format(context)} - ${appointment.service}'),
                            onTap: () => _navigateToDetail(appointment),
                          ),
                        );
                      },
                    ),
        ),
        const Divider(height: 1),
        HorizontalDatePicker(
          initialDate: _selectedDay,
          onDateSelected: _loadAppointments,
        ),
      ],
    );
  }
}
