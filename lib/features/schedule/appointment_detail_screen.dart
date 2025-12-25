
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:try_neuro/features/schedule/appointment_edit_screen.dart';
import 'package:try_neuro/features/schedule/data/schedule_service.dart';
import 'package:try_neuro/features/schedule/domain/appointment_model.dart';
import 'package:try_neuro/service_locator.dart';

class AppointmentDetailScreen extends StatefulWidget {
  final Appointment appointment;
  final List<Appointment> appointmentsForDay; // Добавили список записей

  const AppointmentDetailScreen({
    super.key,
    required this.appointment,
    required this.appointmentsForDay, // Сделали обязательным
  });

  @override
  State<AppointmentDetailScreen> createState() => _AppointmentDetailScreenState();
}

class _AppointmentDetailScreenState extends State<AppointmentDetailScreen> {
  final _scheduleService = sl<ScheduleService>();
  late Appointment _appointment;

  @override
  void initState() {
    super.initState();
    _appointment = widget.appointment;
  }

  void _navigateToEdit() async {
    final result = await Navigator.push<bool>(
      context,
      MaterialPageRoute(
        builder: (context) => AppointmentEditScreen(
          selectedDate: _appointment.date,
          initialAppointment: _appointment,
          appointmentsForDay: widget.appointmentsForDay, // Передаем список дальше
        ),
      ),
    );
    if (result == true && mounted) {
      // Если мы вернулись с сохранения, нужно обновить данные
      // Мы можем либо вернуть обновленную запись, либо просто true и обновить весь список на главном экране
      Navigator.pop(context, true); // Возвращаемся на главный экран и говорим ему обновиться
    }
  }

  Future<void> _deleteAppointment() async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Удалить запись?'),
        content: const Text('Это действие нельзя будет отменить.'),
        actions: [
          TextButton(onPressed: () => Navigator.of(context).pop(false), child: const Text('Отмена')),
          TextButton(onPressed: () => Navigator.of(context).pop(true), child: const Text('Удалить', style: TextStyle(color: Colors.red))),
        ],
      ),
    );
    if (confirm == true) {
      await _scheduleService.deleteAppointment(_appointment.id);
      if (mounted) {
        Navigator.pop(context, true);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final timeFormat = DateFormat.Hm();
    final startTime = timeFormat.format(DateTime(0).add(Duration(hours: _appointment.time.hour, minutes: _appointment.time.minute)));
    final endTime = timeFormat.format(DateTime(0).add(Duration(hours: _appointment.time.hour, minutes: _appointment.time.minute + _appointment.durationInMinutes)));

    return Scaffold(
      appBar: AppBar(
        title: const Text('Детали записи'),
        actions: [
          IconButton(icon: const Icon(Icons.edit), onPressed: _navigateToEdit, tooltip: 'Изменить'),
          IconButton(icon: const Icon(Icons.delete_outline), onPressed: _deleteAppointment, tooltip: 'Удалить'),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _buildDetailRow(context, icon: Icons.person, title: 'Клиент', content: _appointment.clientName),
            _buildDetailRow(context, icon: Icons.cut, title: 'Услуга', content: _appointment.service),
            _buildDetailRow(context, icon: Icons.timer, title: 'Длительность', content: '${_appointment.durationInMinutes} мин.'),
            _buildDetailRow(context, icon: Icons.access_time, title: 'Время', content: '$startTime - $endTime'),
            if (_appointment.staffMemberId != null) // TODO: Загрузить имя сотрудника
              _buildDetailRow(context, icon: Icons.badge, title: 'Сотрудник', content: 'ID: ${_appointment.staffMemberId}'),
            if (_appointment.resourceId != null) // TODO: Загрузить имя ресурса
              _buildDetailRow(context, icon: Icons.build, title: 'Ресурс', content: 'ID: ${_appointment.resourceId}'),
            if (_appointment.comment != null && _appointment.comment!.isNotEmpty)
              _buildDetailRow(context, icon: Icons.comment, title: 'Комментарий', content: _appointment.comment!),
          ],
        ),
      ),
    );
  }

  Widget _buildDetailRow(BuildContext context, {required IconData icon, required String title, required String content}) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: Colors.grey.shade600, size: 20),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: const TextStyle(fontWeight: FontWeight.bold)),
                const SizedBox(height: 4),
                Text(content, style: Theme.of(context).textTheme.bodyLarge),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
