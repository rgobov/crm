import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:try_neuro/features/staff/data/employee_service.dart';
import 'package:try_neuro/features/staff/domain/staff_member_model.dart';
import 'package:try_neuro/service_locator.dart';

class MyWorkScheduleScreen extends StatefulWidget {
  const MyWorkScheduleScreen({super.key});

  @override
  State<MyWorkScheduleScreen> createState() => _MyWorkScheduleScreenState();
}

class _MyWorkScheduleScreenState extends State<MyWorkScheduleScreen> {
  final _employeeService = sl<EmployeeService>();
  StaffMember? _profileOnDate;
  DateTime _selectedDate = DateTime.now();
  
  bool _isLoading = true;
  bool _isSaving = false;

  @override
  void initState() {
    super.initState();
    _loadScheduleForDate(_selectedDate);
  }

  Future<void> _loadScheduleForDate(DateTime date) async {
    setState(() => _isLoading = true);
    try {
      final profile = await _employeeService.getMyProfile(date: date);
      setState(() {
        _profileOnDate = profile;
        _isLoading = false;
      });
    } catch (e) {
      if (mounted) {
        setState(() => _isLoading = false);
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Ошибка загрузки: $e')));
      }
    }
  }

  Future<void> _selectDate() async {
    final DateTime? picked = await showDatePicker(
      context: context,
      initialDate: _selectedDate,
      firstDate: DateTime.now().subtract(const Duration(days: 30)),
      lastDate: DateTime.now().add(const Duration(days: 90)),
    );
    if (picked != null && picked != _selectedDate) {
      setState(() => _selectedDate = picked);
      _loadScheduleForDate(picked);
    }
  }

  Future<void> _selectTime(bool isStart, {bool isBreak = false}) async {
    if (_profileOnDate == null) return;

    TimeOfDay initialTime;
    if (isBreak) {
      initialTime = isStart ? (_profileOnDate!.breakStartTime ?? const TimeOfDay(hour: 12, minute: 0)) : (_profileOnDate!.breakEndTime ?? const TimeOfDay(hour: 13, minute: 0));
    } else {
      initialTime = isStart ? (_profileOnDate!.workStartTime ?? const TimeOfDay(hour: 9, minute: 0)) : (_profileOnDate!.workEndTime ?? const TimeOfDay(hour: 18, minute: 0));
    }

    final TimeOfDay? picked = await showTimePicker(
      context: context,
      initialTime: initialTime,
      builder: (context, child) => MediaQuery(
        data: MediaQuery.of(context).copyWith(alwaysUse24HourFormat: true),
        child: child!,
      ),
    );

    if (picked != null) {
      setState(() {
        if (isBreak) {
          if (isStart) _profileOnDate = _profileOnDate!.copyWith(breakStartTime: picked, isDayOff: false);
          else _profileOnDate = _profileOnDate!.copyWith(breakEndTime: picked, isDayOff: false);
        } else {
          if (isStart) _profileOnDate = _profileOnDate!.copyWith(workStartTime: picked, isDayOff: false);
          else _profileOnDate = _profileOnDate!.copyWith(workEndTime: picked, isDayOff: false);
        }
      });
    }
  }

  Future<void> _save() async {
    if (_profileOnDate == null) return;
    setState(() => _isSaving = true);
    try {
      await _employeeService.updateMyShift(
        date: _selectedDate,
        isDayOff: _profileOnDate!.isDayOff,
        workStart: _profileOnDate!.workStartTime,
        workEnd: _profileOnDate!.workEndTime,
        breakStart: _profileOnDate!.breakStartTime,
        breakEnd: _profileOnDate!.breakEndTime,
      );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('График на выбранный день сохранен')));
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Ошибка сохранения: $e')));
      }
    } finally {
      if (mounted) setState(() => _isSaving = false);
    }
  }

  Future<void> _repeatSchedule() async {
    if (_profileOnDate == null) return;

    final confirmed = await showDialog<int>(
      context: context,
      builder: (context) => SimpleDialog(
        title: const Text('Применить этот график на:'),
        children: [
          SimpleDialogOption(onPressed: () => Navigator.pop(context, 7), child: const Text('7 дней вперед')),
          SimpleDialogOption(onPressed: () => Navigator.pop(context, 14), child: const Text('14 дней вперед')),
          SimpleDialogOption(onPressed: () => Navigator.pop(context, 30), child: const Text('30 дней вперед')),
          const Divider(),
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('ОТМЕНА')),
        ],
      ),
    );

    if (confirmed != null && mounted) {
      setState(() => _isSaving = true);
      try {
        await _employeeService.repeatSchedule(
          sourceDate: _selectedDate,
          isDayOff: _profileOnDate!.isDayOff,
          workStart: _profileOnDate!.workStartTime,
          workEnd: _profileOnDate!.workEndTime,
          breakStart: _profileOnDate!.breakStartTime,
          breakEnd: _profileOnDate!.breakEndTime,
          days: confirmed,
        );
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('График успешно скопирован на $confirmed дней')));
        }
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Ошибка копирования: $e')));
        }
      } finally {
        if (mounted) setState(() => _isSaving = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    final dateStr = DateFormat('EEEE, d MMMM', 'ru').format(_selectedDate);

    return Scaffold(
      appBar: AppBar(title: const Text('Мой график'), centerTitle: true),
      body: Column(
        children: [
          InkWell(
            onTap: _selectDate,
            child: Container(
              padding: const EdgeInsets.all(16),
              color: colorScheme.primaryContainer.withOpacity(0.3),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Icon(Icons.calendar_month),
                  const SizedBox(width: 12),
                  Text(dateStr, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                  const Icon(Icons.arrow_drop_down),
                ],
              ),
            ),
          ),

          Expanded(
            child: _isLoading 
              ? const Center(child: CircularProgressIndicator())
              : SingleChildScrollView(
                  padding: const EdgeInsets.all(20),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      SwitchListTile(
                        title: const Text('Выходной день'),
                        subtitle: const Text('В этот день вы не будете доступны для записи'),
                        secondary: const Icon(Icons.event_busy, color: Colors.redAccent),
                        value: _profileOnDate!.isDayOff,
                        onChanged: (v) => setState(() => _profileOnDate = _profileOnDate!.copyWith(isDayOff: v)),
                      ),
                      const Divider(height: 32),

                      if (!_profileOnDate!.isDayOff) ...[
                        _buildSection(
                          title: 'Рабочее время',
                          icon: Icons.access_time,
                          color: colorScheme.primary,
                          children: [
                            _buildTimeTile('Начало работы', _profileOnDate!.workStartTime, () => _selectTime(true)),
                            _buildTimeTile('Конец работы', _profileOnDate!.workEndTime, () => _selectTime(false)),
                          ],
                        ),
                        const SizedBox(height: 24),
                        _buildSection(
                          title: 'Обеденный перерыв',
                          icon: Icons.restaurant,
                          color: Colors.orange,
                          children: [
                            _buildTimeTile('Начало перерыва', _profileOnDate!.breakStartTime, () => _selectTime(true, isBreak: true)),
                            _buildTimeTile('Конец перерыва', _profileOnDate!.breakEndTime, () => _selectTime(false, isBreak: true)),
                          ],
                        ),
                      ],
                      
                      const SizedBox(height: 40),
                      Row(
                        children: [
                          Expanded(
                            child: OutlinedButton.icon(
                              onPressed: _isSaving ? null : _repeatSchedule,
                              icon: const Icon(Icons.copy_all),
                              label: const Text('ПОВТОРИТЬ...'),
                              style: OutlinedButton.styleFrom(padding: const EdgeInsets.symmetric(vertical: 16)),
                            ),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: FilledButton.icon(
                              onPressed: _isSaving ? null : _save,
                              icon: _isSaving 
                                ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white)) 
                                : const Icon(Icons.save),
                              label: const Text('СОХРАНИТЬ'),
                              style: FilledButton.styleFrom(padding: const EdgeInsets.symmetric(vertical: 16)),
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
          ),
        ],
      ),
    );
  }

  Widget _buildSection({required String title, required IconData icon, required Color color, required List<Widget> children}) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(children: [Icon(icon, size: 20, color: color), const SizedBox(width: 8), Text(title, style: TextStyle(fontWeight: FontWeight.bold, color: color))]),
        const SizedBox(height: 12),
        Card(child: Column(children: children)),
      ],
    );
  }

  Widget _buildTimeTile(String title, TimeOfDay? time, VoidCallback onTap) {
    return ListTile(
      title: Text(title),
      trailing: Text(time?.format(context) ?? '--:--', style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
      onTap: onTap,
    );
  }
}
