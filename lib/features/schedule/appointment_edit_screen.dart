import 'dart:async';
import 'package:dio/dio.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:intl/intl.dart';
import 'package:try_neuro/core/session/session_service.dart';
import 'package:try_neuro/core/utils/phone_utils.dart';
import 'package:try_neuro/features/auth/domain/user_model.dart';
import 'package:try_neuro/features/contacts/add_contact_screen.dart'; 
import 'package:try_neuro/features/contacts/data/contact_service.dart';
import 'package:try_neuro/features/contacts/domain/contact_model.dart';
import 'package:try_neuro/features/manager/data/manager_service.dart';
import 'package:try_neuro/features/resources/data/resource_service.dart';
import 'package:try_neuro/features/resources/domain/resource_model.dart';
import 'package:try_neuro/features/schedule/domain/appointment_model.dart';
import 'package:try_neuro/features/services/data/app_service.dart';
import 'package:try_neuro/features/staff/data/employee_service.dart';
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
  final _contactService = sl<ContactService>();
  final _resourceService = sl<ResourceService>();
  final _staffService = sl<StaffService>();
  final _appService = sl<AppService>();
  final _sessionService = sl<SessionService>();
  final _managerService = sl<ManagerService>();
  final _employeeService = sl<EmployeeService>();

  final _serviceController = TextEditingController();
  final _phoneSearchController = TextEditingController();
  late final TextEditingController _durationController;
  Timer? _phoneDebounce;
  bool _isAutoUpdating = false;

  List<Contact> _contacts = []; 
  List<Resource> _resources = [];
  List<StaffMember> _staff = [];

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
    _selectedDate = widget.initialAppointment?.startTime ?? widget.selectedDate;
    _phoneSearchController.addListener(_onPhoneChanged);
    _loadInitialData();
  }

  @override
  void dispose() {
    _serviceController.dispose();
    _durationController.dispose();
    _phoneSearchController.removeListener(_onPhoneChanged);
    _phoneSearchController.dispose();
    _phoneDebounce?.cancel();
    super.dispose();
  }

  void _onPhoneChanged() {
    if (_isAutoUpdating) return;

    final currentText = _phoneSearchController.text;
    final digits = PhoneUtils.clean(currentText);
    
    if (digits.isEmpty) {
      if (_selectedContact != null) {
        setState(() => _selectedContact = null);
      }
      return;
    }

    if (_selectedContact != null && 
        _selectedContact!.phones.any((p) => PhoneUtils.clean(p) == digits)) {
      return;
    }

    if (_phoneDebounce?.isActive ?? false) _phoneDebounce!.cancel();
    _phoneDebounce = Timer(const Duration(milliseconds: 800), () async {
      final latestDigits = PhoneUtils.clean(_phoneSearchController.text);
      if (latestDigits.length < 6) return;

      final contact = await _contactService.findContactByPhone(latestDigits);
      
      if (contact != null && mounted) {
        setState(() {
          _isAutoUpdating = true;
          if (!_contacts.any((c) => c.id == contact.id)) {
            _contacts.add(contact);
          }
          _selectedContact = contact;
          _phoneSearchController.text = PhoneUtils.format(contact.phones.first);
          _isAutoUpdating = false;
        });

        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Клиент найден: ${contact.name}'), duration: const Duration(seconds: 1)),
        );
      }
    });
  }

  Future<void> _loadInitialData() async {
    setState(() => _isLoading = true);
    try {
      _currentUser = await _sessionService.getCurrentUser();
      if (!mounted) return;

      late final List<StaffMember> staffList;
      if (_currentUser?.role == UserRole.manager || _currentUser?.role == UserRole.admin) {
        staffList = await _managerService.getStaffForSchedule(_selectedDate);
      } else if (_currentUser?.role == UserRole.employee) {
        final self = await _employeeService.getMyProfile(date: _selectedDate);
        staffList = [self];
      } else {
        staffList = await _staffService.getStaff();
      }

      final otherData = await Future.wait([
        _resourceService.getResources(),
        _appService.getServices(),
      ]);

      if (!mounted) return;

      _resources = otherData[0] as List<Resource>;
      _staff = staffList;

      if (widget.initialAppointment != null && widget.initialAppointment!.contactId != null) {
        final contact = await _contactService.getContactById(widget.initialAppointment!.contactId!);
        if (contact != null) {
          setState(() {
            _contacts = [contact];
            _selectedContact = contact;
            _isAutoUpdating = true;
            _phoneSearchController.text = contact.displayPhone;
            _isAutoUpdating = false;
          });
        }
      }

      if (widget.initialAppointment != null) {
        try {
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
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Ошибка загрузки: ${e.toString()}'), backgroundColor: Colors.red));
      }
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  void _showConflictDialog(String message) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Row(
          children: [
            Icon(Icons.warning_amber_rounded, color: Colors.orange),
            SizedBox(width: 8),
            Text('Конфликт времени'),
          ],
        ),
        content: Text(message),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('ПОНЯТНО', style: TextStyle(fontWeight: FontWeight.bold)),
          ),
        ],
      ),
    );
  }

  Future<void> _saveForm() async {
    if (!_formKey.currentState!.validate()) return;
    if (_selectedTime == null || _selectedContact == null) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Выберите клиента и время записи')));
      return;
    }
    setState(() => _isSaving = true);

    try {
      final combinedStartTime = DateTime(
        _selectedDate.year,
        _selectedDate.month,
        _selectedDate.day,
        _selectedTime!.hour,
        _selectedTime!.minute,
      );

      final newAppointment = Appointment(
        id: widget.initialAppointment?.id ?? 'new',
        startTime: combinedStartTime,
        durationInMinutes: int.tryParse(_durationController.text) ?? 60,
        clientName: _selectedContact!.name,
        contactId: _selectedContact!.id, 
        service: _serviceController.text,
        resourceId: _selectedResource?.id,
        staffMemberId: _selectedStaffMember?.id,
        status: widget.initialAppointment?.status ?? AppointmentStatus.scheduled,
      );

      if (widget.initialAppointment != null) {
        if (_currentUser?.role == UserRole.manager || _currentUser?.role == UserRole.admin) {
          await _managerService.updateAppointment(newAppointment);
        } else {
          await _employeeService.updateAppointment(newAppointment);
        }
      } else {
        if (_currentUser?.role == UserRole.manager || _currentUser?.role == UserRole.admin) {
          await _managerService.addAppointment(newAppointment);
        } else {
          await _employeeService.addAppointment(newAppointment);
        }
      }
      if (mounted) Navigator.of(context).pop(true);
    } on DioException catch (e) {
      if (e.response?.statusCode == 409) {
        String errorMsg = e.response?.data?['message'] ?? 'Это время уже занято';
        if (mounted) _showConflictDialog(errorMsg);
      } else {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Ошибка сохранения: ${e.toString()}'), backgroundColor: Colors.red));
        }
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Непредвиденная ошибка: ${e.toString()}'), backgroundColor: Colors.red));
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

  void _quickAddContact() async {
    final dynamic result = await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => AddContactScreen(
          initialPhone: PhoneUtils.clean(_phoneSearchController.text),
        ),
      ),
    );
    
    if (result is Contact) {
      setState(() {
        _isAutoUpdating = true;
        if (!_contacts.any((c) => c.id == result.id)) {
          _contacts.insert(0, result);
        }
        _selectedContact = result;
        _phoneSearchController.text = result.displayPhone;
        _isAutoUpdating = false;
      });
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Клиент добавлен: ${result.name}')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final bool isStaffSelectionLocked = widget.preselectedStaffId != null;
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    return Scaffold(
      backgroundColor: colorScheme.surfaceVariant.withOpacity(0.3),
      appBar: AppBar(
        title: Text(_isEditing ? 'Изменить запись' : 'Новая запись'),
        centerTitle: true,
        elevation: 0,
        backgroundColor: Colors.transparent,
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : Form(
              key: _formKey,
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(20.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    _buildSectionCard(
                      title: 'Информация о клиенте',
                      icon: Icons.person_search_outlined,
                      colorScheme: colorScheme,
                      children: [
                        TextFormField(
                          controller: _phoneSearchController,
                          decoration: InputDecoration(
                            labelText: 'Поиск по телефону',
                            hintText: '+7 (___) ___-__-__',
                            prefixIcon: const Icon(Icons.phone),
                            border: OutlineInputBorder(borderRadius: BorderRadius.circular(16)),
                            filled: true,
                            fillColor: Colors.white,
                            suffixIcon: IconButton(
                              icon: const Icon(Icons.person_add_alt_1, color: Colors.blue),
                              tooltip: 'Новый клиент',
                              onPressed: _quickAddContact,
                            ),
                          ),
                          keyboardType: TextInputType.phone,
                          inputFormatters: [InternationalPhoneInputFormatter()],
                        ),
                        const SizedBox(height: 16),
                        DropdownButtonFormField<Contact>(
                          value: _selectedContact,
                          isExpanded: true,
                          items: _contacts.map((c) => DropdownMenuItem(value: c, child: Text(c.name))).toList(),
                          onChanged: (v) {
                            if (v != null && _selectedContact?.id != v.id) {
                              setState(() {
                                _isAutoUpdating = true;
                                _selectedContact = v;
                                _phoneSearchController.text = v.displayPhone;
                                _isAutoUpdating = false;
                              });
                            }
                          },
                          decoration: InputDecoration(
                            labelText: 'Выбранный клиент',
                            border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                            prefixIcon: const Icon(Icons.person),
                            filled: true,
                            fillColor: Colors.white,
                          ),
                          validator: (v) => v == null ? 'Выберите клиента' : null,
                        ),
                      ],
                    ),
                    const SizedBox(height: 24),

                    _buildSectionCard(
                      title: 'Детали визита',
                      icon: Icons.event_note_outlined,
                      colorScheme: colorScheme,
                      children: [
                        TextFormField(
                          controller: _serviceController,
                          decoration: InputDecoration(
                            labelText: 'Услуга',
                            border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                            prefixIcon: const Icon(Icons.cut),
                            filled: true,
                            fillColor: Colors.white,
                          ),
                        ),
                        const SizedBox(height: 16),
                        Row(
                          children: [
                            Expanded(
                              flex: 3,
                              child: TextFormField(
                                controller: _durationController,
                                decoration: InputDecoration(
                                  labelText: 'Длительность',
                                  suffixText: 'мин',
                                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                                  filled: true,
                                  fillColor: Colors.white,
                                ),
                                keyboardType: TextInputType.number,
                                inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              flex: 4,
                              child: InkWell(
                                onTap: _showWheelTimePicker,
                                child: Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 16),
                                  decoration: BoxDecoration(
                                    color: Colors.white,
                                    border: Border.all(color: Colors.grey.shade400),
                                    borderRadius: BorderRadius.circular(12),
                                  ),
                                  child: Row(
                                    children: [
                                      const Icon(Icons.access_time, size: 20),
                                      const SizedBox(width: 8),
                                      Text(_selectedTime?.format(context) ?? 'Время'),
                                    ],
                                  ),
                                ),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 16),
                        ListTile(
                          contentPadding: EdgeInsets.zero,
                          leading: const Icon(Icons.calendar_today),
                          title: Text(DateFormat.yMMMMd('ru').format(_selectedDate)),
                          subtitle: const Text('Дата записи'),
                        ),
                      ],
                    ),
                    const SizedBox(height: 24),

                    _buildSectionCard(
                      title: 'Исполнение',
                      icon: Icons.badge_outlined,
                      colorScheme: colorScheme,
                      children: [
                        DropdownButtonFormField<StaffMember>(
                          value: _selectedStaffMember,
                          isExpanded: true,
                          items: _staff.map((s) => DropdownMenuItem(value: s, child: Text(s.name))).toList(),
                          onChanged: isStaffSelectionLocked ? null : (v) => setState(() => _selectedStaffMember = v),
                          decoration: InputDecoration(
                            labelText: 'Сотрудник',
                            border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                            prefixIcon: const Icon(Icons.badge),
                            filled: true,
                            fillColor: isStaffSelectionLocked ? Colors.grey.shade100 : Colors.white,
                          ),
                        ),
                        const SizedBox(height: 16),
                        DropdownButtonFormField<Resource>(
                          value: _selectedResource,
                          isExpanded: true,
                          items: _resources.map((r) => DropdownMenuItem(value: r, child: Text(r.name))).toList(),
                          onChanged: (v) => setState(() => _selectedResource = v),
                          decoration: InputDecoration(
                            labelText: 'Ресурс (кабинет)',
                            border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                            prefixIcon: const Icon(Icons.room),
                            filled: true,
                            fillColor: Colors.white,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 32),

                    FilledButton(
                      onPressed: _isSaving ? null : _saveForm,
                      style: FilledButton.styleFrom(
                        padding: const EdgeInsets.symmetric(vertical: 18),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                        minimumSize: const Size(double.infinity, 56),
                      ),
                      child: _isSaving
                          ? const SizedBox(height: 24, width: 24, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2))
                          : Text(_isEditing ? 'ОБНОВИТЬ ЗАПИСЬ' : 'СОЗДАТЬ ЗАПИСЬ', style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                    ),
                    const SizedBox(height: 40),
                  ],
                ),
              ),
            ),
    );
  }

  Widget _buildSectionCard({
    required String title,
    required IconData icon,
    required ColorScheme colorScheme,
    required List<Widget> children,
  }) {
    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(20),
        side: BorderSide(color: colorScheme.outlineVariant),
      ),
      child: Padding(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(icon, size: 18, color: colorScheme.primary),
                const SizedBox(width: 8),
                Text(
                  title.toUpperCase(),
                  style: TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.bold,
                    color: colorScheme.primary,
                    letterSpacing: 1.1,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 20),
            ...children,
          ],
        ),
      ),
    );
  }
}
