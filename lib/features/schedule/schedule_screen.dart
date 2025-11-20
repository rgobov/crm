
import 'package:flutter/material.dart';
import 'package:try_neuro/features/schedule/appointment_detail_screen.dart';
import 'package:try_neuro/features/schedule/appointment_edit_screen.dart';
import 'package:try_neuro/features/schedule/data/schedule_service.dart';
import 'package:try_neuro/features/schedule/day_timeline.dart';
import 'package:try_neuro/features/schedule/domain/appointment_model.dart';
import 'package:try_neuro/features/schedule/horizontal_date_picker.dart';
import 'package:try_neuro/service_locator.dart';

class ScheduleScreen extends StatefulWidget {
  const ScheduleScreen({super.key});

  @override
  State<ScheduleScreen> createState() => _ScheduleScreenState();
}

class _ScheduleScreenState extends State<ScheduleScreen> {
  final ScheduleService _scheduleService = sl<ScheduleService>();

  DateTime _selectedDay = DateTime.now();
  List<Appointment> _appointmentsForDay = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadAppointmentsForDay(_selectedDay);
  }

  Future<void> _loadAppointmentsForDay(DateTime day) async {
    setState(() {
      _isLoading = true;
      _selectedDay = day; // Обновляем выбранный день
    });
    final appointments = await _scheduleService.getAppointmentsForDay(day);
    if (mounted) {
      setState(() {
        _appointmentsForDay = appointments;
        _isLoading = false;
      });
    }
  }

  void _onEmptySlotTap(TimeOfDay time) {
    _navigateToEdit(preselectedTime: time);
  }

  void _navigateToEdit({Appointment? appointment, TimeOfDay? preselectedTime}) async {
    final result = await Navigator.push<bool>(
      context,
      MaterialPageRoute(
        builder: (context) => AppointmentEditScreen(
          selectedDate: _selectedDay,
          initialAppointment: appointment,
          preselectedTime: preselectedTime,
        ),
      ),
    );
    if (result == true) {
      _loadAppointmentsForDay(_selectedDay);
    }
  }

  void _navigateToDetail(Appointment appointment) async {
    final result = await Navigator.push<bool>(
      context,
      MaterialPageRoute(
        builder: (context) => AppointmentDetailScreen(appointment: appointment),
      ),
    );
    if (result == true) {
      _loadAppointmentsForDay(_selectedDay);
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
                    onAppointmentTap: _navigateToDetail,
                    onEmptySlotTap: _onEmptySlotTap,
                  ),
          ),
          // Новый переключатель дат внизу
          const Divider(height: 1),
          HorizontalDatePicker(
            initialDate: _selectedDay,
            onDateSelected: (date) {
              _loadAppointmentsForDay(date);
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
