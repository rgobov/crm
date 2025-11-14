
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:try_neuro/features/resources/data/resource_service.dart';
import 'package:try_neuro/features/schedule/appointment_edit_screen.dart';
import 'package:try_neuro/features/schedule/data/schedule_service.dart';
import 'package:try_neuro/features/schedule/domain/appointment_model.dart';
import 'package:try_neuro/features/staff/data/staff_service.dart';
import 'package:try_neuro/service_locator.dart';

class AppointmentDetailScreen extends StatefulWidget {
  final Appointment appointment;

  const AppointmentDetailScreen({super.key, required this.appointment});

  @override
  State<AppointmentDetailScreen> createState() => _AppointmentDetailScreenState();
}

class _AppointmentDetailScreenState extends State<AppointmentDetailScreen> {
  final _scheduleService = sl<ScheduleService>();
  final _resourceService = sl<ResourceService>();
  final _staffService = sl<StaffService>(); // <-- ДОБАВЛЕНО

  Future<String?>? _resourceNameFuture;
  Future<String?>? _staffNameFuture; // <-- ДОБАВЛЕНО

  @override
  void initState() {
    super.initState();
    if (widget.appointment.resourceId != null) {
      _resourceNameFuture = _getResourceName(widget.appointment.resourceId!);
    }
    if (widget.appointment.staffMemberId != null) {
      _staffNameFuture = _getStaffName(widget.appointment.staffMemberId!); // <-- ДОБАВЛЕНО
    }
  }

  Future<String?> _getResourceName(String resourceId) async {
    final resources = await _resourceService.getResources();
    try {
      return resources.firstWhere((r) => r.id == resourceId).name;
    } catch (e) {
      return 'Неизвестный ресурс';
    }
  }

  Future<String?> _getStaffName(String staffId) async {
    final staff = await _staffService.getStaff();
    try {
      return staff.firstWhere((s) => s.id == staffId).name;
    } catch (e) {
      return 'Неизвестный сотрудник';
    }
  }

  void _navigateToEditScreen() async {
    final result = await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => AppointmentEditScreen(
          selectedDate: widget.appointment.date,
          initialAppointment: widget.appointment,
        ),
      ),
    );
    if (result == true && mounted) {
      Navigator.of(context).pop(true);
    }
  }

  void _deleteAppointment() async {
    // ... (код без изменений)
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Детали записи'), actions: [IconButton(icon: const Icon(Icons.edit), onPressed: _navigateToEditScreen, tooltip: 'Редактировать'), IconButton(icon: const Icon(Icons.delete), onPressed: _deleteAppointment, tooltip: 'Удалить')]),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            _buildDetailRow(context, Icons.person, 'Клиент', widget.appointment.clientName),
            _buildDetailRow(context, Icons.cut, 'Услуга', widget.appointment.service),
            _buildDetailRow(context, Icons.calendar_today, 'Дата', DateFormat.yMMMMd('ru_RU').format(widget.appointment.date)),
            _buildDetailRow(context, Icons.access_time, 'Время', widget.appointment.time.format(context)),
            if (_staffNameFuture != null)
              FutureBuilder<String?>(
                future: _staffNameFuture,
                builder: (context, snapshot) {
                  if (snapshot.connectionState == ConnectionState.waiting) {
                    return const Padding(padding: EdgeInsets.symmetric(vertical: 8.0), child: Center(child: CircularProgressIndicator()));
                  }
                  if (snapshot.hasData) {
                    return _buildDetailRow(context, Icons.badge, 'Сотрудник', snapshot.data!);
                  }
                  return const SizedBox.shrink();
                },
              ),
            if (_resourceNameFuture != null)
              FutureBuilder<String?>(
                future: _resourceNameFuture,
                builder: (context, snapshot) {
                  if (snapshot.connectionState == ConnectionState.waiting) {
                    return const Padding(padding: EdgeInsets.symmetric(vertical: 8.0), child: Center(child: CircularProgressIndicator()));
                  }
                  if (snapshot.hasData) {
                    return _buildDetailRow(context, Icons.build, 'Ресурс', snapshot.data!);
                  }
                  return const SizedBox.shrink();
                },
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildDetailRow(BuildContext context, IconData icon, String title, String value) {
    // ... (код без изменений)
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 12.0),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: Theme.of(context).primaryColor, size: 28),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: Theme.of(context).textTheme.bodyMedium?.copyWith(color: Colors.grey.shade700)),
                const SizedBox(height: 2),
                Text(value, style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
