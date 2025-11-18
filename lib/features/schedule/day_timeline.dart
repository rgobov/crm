
import 'dart:async';
import 'package:flutter/material.dart';
import 'package:try_neuro/features/schedule/domain/appointment_model.dart';

// ИЗМЕНЕНО: Теперь это StatefulWidget
class DayTimeline extends StatefulWidget {
  final DateTime day;
  final List<Appointment> appointments;
  final Function(Appointment) onAppointmentTap;
  final Function(TimeOfDay) onEmptySlotTap;

  const DayTimeline({
    super.key,
    required this.day,
    required this.appointments,
    required this.onAppointmentTap,
    required this.onEmptySlotTap,
  });

  @override
  State<DayTimeline> createState() => _DayTimelineState();
}

class _DayTimelineState extends State<DayTimeline> {
  Timer? _timer;

  // Настройки внешнего вида
  final double hourHeight = 60.0;
  final int startHour = 8;
  final int endHour = 22;
  final double timeColumnWidth = 50.0;

  @override
  void initState() {
    super.initState();
    // Запускаем таймер, который будет обновлять UI каждую минуту
    _timer = Timer.periodic(const Duration(minutes: 1), (timer) {
      // Вызываем пустой setState, чтобы спровоцировать перерисовку
      if (mounted) {
        setState(() {});
      }
    });
  }

  @override
  void dispose() {
    _timer?.cancel(); // Обязательно отменяем таймер
    super.dispose();
  }

  bool get _isToday {
    final now = DateTime.now();
    return widget.day.year == now.year && widget.day.month == now.month && widget.day.day == now.day;
  }

  @override
  Widget build(BuildContext context) {
    final totalHours = endHour - startHour;
    final totalHeight = totalHours * hourHeight;

    return SingleChildScrollView(
      child: Stack(
        children: [
          _buildTimeSlots(totalHeight),
          ..._buildAppointmentLayout(context),
          if (_isToday) _buildCurrentTimeIndicator(),
        ],
      ),
    );
  }

  Widget _buildCurrentTimeIndicator() {
    final now = DateTime.now();
    if (now.hour < startHour || now.hour >= endHour) {
      return const SizedBox.shrink();
    }
    final minutesFromStart = (now.hour - startHour) * 60 + now.minute;
    final top = minutesFromStart * (hourHeight / 60);

    // Используем Stack, чтобы добавить кружок в начале линии
    return Positioned(
      top: top - 6, // Смещаем, чтобы кружок был по центру линии
      left: timeColumnWidth - 6, // Смещаем, чтобы кружок был на границе
      right: 0,
      child: SizedBox(
        height: 12,
        child: Row(
          children: [
            Container(
              width: 12,
              height: 12,
              decoration: BoxDecoration(
                color: Colors.red,
                shape: BoxShape.circle,
              ),
            ),
            Expanded(
              child: Container(
                height: 2,
                color: Colors.red,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTimeSlots(double totalHeight) {
    return SizedBox(
      height: totalHeight,
      child: ListView.builder(
        physics: const NeverScrollableScrollPhysics(),
        itemCount: endHour - startHour,
        itemBuilder: (context, index) {
          final hour = startHour + index;
          return InkWell(
            onTap: () => widget.onEmptySlotTap(TimeOfDay(hour: hour, minute: 0)),
            child: Container(
              height: hourHeight,
              decoration: BoxDecoration(
                border: Border(top: BorderSide(color: Colors.grey.shade300)),
              ),
              child: Row(
                children: [
                  SizedBox(
                    width: timeColumnWidth,
                    child: Center(child: Text('$hour:00')),
                  ),
                  const VerticalDivider(),
                ],
              ),
            ),
          );
        },
      ),
    );
  }

  List<Widget> _buildAppointmentLayout(BuildContext context) {
    if (widget.appointments.isEmpty) return [];

    final List<List<Appointment>> columns = [];
    final sortedAppointments = List<Appointment>.from(widget.appointments)
      ..sort((a, b) => (a.time.hour * 60 + a.time.minute).compareTo(b.time.hour * 60 + b.time.minute));

    for (final appointment in sortedAppointments) {
      bool placed = false;
      for (final column in columns) {
        final lastInColumn = column.last;
        final lastEnd = lastInColumn.time.hour * 60 + lastInColumn.time.minute + lastInColumn.durationInMinutes;
        final currentStart = appointment.time.hour * 60 + appointment.time.minute;
        if (currentStart >= lastEnd) {
          column.add(appointment);
          placed = true;
          break;
        }
      }
      if (!placed) {
        columns.add([appointment]);
      }
    }

    final List<Widget> positionedWidgets = [];
    for (int i = 0; i < columns.length; i++) {
      for (final appointment in columns[i]) {
        final minutesFromStart = (appointment.time.hour - startHour) * 60 + appointment.time.minute;
        final top = minutesFromStart * (hourHeight / 60);
        final height = appointment.durationInMinutes * (hourHeight / 60);

        positionedWidgets.add(
          Positioned(
            top: top,
            left: timeColumnWidth + (i * (MediaQuery.of(context).size.width - timeColumnWidth) / columns.length),
            width: (MediaQuery.of(context).size.width - timeColumnWidth) / columns.length - 5, 
            height: height,
            child: GestureDetector(
              onTap: () => widget.onAppointmentTap(appointment),
              child: Card(
                color: Theme.of(context).primaryColor.withOpacity(0.9),
                child: Padding(
                  padding: const EdgeInsets.all(4.0),
                  child: Text(
                    '${appointment.clientName}\n${appointment.service}',
                    style: const TextStyle(color: Colors.white, fontSize: 11),
                    overflow: TextOverflow.ellipsis,
                    maxLines: 4,
                  ),
                ),
              ),
            ),
          ),
        );
      }
    }
    return positionedWidgets;
  }
}
