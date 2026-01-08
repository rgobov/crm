import 'dart:async';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:intl/intl.dart';
import 'package:try_neuro/core/session/session_service.dart';
import 'package:try_neuro/features/auth/domain/user_model.dart';
import 'package:try_neuro/features/contacts/data/contact_service.dart';
import 'package:try_neuro/features/contacts/domain/contact_model.dart';
import 'package:try_neuro/features/manager/data/manager_service.dart';
import 'package:try_neuro/features/resources/data/resource_service.dart';
import 'package:try_neuro/features/resources/domain/resource_model.dart';
import 'package:try_neuro/features/schedule/data/schedule_service.dart';
import 'package:try_neuro/features/schedule/domain/appointment_model.dart';
import 'package:try_neuro/features/services/data/app_service.dart';
import 'package:try_neuro/features/services/domain/service_model.dart';
import 'package:try_neuro/features/staff/data/staff_service.dart';
import 'package:try_neuro/features/staff/domain/staff_member_model.dart';
import 'package:try_neuro/service_locator.dart';

class AppointmentEditScreen extends StatefulWidget {
  final DateTime selectedDate;
  final Appointment? initialAppointment;
  final TimeOfDay? preselectedTime;
  final String? preselectedStaffId;
  final List<Appointment> appointmentsForDay;

  const AppointmentEditScreen({
    super.key,
    required this.selectedDate,
    this.initialAppointment,
    this.preselectedTime,
    this.preselectedStaffId,
    required this.appointmentsForDay,
  });

  @override
  State<AppointmentEditScreen> createState() => _AppointmentEditScreenState();
}

class _AppointmentEditScreenState extends State<AppointmentEditScreen> {
  final _formKey = GlobalKey<FormState>();
  final _scheduleService = sl<ScheduleService>();
  final _contactService = sl<ContactService>();
  final _resourceService = sl<ResourceService>();
  final _staffService = sl<StaffService>();
  final _appService = sl<AppService>();
  final _sessionService = sl<SessionService>();
  final _managerService = sl<ManagerService>();

  final _serviceController = TextEditingController();
  late final TextEditingController _durationController;

  List<Contact> _contacts = [];
  List<Resource> _resources = [];
  List<StaffMember> _staff = [];
  List<Service> _services = [];

  Contact? _selectedContact;
  StaffMember? _selectedStaffMember;
  Resource? _selectedResource;
  TimeOfDay? _selectedTime;
  late DateTime _selectedDate;

  bool _isLoading = true;
  bool _isSaving = false;
  User? _currentUser;
  bool get _isEditing => widget.initialAppointment != null;

  @override
  void initState() {
    super.initState();
    _serviceController.text = widget.initialAppointment?.service ?? '';
    _durationController = TextEditingController(text: widget.initialAppointment?.durationInMinutes.toString() ?? '60');
    _selectedTime = widget.initialAppointment?.time ?? widget.preselectedTime;
    _selectedDate = widget.initialAppointment?.date ?? widget.selectedDate;
    _loadInitialData();
  }

  @override
  void dispose() {
    _serviceController.dispose();
    _durationController.dispose();
    super.dispose();
  }

  Future<void> _loadInitialData() async {
    setState(() => _isLoading = true);
    try {
      _currentUser = await _sessionService.getCurrentUser();
      if (!mounted) return;

      late final List<StaffMember> staffList;
      if (_currentUser?.role == UserRole.manager) {
        staffList = await _managerService.getStaffForSchedule();
      } else {
        staffList = await _staffService.getStaff();
      }

      final otherData = await Future.wait([
        _contactService.getContacts(),
        _resourceService.getResources(),
        _appService.getServices(),
      ]);

      if (!mounted) return;

      _contacts = otherData[0] as List<Contact>;
      _resources = otherData[1] as List<Resource>;
      _staff = staffList;
      _services = otherData[2] as List<Service>;

      if (widget.initialAppointment != null) {
        try {
          _selectedContact = _contacts.firstWhere((c) => c.name == widget.initialAppointment!.clientName);
          if (widget.initialAppointment!.resourceId != null) _selectedResource = _resources.firstWhere((r) => r.id == widget.initialAppointment!.resourceId);
          if (widget.initialAppointment!.staffMemberId != null) _selectedStaffMember = _staff.firstWhere((s) => s.id == widget.initialAppointment!.staffMemberId);
        } catch (e) { /* ignore */ }
      } else if (widget.preselectedStaffId != null) {
        final matchingStaff = _staff.where((s) => s.id == widget.preselectedStaffId).toList();
        if (matchingStaff.isNotEmpty) {
          _selectedStaffMember = matchingStaff.first;
        }
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Ошибка загрузки данных: ${e.toString()}'), backgroundColor: Colors.red));
        Navigator.of(context).pop();
      }
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  Future<void> _saveForm() async {
    if (!_formKey.currentState!.validate()) return;
    if (_selectedTime == null || _selectedContact == null) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Выберите клиента и время записи')));
      return;
    }
    setState(() => _isSaving = true);

    try {
      final newAppointment = Appointment(
        id: widget.initialAppointment?.id ?? 'new',
        date: _selectedDate,
        time: _selectedTime!,
        durationInMinutes: int.tryParse(_durationController.text) ?? 60,
        clientName: _selectedContact!.name,
        service: _serviceController.text,
        resourceId: _selectedResource?.id,
        staffMemberId: _selectedStaffMember?.id,
        status: widget.initialAppointment?.status ?? AppointmentStatus.scheduled,
      );

      if (widget.initialAppointment != null) {
        await _scheduleService.updateAppointment(newAppointment);
      } else {
        if (_currentUser?.role == UserRole.manager) {
          await _managerService.addAppointment(newAppointment);
        } else {
          await _scheduleService.addAppointment(
              date: newAppointment.date,
              time: newAppointment.time,
              durationInMinutes: newAppointment.durationInMinutes,
              clientName: newAppointment.clientName,
              service: newAppointment.service,
              resourceId: newAppointment.resourceId,
              staffMemberId: newAppointment.staffMemberId,
              status: newAppointment.status);
        }
      }
      if (mounted) Navigator.of(context).pop(true);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Ошибка сохранения: ${e.toString()}'), backgroundColor: Colors.red));
      }
    } finally {
      if (mounted) setState(() => _isSaving = false);
    }
  }

  Future<void> _showWheelTimePicker() async {
    int selectedHour = _selectedTime?.hour ?? TimeOfDay.now().hour;
    int selectedMinute = _selectedTime?.minute ?? 0;

    if (selectedMinute % 5 != 0) {
        selectedMinute = (selectedMinute / 5).round() * 5;
        if (selectedMinute == 60) selectedMinute = 55;
    }

    await showDialog<void>(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: const Text('Выберите время'),
          content: SizedBox(
            height: 150,
            width: 200,
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.center,
              children: <Widget>[
                Expanded(
                  child: CupertinoPicker(
                    scrollController: FixedExtentScrollController(initialItem: selectedHour),
                    itemExtent: 32.0,
                    onSelectedItemChanged: (int index) {
                      selectedHour = index;
                    },
                    children: List<Widget>.generate(24, (int index) {
                      return Center(child: Text(index.toString().padLeft(2, '0')));
                    }),
                  ),
                ),
                Expanded(
                  child: CupertinoPicker(
                    scrollController: FixedExtentScrollController(initialItem: selectedMinute ~/ 5),
                    itemExtent: 32.0,
                    onSelectedItemChanged: (int index) {
                      selectedMinute = index * 5;
                    },
                    children: List<Widget>.generate(12, (int index) {
                        return Center(child: Text((index * 5).toString().padLeft(2, '0')));
                    }),
                  ),
                ),
              ],
            ),
          ),
          actions: <Widget>[
            TextButton(onPressed: () => Navigator.of(context).pop(), child: const Text('Отмена')),
            TextButton(
              onPressed: () {
                setState(() {
                  _selectedTime = TimeOfDay(hour: selectedHour, minute: selectedMinute);
                });
                Navigator.of(context).pop();
              },
              child: const Text('Готово', style: TextStyle(fontWeight: FontWeight.bold)),
            ),
          ],
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final bool isStaffSelectionLocked = widget.preselectedStaffId != null;
    return Scaffold(
      appBar: AppBar(title: Text(widget.initialAppointment != null ? 'Изменить запись' : 'Новая запись')),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : Form(
              key: _formKey,
              child: ListView(
                padding: const EdgeInsets.all(16.0),
                children: [
                  DropdownButtonFormField<Contact>(
                    value: _selectedContact,
                    items: _contacts.map((c) => DropdownMenuItem(value: c, child: Text(c.name))).toList(),
                    onChanged: (v) => setState(() => _selectedContact = v),
                    decoration: const InputDecoration(labelText: 'Клиент', border: OutlineInputBorder()),
                    validator: (v) => v == null ? 'Выберите клиента' : null,
                  ),
                  const SizedBox(height: 16),
                  TextFormField(controller: _serviceController, decoration: const InputDecoration(labelText: 'Услуга', border: OutlineInputBorder())),
                  const SizedBox(height: 16),
                  TextFormField(controller: _durationController, decoration: const InputDecoration(labelText: 'Длительность (мин)'), keyboardType: TextInputType.number, inputFormatters: [FilteringTextInputFormatter.digitsOnly]),
                   const SizedBox(height: 16),
                  ListTile(title: Text('Дата: ${DateFormat.yMMMMd('ru').format(_selectedDate)}')),
                  ListTile(
                    leading: const Icon(Icons.access_time),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8), side: BorderSide(color: Colors.grey.shade400)),
                    title: const Text('Время записи'),
                    subtitle: Text(_selectedTime?.format(context) ?? 'Не выбрано'),
                    onTap: _showWheelTimePicker,
                  ),
                  const SizedBox(height: 16),
                  DropdownButtonFormField<StaffMember>(
                    value: _selectedStaffMember,
                    items: _staff.map((s) => DropdownMenuItem(value: s, child: Text(s.name))).toList(),
                    onChanged: isStaffSelectionLocked ? null : (v) => setState(() => _selectedStaffMember = v),
                    decoration: InputDecoration(
                      labelText: 'Сотрудник',
                      border: const OutlineInputBorder(),
                      filled: isStaffSelectionLocked,
                      fillColor: isStaffSelectionLocked ? Colors.grey.shade200 : null,
                    ),
                  ),
                  const SizedBox(height: 16),
                  DropdownButtonFormField<Resource>(
                    value: _selectedResource,
                    items: _resources.map((r) => DropdownMenuItem(value: r, child: Text(r.name))).toList(),
                    onChanged: (v) => setState(() => _selectedResource = v),
                    decoration: const InputDecoration(labelText: 'Ресурс', border: OutlineInputBorder()),
                  ),
                  const SizedBox(height: 24),
                  ElevatedButton(
                    onPressed: _isSaving ? null : _saveForm,
                    child: const Text('Сохранить'),
                  ),
                ],
              ),
            ),
    );
  }
}
