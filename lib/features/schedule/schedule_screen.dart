
import 'dart:collection';
import 'package:flutter/material.dart';
import 'package:table_calendar/table_calendar.dart';
import 'package:try_neuro/features/schedule/appointment_detail_screen.dart';
import 'package:try_neuro/features/schedule/appointment_edit_screen.dart';
import 'package:try_neuro/features/schedule/data/schedule_service.dart';
import 'package:try_neuro/features/schedule/domain/appointment_model.dart';
import 'package:try_neuro/service_locator.dart';

class ScheduleScreen extends StatefulWidget {
  const ScheduleScreen({super.key});

  @override
  State<ScheduleScreen> createState() => _ScheduleScreenState();
}

class _ScheduleScreenState extends State<ScheduleScreen> {
  final ScheduleService _scheduleService = sl<ScheduleService>();

  late final ValueNotifier<List<Appointment>> _selectedAppointments;
  
  LinkedHashMap<DateTime, List<Appointment>> _events = LinkedHashMap(
    equals: isSameDay,
    hashCode: (key) => key.day * 1000000 + key.month * 10000 + key.year,
  );

  CalendarFormat _calendarFormat = CalendarFormat.week;
  DateTime _focusedDay = DateTime.now();
  DateTime? _selectedDay;

  @override
  void initState() {
    super.initState();
    _selectedDay = _focusedDay;
    _selectedAppointments = ValueNotifier([]);
    _loadAppointmentsForMonth(_focusedDay);
  }

  @override
  void dispose() {
    _selectedAppointments.dispose();
    super.dispose();
  }

  void _loadAppointmentsForMonth(DateTime month) async {
    final appointments = await _scheduleService.getAppointmentsForMonth(month);
    _events = LinkedHashMap(
      equals: isSameDay,
      hashCode: (key) => key.day * 1000000 + key.month * 10000 + key.year,
    );
    for (var appointment in appointments) {
      final day = DateTime.utc(appointment.date.year, appointment.date.month, appointment.date.day);
      if (_events[day] == null) {
        _events[day] = [];
      }
      _events[day]!.add(appointment);
    }
    if(mounted) {
        setState(() {}); 
        _onDaySelected(_selectedDay!, _focusedDay);
    }
  }

  List<Appointment> _getEventsForDay(DateTime day) {
    return _events[day] ?? [];
  }

  void _onDaySelected(DateTime selectedDay, DateTime focusedDay) {
    if (!isSameDay(_selectedDay, selectedDay)) {
      setState(() {
        _selectedDay = selectedDay;
        _focusedDay = focusedDay;
      });
    }
    _selectedAppointments.value = _getEventsForDay(selectedDay);
  }

  void _onPageChanged(DateTime focusedDay) {
    _focusedDay = focusedDay;
    _loadAppointmentsForMonth(focusedDay);
  }

  void _navigateAndRefresh() async {
    final result = await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => AppointmentEditScreen(
          selectedDate: _selectedDay ?? DateTime.now(),
        ),
      ),
    );
    if (result == true) {
       _loadAppointmentsForMonth(_focusedDay);
    }
  }

  void _navigateToDetailScreen(Appointment appointment) async {
    final result = await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => AppointmentDetailScreen(appointment: appointment),
      ),
    );
    if (result == true) {
      _loadAppointmentsForMonth(_focusedDay);
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
          TableCalendar<Appointment>(
            firstDay: DateTime.utc(2020, 1, 1),
            lastDay: DateTime.utc(2030, 12, 31),
            focusedDay: _focusedDay,
            selectedDayPredicate: (day) => isSameDay(_selectedDay, day),
            calendarFormat: _calendarFormat,
            startingDayOfWeek: StartingDayOfWeek.monday,
            locale: 'ru_RU',
            eventLoader: _getEventsForDay,
            onDaySelected: _onDaySelected,
            onFormatChanged: (format) {
              if (_calendarFormat != format) {
                setState(() {
                  _calendarFormat = format;
                });
              }
            },
            onPageChanged: _onPageChanged,
          ),
          const SizedBox(height: 8.0),
          Expanded(
            child: ValueListenableBuilder<List<Appointment>>(
              valueListenable: _selectedAppointments,
              builder: (context, value, _) {
                return _buildAppointmentList(value);
              },
            ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        heroTag: 'schedule_fab',
        onPressed: _navigateAndRefresh,
        tooltip: 'Создать запись',
        child: const Icon(Icons.add),
      ),
    );
  }

  Widget _buildAppointmentList(List<Appointment> appointments) {
    if (appointments.isEmpty) {
      return const Center(
        child: Text(
          'На выбранный день нет записей',
          style: TextStyle(fontSize: 18, color: Colors.grey),
        ),
      );
    }
    return ListView.builder(
      itemCount: appointments.length,
      itemBuilder: (context, index) {
        final appointment = appointments[index];
        return Card(
          margin: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 4.0),
          child: ListTile(
            leading: CircleAvatar(child: Text('${appointment.durationInMinutes}м')),
            title: Text(appointment.clientName),
            subtitle: Text(appointment.service),
            trailing: Text(appointment.time.format(context)),
            onTap: () => _navigateToDetailScreen(appointment),
          ),
        );
      },
    );
  }
}
