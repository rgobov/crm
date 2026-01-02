import 'package:flutter/material.dart';
import 'package:table_calendar/table_calendar.dart';
import 'package:try_neuro/features/schedule/data/schedule_service.dart';
import 'package:try_neuro/features/schedule/domain/workload_model.dart';
import 'package:try_neuro/service_locator.dart';

class CalendarScreen extends StatefulWidget {
  const CalendarScreen({super.key});

  @override
  State<CalendarScreen> createState() => _CalendarScreenState();
}

class _CalendarScreenState extends State<CalendarScreen> {
  final ScheduleService _scheduleService = sl<ScheduleService>();

  DateTime _focusedDay = DateTime.now();
  DateTime? _selectedDay;
  Map<int, int> _workloadData = {}; // <День, Количество записей>
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _selectedDay = _focusedDay;
    _loadWorkload(_focusedDay);
  }

  void _loadWorkload(DateTime month) {
    setState(() => _isLoading = true);
    _scheduleService.getWorkloadForMonth(month.year, month.month).then((workload) {
      if (mounted) {
        setState(() {
          _workloadData = {for (var item in workload) item.day: item.appointmentCount};
          _isLoading = false;
        });
      }
    }).catchError((error) {
      // Обработка ошибок, если необходимо
      if (mounted) {
        setState(() => _isLoading = false);
      }
    });
  }

  Color _getWorkloadColor(int appointmentCount) {
    if (appointmentCount == 0) return Colors.transparent;
    if (appointmentCount <= 2) return Colors.green.withOpacity(0.3);
    if (appointmentCount <= 5) return Colors.yellow.withOpacity(0.4);
    if (appointmentCount <= 8) return Colors.orange.withOpacity(0.5);
    return Colors.red.withOpacity(0.6);
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
            onDaySelected: (selectedDay, focusedDay) {
              // TODO: При нажатии на день можно переходить на экран расписания этого дня
              setState(() {
                _selectedDay = selectedDay;
                _focusedDay = focusedDay;
              });
            },
            onPageChanged: (focusedDay) {
              _focusedDay = focusedDay;
              _loadWorkload(focusedDay);
            },
            calendarBuilders: CalendarBuilders(
              defaultBuilder: (context, day, focusedDay) {
                final count = _workloadData[day.day] ?? 0;
                if (count > 0 && day.month == focusedDay.month) {
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
          // Можно добавить легенду для цветов
        ],
      ),
    );
  }
}
