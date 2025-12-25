
import 'package:flutter/material.dart';
import 'package:try_neuro/features/auth/domain/user_model.dart';
import 'package:try_neuro/features/schedule/appointment_detail_screen.dart';
import 'package:try_neuro/features/schedule/data/schedule_service.dart';
import 'package:try_neuro/features/schedule/domain/appointment_model.dart';
import 'package:try_neuro/features/schedule/horizontal_date_picker.dart';
import 'package:try_neuro/service_locator.dart';
import 'package:intl/intl.dart';

class EmployeeHomeScreen extends StatefulWidget {
  final User user;

  const EmployeeHomeScreen({super.key, required this.user});

  @override
  State<EmployeeHomeScreen> createState() => _EmployeeHomeScreenState();
}

class _EmployeeHomeScreenState extends State<EmployeeHomeScreen> {
  final ScheduleService _scheduleService = sl<ScheduleService>();
  
  DateTime _selectedDay = DateTime.now();
  List<Appointment> _allAppointmentsForDay = []; // Теперь храним все записи
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    if (widget.user.staffId == null) {
      // Обработка ошибки: у сотрудника должен быть staffId
    }
    _loadAppointments(_selectedDay);
  }

  Future<void> _loadAppointments(DateTime day) async {
    if (widget.user.staffId == null) return;

    setState(() {
      _isLoading = true;
      _selectedDay = day;
    });

    // Загружаем ВСЕ записи на день
    final appointments = await _scheduleService.getAppointmentsForDay(day);

    if (mounted) {
      setState(() {
        _allAppointmentsForDay = appointments;
        _isLoading = false;
      });
    }
  }
  
  Color _getStatusColor(AppointmentStatus status) {
    switch (status) {
      case AppointmentStatus.scheduled: return Colors.blue.shade100;
      case AppointmentStatus.completed: return Colors.green.shade100;
      case AppointmentStatus.cancelled: return Colors.red.shade100;
    }
  }
  
  Color _getStatusTextColor(AppointmentStatus status) {
     switch (status) {
      case AppointmentStatus.scheduled: return Colors.blue.shade900;
      case AppointmentStatus.completed: return Colors.green.shade900;
      case AppointmentStatus.cancelled: return Colors.red.shade900;
    }
  }

  void _navigateToDetail(Appointment appointment) async {
    final result = await Navigator.push(
      context,
      // Передаем список всех записей
      MaterialPageRoute(builder: (context) => AppointmentDetailScreen(appointment: appointment, appointmentsForDay: _allAppointmentsForDay)),
    );
    if (result == true) {
      _loadAppointments(_selectedDay);
    }
  }

  @override
  Widget build(BuildContext context) {
    // Фильтруем только записи текущего сотрудника для отображения
    final myAppointments = _allAppointmentsForDay.where((a) => a.staffMemberId == widget.user.staffId).toList();

    return Scaffold(
      appBar: AppBar(
        title: const Text('Моё расписание'),
      ),
      body: Column(
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
                        color: _getStatusColor(appointment.status),
                        child: ListTile(
                          contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                          title: Text(
                            appointment.clientName,
                            style: TextStyle(
                              fontWeight: FontWeight.bold,
                              decoration: appointment.status == AppointmentStatus.cancelled ? TextDecoration.lineThrough : null,
                              color: _getStatusTextColor(appointment.status),
                            ),
                          ),
                          subtitle: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text('${appointment.time.format(context)} - ${appointment.service}'),
                              if (appointment.comment != null && appointment.comment!.isNotEmpty)
                                Padding(
                                  padding: const EdgeInsets.only(top: 4),
                                  child: Text(
                                    '💬 ${appointment.comment}',
                                    style: TextStyle(fontStyle: FontStyle.italic, fontSize: 12, color: Colors.black.withOpacity(0.6)),
                                  ),
                                ),
                            ],
                          ),
                          trailing: const Icon(Icons.chevron_right),
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
      ),
    );
  }
}
