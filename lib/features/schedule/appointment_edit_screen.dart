import 'dart:async';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:intl/intl.dart';
import 'package:try_neuro/core/session/session_service.dart';
import 'package:try_neuro/features/auth/domain/user_model.dart';
import 'package:try_neuro/features/contacts/add_contact_screen.dart'; 
import 'package:try_neuro/features/contacts/data/contact_service.dart';
import 'package:try_neuro/features/contacts/domain/contact_model.dart';
import 'package:try_neuro/features/manager/data/manager_service.dart';
import 'package:try_neuro/features/resources/data/resource_service.dart';
import 'package:try_neuro/features/resources/domain/resource_model.dart';
import 'package:try_neuro/features/schedule/domain/appointment_model.dart';
import 'package:try_neuro/features/services/data/app_service.dart';
import 'package:try_neuro/features/services/domain/service_model.dart';
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
    final phoneInput = _phoneSearchController.text.trim();
    
    // 1. Если поле пустое — сбрасываем выбор клиента
    if (phoneInput.isEmpty) {
      if (_selectedContact != null) {
        setState(() => _selectedContact = null);
      }
      return;
    }

    // 2. Если этот номер уже принадлежит выбранному контакту — ничего не ищем
    if (_selectedContact != null && _selectedContact!.phone == phoneInput) {
      return;
    }

    // 3. Запускаем поиск с задержкой (Debounce)
    if (_phoneDebounce?.isActive ?? false) _phoneDebounce!.cancel();
    _phoneDebounce = Timer(const Duration(milliseconds: 700), () async {
      // Берем самое актуальное значение из контроллера внутри таймера
      final currentPhone = _phoneSearchController.text.trim();
      
      if (currentPhone.length >= 5) {
        final contact = await _contactService.findContactByPhone(currentPhone);
        
        if (contact != null && mounted) {
          // Если за время запроса мы уже выбрали этого клиента — выходим
          if (_selectedContact?.id == contact.id) return;

          setState(() {
            // Пытаемся найти контакт в уже загруженном списке или добавляем его
            final index = _contacts.indexWhere((c) => c.id == contact.id);
            if (index != -1) {
              _selectedContact = _contacts[index];
            } else {
              _contacts.insert(0, contact);
              _selectedContact = contact;
            }
            
            // Синхронизируем текст (без вызова слушателя снова)
            _phoneSearchController.removeListener(_onPhoneChanged);
            _phoneSearchController.text = contact.phone;
            _phoneSearchController.addListener(_onPhoneChanged);
          });

          // Уведомляем пользователя один раз
          ScaffoldMessenger.of(context).clearSnackBars();
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('Клиент найден: ${contact.name}'), 
              duration: const Duration(seconds: 2),
              behavior: SnackBarBehavior.floating,
            ),
          );
        }
      }
    });
  }

  Future<void> _loadInitialData() async {
    setState(() => _isLoading = true);
    try {
      _currentUser = await _sessionService.getCurrentUser();
      if (!mounted) return;

      late final List<StaffMember> staffList;
      if (_currentUser?.role == UserRole.manager) {
        staffList = await _managerService.getStaffForSchedule();
      } else if (_currentUser?.role == UserRole.employee) {
        final self = await _employeeService.getMyProfile();
        staffList = [self];
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
          
          if (_selectedContact != null) {
            _phoneSearchController.removeListener(_onPhoneChanged);
            _phoneSearchController.text = _selectedContact!.phone;
            _phoneSearchController.addListener(_onPhoneChanged);
          }
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
        if (_currentUser?.role == UserRole.manager) {
          await _managerService.updateAppointment(newAppointment);
        } else {
          await _employeeService.updateAppointment(newAppointment);
        }
      } else {
        if (_currentUser?.role == UserRole.manager) {
          await _managerService.addAppointment(newAppointment);
        } else {
          await _employeeService.addAppointment(newAppointment);
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

  void _quickAddContact() async {
    final dynamic result = await Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => const AddContactScreen()),
    );
    
    if (result is Contact) {
      setState(() {
        _contacts.insert(0, result);
        _selectedContact = result;
        
        _phoneSearchController.removeListener(_onPhoneChanged);
        _phoneSearchController.text = result.phone;
        _phoneSearchController.addListener(_onPhoneChanged);
      });
      ScaffoldMessenger.of(context).clearSnackBars();
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Клиент добавлен и выбран: ${result.name}')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final bool isStaffSelectionLocked = widget.preselectedStaffId != null;
    final primaryColor = Theme.of(context).primaryColor;

    return Scaffold(
      backgroundColor: Colors.grey.shade50,
      appBar: AppBar(
        title: Text(_isEditing ? 'Изменить запись' : 'Новая запись'),
        elevation: 0,
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
                    _buildSectionHeader('Информация о клиенте'),
                    Card(
                      elevation: 0,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                        side: BorderSide(color: Colors.grey.shade200),
                      ),
                      child: Padding(
                        padding: const EdgeInsets.all(16.0),
                        child: Column(
                          children: [
                            TextFormField(
                              controller: _phoneSearchController,
                              decoration: InputDecoration(
                                labelText: 'Поиск по телефону',
                                hintText: '+7...',
                                prefixIcon: const Icon(Icons.phone),
                                border: const OutlineInputBorder(),
                                suffixIcon: IconButton(
                                  icon: const Icon(Icons.person_add_alt_1, color: Colors.blue),
                                  tooltip: 'Новый клиент',
                                  onPressed: _quickAddContact,
                                ),
                              ),
                              keyboardType: TextInputType.phone,
                            ),
                            const SizedBox(height: 16),
                            DropdownButtonFormField<Contact>(
                              value: _selectedContact,
                              isExpanded: true,
                              items: _contacts.map((c) => DropdownMenuItem(value: c, child: Text(c.name))).toList(),
                              onChanged: (v) {
                                if (v != null && _selectedContact?.id != v.id) {
                                  setState(() {
                                    _selectedContact = v;
                                    _phoneSearchController.removeListener(_onPhoneChanged);
                                    _phoneSearchController.text = v.phone;
                                    _phoneSearchController.addListener(_onPhoneChanged);
                                  });
                                }
                              },
                              decoration: const InputDecoration(
                                labelText: 'Клиент',
                                border: OutlineInputBorder(),
                                prefixIcon: Icon(Icons.person),
                              ),
                              validator: (v) => v == null ? 'Выберите клиента' : null,
                            ),
                          ],
                        ),
                      ),
                    ),
                    const SizedBox(height: 24),

                    _buildSectionHeader('Детали визита'),
                    Card(
                      elevation: 0,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                        side: BorderSide(color: Colors.grey.shade200),
                      ),
                      child: Padding(
                        padding: const EdgeInsets.all(16.0),
                        child: Column(
                          children: [
                            TextFormField(
                              controller: _serviceController,
                              decoration: const InputDecoration(
                                labelText: 'Услуга',
                                border: OutlineInputBorder(),
                                prefixIcon: Icon(Icons.cut),
                              ),
                            ),
                            const SizedBox(height: 16),
                            Row(
                              children: [
                                Expanded(
                                  flex: 3,
                                  child: TextFormField(
                                    controller: _durationController,
                                    decoration: const InputDecoration(
                                      labelText: 'Длительность',
                                      suffixText: 'мин',
                                      border: OutlineInputBorder(),
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
                                        border: Border.all(color: Colors.grey.shade400),
                                        borderRadius: BorderRadius.circular(4),
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
                      ),
                    ),
                    const SizedBox(height: 24),

                    _buildSectionHeader('Исполнение'),
                    Card(
                      elevation: 0,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                        side: BorderSide(color: Colors.grey.shade200),
                      ),
                      child: Padding(
                        padding: const EdgeInsets.all(16.0),
                        child: Column(
                          children: [
                            DropdownButtonFormField<StaffMember>(
                              value: _selectedStaffMember,
                              isExpanded: true,
                              items: _staff.map((s) => DropdownMenuItem(value: s, child: Text(s.name))).toList(),
                              onChanged: isStaffSelectionLocked ? null : (v) => setState(() => _selectedStaffMember = v),
                              decoration: InputDecoration(
                                labelText: 'Сотрудник',
                                border: const OutlineInputBorder(),
                                prefixIcon: const Icon(Icons.badge),
                                filled: isStaffSelectionLocked,
                                fillColor: isStaffSelectionLocked ? Colors.grey.shade100 : null,
                              ),
                            ),
                            const SizedBox(height: 16),
                            DropdownButtonFormField<Resource>(
                              value: _selectedResource,
                              isExpanded: true,
                              items: _resources.map((r) => DropdownMenuItem(value: r, child: Text(r.name))).toList(),
                              onChanged: (v) => setState(() => _selectedResource = v),
                              decoration: const InputDecoration(
                                labelText: 'Ресурс (кабинет)',
                                border: OutlineInputBorder(),
                                prefixIcon: Icon(Icons.room),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                    const SizedBox(height: 32),

                    SizedBox(
                      height: 54,
                      child: ElevatedButton(
                        style: ElevatedButton.styleFrom(
                          backgroundColor: primaryColor,
                          foregroundColor: Colors.white,
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                          elevation: 0,
                        ),
                        onPressed: _isSaving ? null : _saveForm,
                        child: _isSaving
                            ? const CircularProgressIndicator(color: Colors.white)
                            : Text(_isEditing ? 'ОБНОВИТЬ ЗАПИСЬ' : 'СОЗДАТЬ ЗАПИСЬ', style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                      ),
                    ),
                    const SizedBox(height: 40),
                  ],
                ),
              ),
            ),
    );
  }

  Widget _buildSectionHeader(String title) {
    return Padding(
      padding: const EdgeInsets.only(left: 4, bottom: 8),
      child: Text(
        title.toUpperCase(),
        style: TextStyle(
          fontSize: 12,
          fontWeight: FontWeight.bold,
          color: Colors.grey.shade600,
          letterSpacing: 1.1,
        ),
      ),
    );
  }
}
