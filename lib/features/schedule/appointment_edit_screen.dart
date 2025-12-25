
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
  final List<Appointment> appointmentsForDay;

  const AppointmentEditScreen({
    super.key, 
    required this.selectedDate, 
    this.initialAppointment,
    this.preselectedTime,
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
    }
    setState(() => _isLoading = false);
  }
  
  void _checkAvailability() {
    if (_debounce?.isActive ?? false) _debounce!.cancel();
    _debounce = Timer(const Duration(milliseconds: 500), () async {
      if (!mounted || _selectedTime == null) return;
      final duration = int.tryParse(_durationController.text);
      if (duration == null || duration <= 0) return;
      setState(() => _isCheckingAvailability = true);

      final date = _selectedDate;
      
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

  // УЛУЧШЕННЫЙ МЕТОД ВЫБОРА ВРЕМЕНИ
  Future<void> _showAvailableTimePicker() async {
    final int duration = int.tryParse(_durationController.text) ?? 60;
    if (duration <= 0) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Укажите длительность услуги')));
      return;
    }

    final List<TimeOfDay> availableSlots = [];
    final int startHour = _selectedStaffMember?.workStartTime?.hour ?? 8;
    final int endHour = _selectedStaffMember?.workEndTime?.hour ?? 22;
    const int stepInMinutes = 5;

    final conflictingAppointments = _appointmentsForSelectedDate.where((appt) {
      if (_isEditing && appt.id == widget.initialAppointment!.id) return false;
      final isStaffConflict = _selectedStaffMember != null && appt.staffMemberId == _selectedStaffMember!.id;
      final isResourceConflict = _selectedResource != null && appt.resourceId == _selectedResource!.id;
      return isStaffConflict || isResourceConflict;
    }).toList();

    for (int h = startHour; h < endHour; h++) {
      for (int m = 0; m < 60; m += stepInMinutes) {
        final slot = TimeOfDay(hour: h, minute: m);
        bool isSlotFree = true;
        final slotStartInMinutes = slot.hour * 60 + slot.minute;
        final slotEndInMinutes = slotStartInMinutes + duration;

        // Проверка на выход за рабочие часы
        if (_selectedStaffMember?.workEndTime != null) {
            final workEndInMinutes = _selectedStaffMember!.workEndTime!.hour * 60 + _selectedStaffMember!.workEndTime!.minute;
            if (slotEndInMinutes > workEndInMinutes) {
                isSlotFree = false;
            }
        }

        // Проверка на перерыв
        if (isSlotFree && _selectedStaffMember?.breakStartTime != null && _selectedStaffMember?.breakEndTime != null) {
             final breakStartInMinutes = _selectedStaffMember!.breakStartTime!.hour * 60 + _selectedStaffMember!.breakStartTime!.minute;
             final breakEndInMinutes = _selectedStaffMember!.breakEndTime!.hour * 60 + _selectedStaffMember!.breakEndTime!.minute;
             if (slotStartInMinutes < breakEndInMinutes && slotEndInMinutes > breakStartInMinutes) {
                 isSlotFree = false;
             }
        }

        if (isSlotFree) {
            for (final appointment in conflictingAppointments) {
                final appointmentStartInMinutes = appointment.time.hour * 60 + appointment.time.minute;
                final appointmentEndInMinutes = appointmentStartInMinutes + appointment.durationInMinutes;

                if (slotStartInMinutes < appointmentEndInMinutes && slotEndInMinutes > appointmentStartInMinutes) {
                    isSlotFree = false;
                    break;
                }
            }
        }

        if (isSlotFree) {
          availableSlots.add(slot);
        }
      }
    }

    final selected = await showDialog<TimeOfDay>(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: const Text('Выберите доступное время'),
          contentPadding: const EdgeInsets.all(8),
          content: SizedBox(
            width: double.maxFinite,
            child: availableSlots.isEmpty
                ? const Center(child: Text('Нет доступных слотов.'))
                : GridView.builder(
                    padding: const EdgeInsets.all(16),
                    gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 4, childAspectRatio: 2.5, crossAxisSpacing: 8, mainAxisSpacing: 8),
                    itemCount: availableSlots.length,
                    itemBuilder: (context, index) {
                      final time = availableSlots[index];
                      return ActionChip(label: Text(time.format(context)), onPressed: () => Navigator.of(context).pop(time));
                    },
                  ),
          ),
          actions: [TextButton(onPressed: () => Navigator.of(context).pop(), child: const Text('Закрыть'))],
        );
      },
    );

    if (selected != null) {
      setState(() => _selectedTime = selected);
      _checkAvailability();
    }
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
          content: Form(key: formKey, child: SingleChildScrollView(child: Column(mainAxisSize: MainAxisSize.min, children: [TextFormField(controller: nameController, decoration: const InputDecoration(labelText: 'Имя'), validator: (v) => (v == null || v.isEmpty) ? 'Введите имя' : null),TextFormField(controller: phoneController, decoration: const InputDecoration(labelText: 'Телефон'), validator: (v) => (v == null || v.isEmpty) ? 'Введите телефон' : null)]))),
          actions: [TextButton(onPressed: () => Navigator.of(context).pop(), child: const Text('Отмена')), ElevatedButton(onPressed: () async {if (formKey.currentState!.validate()) {final tempContact = Contact(id: 'temp', name: nameController.text, phone: phoneController.text); await _contactService.addContact(name: nameController.text, phone: phoneController.text); Navigator.of(context).pop(tempContact);}}, child: const Text('Сохранить'))],
        );
      },
    );

    if (newContact != null) {
      await _loadInitialData();
      try {
          final fullNewContact = _contacts.firstWhere((c) => c.name == newContact.name, orElse: () => _contacts.last);
          setState(() => _selectedContact = fullNewContact);
      } catch (e) {}
    }
  }
  
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
    return Scaffold(
      appBar: AppBar(title: Text(_isEditing ? 'Изменить запись' : 'Новая запись'), actions: [if (_isCheckingAvailability || _isSaving) const Padding(padding: EdgeInsets.only(right: 16), child: Center(child: SizedBox(width: 24, height: 24, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 3)))) else IconButton(icon: const Icon(Icons.save), onPressed: _canSave ? _saveForm : null, tooltip: 'Сохранить')]),
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
              ListTile(
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8), side: BorderSide(color: Colors.grey.shade400)), 
                leading: const Icon(Icons.calendar_today),
                title: const Text('Дата записи'),
                subtitle: Text(DateFormat.yMMMMd('ru').format(_selectedDate)),
                onTap: () async {
                  final newDate = await showDatePicker(context: context, initialDate: _selectedDate, firstDate: DateTime.now().subtract(const Duration(days: 365)), lastDate: DateTime.now().add(const Duration(days: 365)));
                  if (newDate != null && newDate != _selectedDate) {
                    final appointments = await _scheduleService.getAppointmentsForDay(newDate);
                    if(mounted) { setState(() { _selectedDate = newDate; _appointmentsForSelectedDate = appointments; }); _checkAvailability(); }
                  }
                },
              ),
              const SizedBox(height: 16),
              ListTile(
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8), side: BorderSide(color: Colors.grey.shade400)), 
                leading: const Icon(Icons.access_time), 
                title: const Text('Время записи'), 
                subtitle: Text(_selectedTime?.format(context) ?? 'Не выбрано'), 
                onTap: _showAvailableTimePicker,
              ),
              const SizedBox(height: 16),
              DropdownButtonFormField<StaffMember>(value: _selectedStaffMember, items: _staff.map((s) => DropdownMenuItem(value: s, child: Text(s.name))).toList(), onChanged: (v) { setState(() => _selectedStaffMember = v); _checkAvailability(); }, decoration: InputDecoration(labelText: 'Сотрудник', border: const OutlineInputBorder(), prefixIcon: const Icon(Icons.badge), suffixIcon: _buildAvailabilityIcon(_isStaffAvailable))),
              const SizedBox(height: 16),
              DropdownButtonFormField<Resource>(value: _selectedResource, items: _resources.map((r) => DropdownMenuItem(value: r, child: Text(r.name))).toList(), onChanged: (v) { setState(() => _selectedResource = v); _checkAvailability(); }, decoration: InputDecoration(labelText: 'Ресурс', border: const OutlineInputBorder(), prefixIcon: const Icon(Icons.build), suffixIcon: _buildAvailabilityIcon(_isResourceAvailable)))
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
