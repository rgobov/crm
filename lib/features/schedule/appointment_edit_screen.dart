
import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
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

  const AppointmentEditScreen({super.key, required this.selectedDate, this.initialAppointment});

  @override
  State<AppointmentEditScreen> createState() => _AppointmentEditScreenState();
}

class _AppointmentEditScreenState extends State<AppointmentEditScreen> {
  final _formKey = GlobalKey<FormState>();
  // ... сервисы ...
  final _contactService = sl<ContactService>();
  final _scheduleService = sl<ScheduleService>();
  final _resourceService = sl<ResourceService>();
  final _staffService = sl<StaffService>();
  final _appService = sl<AppService>();

  // ... контроллеры ...
  final _serviceController = TextEditingController();
  late final TextEditingController _durationController;
  
  // ... списки данных ...
  List<Contact> _contacts = [];
  List<Resource> _resources = [];
  List<StaffMember> _staff = [];
  List<Service> _services = [];

  // ... выбранные значения ...
  Contact? _selectedContact;
  StaffMember? _selectedStaffMember;
  Resource? _selectedResource;
  TimeOfDay? _selectedTime;

  // ... состояния UI ...
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
    _selectedTime = widget.initialAppointment?.time;
    _loadInitialData().then((_) {
      if (_isEditing) _checkAvailability();
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
    }
    if(mounted) {
      setState(() => _isLoading = false);
    }
  }

  void _checkAvailability() {
    if (_debounce?.isActive ?? false) _debounce!.cancel();
    _debounce = Timer(const Duration(milliseconds: 500), () async {
      if (!mounted || _selectedTime == null) return;
      final duration = int.tryParse(_durationController.text);
      if (duration == null || duration <= 0) return;
      setState(() => _isCheckingAvailability = true);

      final date = _isEditing ? widget.initialAppointment!.date : widget.selectedDate;
      
      if (_selectedStaffMember != null) {
        final available = await _scheduleService.isStaffMemberAvailable(staffMemberId: _selectedStaffMember!.id, date: date, time: _selectedTime!, duration: duration, currentAppointmentId: widget.initialAppointment?.id);
        if (mounted) setState(() => _isStaffAvailable = available);
      } else {
         if (mounted) setState(() => _isStaffAvailable = null);
      }

      if (_selectedResource != null) {
        final available = await _scheduleService.isResourceAvailable(resourceId: _selectedResource!.id, date: date, time: _selectedTime!, duration: duration, currentAppointmentId: widget.initialAppointment?.id);
        if (mounted) setState(() => _isResourceAvailable = available);
      } else {
        if (mounted) setState(() => _isResourceAvailable = null);
      }

      if(mounted) {
        setState(() => _isCheckingAvailability = false);
      }
    });
  }
  
  Future<void> _showQuickAddClientDialog() async {
     final formKey = GlobalKey<FormState>();
    final nameController = TextEditingController();
    final phoneController = TextEditingController();

    final newContact = await showDialog<Contact>(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: const Text('Быстрое добавление клиента'),
          content: Form(
            key: formKey,
            child: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  TextFormField(controller: nameController, decoration: const InputDecoration(labelText: 'Имя'), validator: (v) => (v == null || v.isEmpty) ? 'Введите имя' : null),
                  TextFormField(controller: phoneController, decoration: const InputDecoration(labelText: 'Телефон'), validator: (v) => (v == null || v.isEmpty) ? 'Введите телефон' : null),
                ],
              ),
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.of(context).pop(), child: const Text('Отмена')),
            ElevatedButton(
              onPressed: () async {
                if (formKey.currentState!.validate()) {
                  final tempContact = Contact(id: 'temp', name: nameController.text, phone: phoneController.text);
                  await _contactService.addContact(name: nameController.text, phone: phoneController.text);
                  Navigator.of(context).pop(tempContact);
                }
              },
              child: const Text('Сохранить'),
            ),
          ],
        );
      },
    );

    if (newContact != null) {
      await _loadInitialData();
      final fullNewContact = _contacts.firstWhere((c) => c.name == newContact.name, orElse: () => _contacts.last);
      setState(() => _selectedContact = fullNewContact);
    }
  }

  Future<void> _saveForm() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _isSaving = true);
    
    final serviceName = _serviceController.text;
    final duration = int.parse(_durationController.text);

    final existingService = _services.where((s) => s.name.toLowerCase() == serviceName.toLowerCase());
    if (existingService.isEmpty) {
      await _appService.addService(name: serviceName, durationInMinutes: duration);
    }

    if (_isEditing) {
      final updatedAppointment = Appointment(id: widget.initialAppointment!.id, date: widget.initialAppointment!.date, time: _selectedTime!, durationInMinutes: duration, clientName: _selectedContact!.name, service: serviceName, resourceId: _selectedResource?.id, staffMemberId: _selectedStaffMember?.id);
      await _scheduleService.updateAppointment(updatedAppointment);
    } else {
      await _scheduleService.addAppointment(date: widget.selectedDate, time: _selectedTime!, durationInMinutes: duration, clientName: _selectedContact!.name, service: serviceName, resourceId: _selectedResource?.id, staffMemberId: _selectedStaffMember?.id);
    }
    
    if (mounted) Navigator.of(context).pop(true);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_isEditing ? 'Изменить запись' : 'Новая запись'),
        actions: [
          if (_isCheckingAvailability || _isSaving) const Padding(padding: EdgeInsets.only(right: 16), child: Center(child: SizedBox(width: 24, height: 24, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 3)))) else IconButton(icon: const Icon(Icons.save), onPressed: _canSave ? _saveForm : null, tooltip: 'Сохранить')
        ],
      ),
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
                    optionsBuilder: (TextEditingValue textEditingValue) {
                      if (textEditingValue.text == '') {
                        return const Iterable<Service>.empty();
                      }
                      return _services.where((s) => s.name.toLowerCase().contains(textEditingValue.text.toLowerCase()));
                    },
                    displayStringForOption: (Service option) => option.name,
                    fieldViewBuilder: (BuildContext context, TextEditingController fieldController, FocusNode fieldFocusNode, VoidCallback onFieldSubmitted) {
                      return TextFormField(
                        controller: fieldController,
                        focusNode: fieldFocusNode,
                        decoration: const InputDecoration(labelText: 'Услуга', border: OutlineInputBorder(), prefixIcon: Icon(Icons.cut)),
                        validator: (v) => (v == null || v.trim().isEmpty) ? 'Введите или выберите услугу' : null,
                        onChanged: (text) => _serviceController.text = text,
                      );
                    },
                    onSelected: (Service selection) {
                      setState(() { // ИСПРАВЛЕНИЕ
                        _serviceController.text = selection.name;
                        _durationController.text = selection.durationInMinutes.toString();
                      });
                      _checkAvailability();
                    },
                  ),
                ),
                const SizedBox(width: 8),
                SizedBox(width: 120, child: TextFormField(controller: _durationController, decoration: const InputDecoration(labelText: 'Длительность (мин)', border: OutlineInputBorder()), keyboardType: TextInputType.number, inputFormatters: [FilteringTextInputFormatter.digitsOnly], onChanged: (_) => _checkAvailability(), validator: (v) => (v == null || v.isEmpty || (int.tryParse(v) ?? 0) <= 0) ? '! ' : null))
              ]),
              const SizedBox(height: 16),
              ListTile(shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8), side: BorderSide(color: Colors.grey.shade400)), leading: const Icon(Icons.access_time), title: const Text('Время записи'), subtitle: Text(_selectedTime?.format(context) ?? 'Не выбрано'), onTap: () async { final time = await showTimePicker(context: context, initialTime: _selectedTime ?? TimeOfDay.now()); if (time != null) { setState(() => _selectedTime = time); _checkAvailability(); } }),
              const SizedBox(height: 16),
              DropdownButtonFormField<StaffMember>(value: _selectedStaffMember, items: _staff.map((s) => DropdownMenuItem(value: s, child: Text(s.name))).toList(), onChanged: (v) { setState(() => _selectedStaffMember = v); _checkAvailability(); }, decoration: InputDecoration(labelText: 'Сотрудник (необязательно)', border: const OutlineInputBorder(), prefixIcon: const Icon(Icons.badge), suffixIcon: _buildAvailabilityIcon(_isStaffAvailable))),
              const SizedBox(height: 16),
              DropdownButtonFormField<Resource>(value: _selectedResource, items: _resources.map((r) => DropdownMenuItem(value: r, child: Text(r.name))).toList(), onChanged: (v) { setState(() => _selectedResource = v); _checkAvailability(); }, decoration: InputDecoration(labelText: 'Ресурс (необязательно)', border: const OutlineInputBorder(), prefixIcon: const Icon(Icons.build), suffixIcon: _buildAvailabilityIcon(_isResourceAvailable)))
            ]
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
