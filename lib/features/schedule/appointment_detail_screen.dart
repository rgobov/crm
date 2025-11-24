
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
  final _staffService = sl<StaffService>();

  Future<String?>? _resourceNameFuture;
  Future<String?>? _staffNameFuture;
  
  late Appointment _currentAppointment;
  late TextEditingController _commentController;
  bool _isEditingComment = false;
  bool _hasChanges = false; // Флаг изменений

  @override
  void initState() {
    super.initState();
    _currentAppointment = widget.appointment;
    _commentController = TextEditingController(text: _currentAppointment.comment);

    if (_currentAppointment.resourceId != null) {
      _resourceNameFuture = _getResourceName(_currentAppointment.resourceId!);
    }
    if (_currentAppointment.staffMemberId != null) {
      _staffNameFuture = _getStaffName(_currentAppointment.staffMemberId!);
    }
  }

  @override
  void dispose() {
    _commentController.dispose();
    super.dispose();
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
          selectedDate: _currentAppointment.date,
          initialAppointment: _currentAppointment,
        ),
      ),
    );
    if (result == true && mounted) {
       Navigator.of(context).pop(true);
    }
  }

  void _deleteAppointment() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Удаление'),
        content: const Text('Вы уверены, что хотите удалить запись?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Отмена')),
          TextButton(onPressed: () => Navigator.pop(context, true), child: const Text('Удалить', style: TextStyle(color: Colors.red))),
        ],
      ),
    );

    if (confirmed == true) {
      await _scheduleService.deleteAppointment(_currentAppointment.id);
      if (mounted) Navigator.of(context).pop(true);
    }
  }

  Future<void> _updateStatus(AppointmentStatus newStatus) async {
    if (newStatus == _currentAppointment.status) return;

    final updated = Appointment(
      id: _currentAppointment.id,
      date: _currentAppointment.date,
      time: _currentAppointment.time,
      durationInMinutes: _currentAppointment.durationInMinutes,
      clientName: _currentAppointment.clientName,
      service: _currentAppointment.service,
      resourceId: _currentAppointment.resourceId,
      staffMemberId: _currentAppointment.staffMemberId,
      status: newStatus,
      comment: _currentAppointment.comment,
    );

    await _scheduleService.updateAppointment(updated);
    setState(() {
      _currentAppointment = updated;
      _hasChanges = true; // Отмечаем, что были изменения
    });
  }
  
  Future<void> _saveComment() async {
    final updated = Appointment(
      id: _currentAppointment.id,
      date: _currentAppointment.date,
      time: _currentAppointment.time,
      durationInMinutes: _currentAppointment.durationInMinutes,
      clientName: _currentAppointment.clientName,
      service: _currentAppointment.service,
      resourceId: _currentAppointment.resourceId,
      staffMemberId: _currentAppointment.staffMemberId,
      status: _currentAppointment.status,
      comment: _commentController.text,
    );
    
    await _scheduleService.updateAppointment(updated);
    setState(() {
      _currentAppointment = updated;
      _isEditingComment = false;
      _hasChanges = true; // Отмечаем, что были изменения
    });
    FocusScope.of(context).unfocus();
  }

  @override
  Widget build(BuildContext context) {
    // Перехватываем кнопку "Назад"
    return PopScope(
      canPop: false,
      onPopInvoked: (didPop) {
        if (didPop) return;
        Navigator.of(context).pop(_hasChanges);
      },
      child: Scaffold(
        appBar: AppBar(
          title: const Text('Детали записи'),
          leading: IconButton(
            icon: const Icon(Icons.arrow_back),
            onPressed: () => Navigator.of(context).pop(_hasChanges),
          ),
          actions: [
            IconButton(icon: const Icon(Icons.edit), onPressed: _navigateToEditScreen, tooltip: 'Редактировать'),
            IconButton(icon: const Icon(Icons.delete), onPressed: _deleteAppointment, tooltip: 'Удалить')
          ]
        ),
        body: SingleChildScrollView(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Статус
              Card(
                color: Colors.grey.shade100,
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                  child: DropdownButton<AppointmentStatus>(
                    value: _currentAppointment.status,
                    isExpanded: true,
                    underline: const SizedBox(),
                    items: const [
                      DropdownMenuItem(value: AppointmentStatus.scheduled, child: Text('Запланировано')),
                      DropdownMenuItem(value: AppointmentStatus.completed, child: Text('Выполнено', style: TextStyle(color: Colors.green, fontWeight: FontWeight.bold))),
                      DropdownMenuItem(value: AppointmentStatus.cancelled, child: Text('Отменено', style: TextStyle(color: Colors.red, fontWeight: FontWeight.bold))),
                    ],
                    onChanged: (val) {
                      if (val != null) _updateStatus(val);
                    },
                  ),
                ),
              ),
              const SizedBox(height: 16),
              
              _buildDetailRow(context, Icons.person, 'Клиент', _currentAppointment.clientName),
              _buildDetailRow(context, Icons.cut, 'Услуга', _currentAppointment.service),
              _buildDetailRow(context, Icons.calendar_today, 'Дата', DateFormat.yMMMMd('ru_RU').format(_currentAppointment.date)),
              _buildDetailRow(context, Icons.access_time, 'Время', _currentAppointment.time.format(context)),
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
                
              const Divider(height: 32),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text('Комментарий', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                  IconButton(
                    icon: Icon(_isEditingComment ? Icons.close : Icons.edit_note),
                    onPressed: () {
                      setState(() {
                        if (_isEditingComment) {
                          // Отмена редактирования: возвращаем старый текст
                          _commentController.text = _currentAppointment.comment ?? '';
                        }
                        _isEditingComment = !_isEditingComment;
                      });
                    },
                  )
                ],
              ),
              const SizedBox(height: 8),
              if (_isEditingComment)
                Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    TextField(
                      controller: _commentController,
                      maxLines: 3,
                      decoration: const InputDecoration(
                        border: OutlineInputBorder(),
                        hintText: 'Введите комментарий к записи...',
                      ),
                    ),
                    const SizedBox(height: 8),
                    ElevatedButton.icon(
                      onPressed: _saveComment,
                      icon: const Icon(Icons.check),
                      label: const Text('Сохранить комментарий'),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.green,
                        foregroundColor: Colors.white,
                      ),
                    ),
                  ],
                )
              else
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: Colors.grey.shade50,
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: Colors.grey.shade200),
                  ),
                  child: Text(
                    (_currentAppointment.comment == null || _currentAppointment.comment!.isEmpty) 
                      ? 'Нет комментария' 
                      : _currentAppointment.comment!,
                    style: TextStyle(color: Colors.grey.shade800),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildDetailRow(BuildContext context, IconData icon, String title, String value) {
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
