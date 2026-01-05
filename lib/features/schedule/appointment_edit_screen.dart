import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:intl/intl.dart';
import 'package:try_neuro/features/contacts/data/contact_service.dart';
import 'package:try_neuro/features/contacts/domain/contact_model.dart';
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
  final String? preselectedStaffId; // ВОССТАНАВЛИВАЕМ
  final List<Appointment> appointmentsForDay;

  const AppointmentEditScreen({
    super.key, 
    required this.selectedDate, 
    this.initialAppointment,
    this.preselectedTime,
    this.preselectedStaffId, // ВОССТАНАВЛИВАЕМ
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
  late List<Appointment> _appointmentsForSelectedDate;

  bool? _isStaffAvailable, _isResourceAvailable;
  bool _isCheckingAvailability = false;
  Timer? _debounce;
  bool _isLoading = true;
  bool _isSaving = false;
  bool get _isEditing => widget.initialAppointment != null;
  bool get _canSave => !_hasConflict && !_isSaving;
  bool get _hasConflict => _isStaffAvailable == false || _isResourceAvailable == false;

  @override
  void initState() {
    super.initState();
    _serviceController.text = widget.initialAppointment?.service ?? '';
    _durationController = TextEditingController(text: widget.initialAppointment?.durationInMinutes.toString() ?? '60');
    _selectedTime = widget.initialAppointment?.time ?? widget.preselectedTime;
    _selectedDate = widget.initialAppointment?.date ?? widget.selectedDate;
    _appointmentsForSelectedDate = widget.appointmentsForDay;
    
    _loadInitialData().then((_) {
      if (_isEditing || widget.preselectedTime != null) {
        _checkAvailability();
      }
    });
  }

  @override
  void dispose() {
    _debounce?.cancel();
    _serviceController.dispose();
    _durationController.dispose();
    super.dispose();
  }

  Future<void> _loadInitialData() async {
    setState(() => _isLoading = true);
    final data = await Future.wait([
      _contactService.getContacts(), 
      _resourceService.getResources(), 
      _staffService.getStaff(), 
      _appService.getServices()
    ]);
    if (!mounted) return;
    _contacts = data[0] as List<Contact>;
    _resources = data[1] as List<Resource>;
    _staff = data[2] as List<StaffMember>;
    _services = data[3] as List<Service>;
    if (_isEditing) {
      try {
        _selectedContact = _contacts.firstWhere((c) => c.name == widget.initialAppointment!.clientName);
        if (widget.initialAppointment!.resourceId != null) _selectedResource = _resources.firstWhere((r) => r.id == widget.initialAppointment!.resourceId);
        if (widget.initialAppointment!.staffMemberId != null) _selectedStaffMember = _staff.firstWhere((s) => s.id == widget.initialAppointment!.staffMemberId);
      } catch (e) { /* ignore */ }
    } else if (widget.preselectedStaffId != null) { // ВОССТАНАВЛИВАЕМ ЛОГИКУ
      try {
        _selectedStaffMember = _staff.firstWhere((s) => s.id == widget.preselectedStaffId);
      } catch (e) { /* ignore */ }
    }
    setState(() => _isLoading = false);
  }
  
  void _checkAvailability() { /* ... */ }
  Future<void> _showAvailableTimePicker() async { /* ... */ }
  Future<void> _showQuickAddClientDialog() async { /* ... */ }
  
  Future<void> _saveForm() async {
    if (!_formKey.currentState!.validate()) return;
    if (_selectedTime == null) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Выберите время записи')));
      return;
    }
    setState(() => _isSaving = true);
    
    final serviceName = _serviceController.text;
    final duration = int.parse(_durationController.text);

    if ((_services.where((s) => s.name.toLowerCase() == serviceName.toLowerCase())).isEmpty) {
      await _appService.addService(name: serviceName, durationInMinutes: duration);
    }

    if (_isEditing) {
      final updatedAppointment = Appointment(id: widget.initialAppointment!.id, date: _selectedDate, time: _selectedTime!, durationInMinutes: duration, clientName: _selectedContact!.name, service: serviceName, resourceId: _selectedResource?.id, staffMemberId: _selectedStaffMember?.id);
      await _scheduleService.updateAppointment(updatedAppointment);
    } else {
      await _scheduleService.addAppointment(date: _selectedDate, time: _selectedTime!, durationInMinutes: duration, clientName: _selectedContact!.name, service: serviceName, resourceId: _selectedResource?.id, staffMemberId: _selectedStaffMember?.id);
    }
    
    if (mounted) Navigator.of(context).pop(true);
  }

  @override
  Widget build(BuildContext context) {
    final bool isStaffSelectionLocked = widget.preselectedStaffId != null;

    return Scaffold(
      appBar: AppBar(title: Text(_isEditing ? 'Изменить запись' : 'Новая запись')),
      body: _isLoading ? const Center(child: CircularProgressIndicator()) : Form(
        key: _formKey,
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Row(crossAxisAlignment: CrossAxisAlignment.start, children: [Expanded(child: DropdownButtonFormField<Contact>(value: _selectedContact, items: _contacts.map((c) => DropdownMenuItem(value: c, child: Text(c.name))).toList(), onChanged: (v) => setState(() => _selectedContact = v), decoration: const InputDecoration(labelText: 'Клиент', border: OutlineInputBorder(), prefixIcon: Icon(Icons.person)), validator: (v) => v == null ? 'Выберите клиента' : null)), IconButton(icon: const Icon(Icons.add_circle_outline), onPressed: _showQuickAddClientDialog, tooltip: 'Быстро добавить клиента')]),
              const SizedBox(height: 16),
              Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Expanded(
                  child: Autocomplete<Service>(
                    optionsBuilder: (TextEditingValue textEditingValue) => textEditingValue.text == '' ? const Iterable<Service>.empty() : _services.where((s) => s.name.toLowerCase().contains(textEditingValue.text.toLowerCase())),
                    displayStringForOption: (Service option) => option.name,
                    fieldViewBuilder: (BuildContext context, TextEditingController fc, FocusNode fn, VoidCallback onFieldSubmitted) => TextFormField(controller: fc, focusNode: fn, decoration: const InputDecoration(labelText: 'Услуга', border: OutlineInputBorder(), prefixIcon: Icon(Icons.cut)), validator: (v) => (v == null || v.trim().isEmpty) ? 'Введите услугу' : null, onChanged: (t) => _serviceController.text = t),
                    onSelected: (Service s) { setState(() { _serviceController.text = s.name; _durationController.text = s.durationInMinutes.toString(); }); _checkAvailability(); },
                  ),
                ),
                const SizedBox(width: 8),
                SizedBox(width: 120, child: TextFormField(controller: _durationController, decoration: const InputDecoration(labelText: 'Длит. (мин)', border: OutlineInputBorder()), keyboardType: TextInputType.number, inputFormatters: [FilteringTextInputFormatter.digitsOnly], onChanged: (_) => _checkAvailability(), validator: (v) => (v == null || v.isEmpty || (int.tryParse(v) ?? 0) <= 0) ? '! ' : null))
              ]),
              const SizedBox(height: 16),
              ListTile(shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8), side: BorderSide(color: Colors.grey.shade400)), leading: const Icon(Icons.calendar_today), title: const Text('Дата записи'), subtitle: Text(DateFormat.yMMMMd('ru').format(_selectedDate)), onTap: () async { /*...*/ }),
              const SizedBox(height: 16),
              ListTile(shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8), side: BorderSide(color: Colors.grey.shade400)), leading: const Icon(Icons.access_time), title: const Text('Время записи'), subtitle: Text(_selectedTime?.format(context) ?? 'Не выбрано'), onTap: _showAvailableTimePicker),
              const SizedBox(height: 16),
              DropdownButtonFormField<StaffMember>(
                value: _selectedStaffMember,
                items: _staff.map((s) => DropdownMenuItem(value: s, child: Text(s.name))).toList(),
                onChanged: isStaffSelectionLocked ? null : (v) { 
                  setState(() => _selectedStaffMember = v);
                  _checkAvailability();
                },
                decoration: InputDecoration(
                  labelText: 'Сотрудник',
                  border: const OutlineInputBorder(),
                  prefixIcon: const Icon(Icons.badge),
                  filled: isStaffSelectionLocked,
                  fillColor: isStaffSelectionLocked ? Colors.grey.shade200 : null,
                  suffixIcon: _buildAvailabilityIcon(_isStaffAvailable)
                )
              ),
              const SizedBox(height: 16),
              DropdownButtonFormField<Resource>(value: _selectedResource, items: _resources.map((r) => DropdownMenuItem(value: r, child: Text(r.name))).toList(), onChanged: (v) { setState(() => _selectedResource = v); _checkAvailability(); }, decoration: InputDecoration(labelText: 'Ресурс', border: const OutlineInputBorder(), prefixIcon: const Icon(Icons.build), suffixIcon: _buildAvailabilityIcon(_isResourceAvailable)))
            ],
          )
        )
      )
    );
  }

  Widget? _buildAvailabilityIcon(bool? isAvailable) {
    if (isAvailable == null) return null;
    return Padding(padding: const EdgeInsets.all(8.0), child: Icon(isAvailable ? Icons.check_circle : Icons.cancel, color: isAvailable ? Colors.green : Colors.red));
  }
}
