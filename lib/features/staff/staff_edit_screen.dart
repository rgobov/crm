
import 'package:flutter/material.dart';
import 'package:try_neuro/features/staff/data/staff_service.dart';
import 'package:try_neuro/features/staff/domain/staff_member_model.dart';
import 'package:try_neuro/service_locator.dart';
import 'package:intl/intl.dart';

class StaffEditScreen extends StatefulWidget {
  final StaffMember? initialStaffMember;

  const StaffEditScreen({super.key, this.initialStaffMember});

  @override
  State<StaffEditScreen> createState() => _StaffEditScreenState();
}

class _StaffEditScreenState extends State<StaffEditScreen> {
  final _formKey = GlobalKey<FormState>();
  final _staffService = sl<StaffService>();

  late final TextEditingController _nameController;
  late final TextEditingController _specialtyController;
  
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  String _selectedRole = 'EMPLOYEE';
  bool _isAvailable = true;
  TimeOfDay? _workStartTime, _workEndTime, _breakStartTime, _breakEndTime;

  bool _isSaving = false;
  bool _createUser = false;
  bool get _isEditing => widget.initialStaffMember != null;
  bool _hasAccount = false;

  @override
  void initState() {
    super.initState();
    final staff = widget.initialStaffMember;
    _nameController = TextEditingController(text: staff?.name);
    _specialtyController = TextEditingController(text: staff?.specialty);
    
    if (staff != null) {
      _isAvailable = staff.available;
      _workStartTime = staff.workStartTime;
      _workEndTime = staff.workEndTime;
      _breakStartTime = staff.breakStartTime;
      _breakEndTime = staff.breakEndTime;

      if (staff.role != null && staff.role != 'NONE') {
        _hasAccount = true;
        _selectedRole = staff.role!;
      }
    }
  }

  Future<void> _saveForm() async {
    if (_formKey.currentState!.validate()) {
      setState(() => _isSaving = true);

      try {
        if (_isEditing) {
          final updatedStaffMember = StaffMember(
            id: widget.initialStaffMember!.id,
            name: _nameController.text,
            specialty: _specialtyController.text,
            available: _isAvailable,
            workStartTime: _workStartTime,
            workEndTime: _workEndTime,
            breakStartTime: _breakStartTime,
            breakEndTime: _breakEndTime,
          );
          await _staffService.updateStaffMember(updatedStaffMember, role: _hasAccount ? _selectedRole : null);
        } else {
          await _staffService.addStaffMember(
            name: _nameController.text,
            specialty: _specialtyController.text,
            email: _createUser ? _emailController.text : null,
            password: _createUser ? _passwordController.text : null,
            role: _createUser ? _selectedRole : null,
            available: _isAvailable,
            workStartTime: _workStartTime,
            workEndTime: _workEndTime,
            breakStartTime: _breakStartTime,
            breakEndTime: _breakEndTime,
          );
        }

        if (mounted) Navigator.of(context).pop(true);
      } catch (e) {
        if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Ошибка: $e')));
      } finally {
        if (mounted) setState(() => _isSaving = false);
      }
    }
  }

  @override
  void dispose() {
    _nameController.dispose();
    _specialtyController.dispose();
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_isEditing ? 'Изменить сотрудника' : 'Новый сотрудник'),
        actions: [
          if (_isSaving) const Padding(padding: EdgeInsets.only(right: 16), child: CircularProgressIndicator()) else IconButton(icon: const Icon(Icons.save), onPressed: _saveForm, tooltip: 'Сохранить')
        ],
      ),
      body: Form(
        key: _formKey,
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              TextFormField(controller: _nameController, decoration: const InputDecoration(labelText: 'Имя', border: OutlineInputBorder()), validator: (v) => v!.trim().isEmpty ? 'Введите имя' : null),
              const SizedBox(height: 16),
              TextFormField(controller: _specialtyController, decoration: const InputDecoration(labelText: 'Специальность', border: OutlineInputBorder())),
              const SizedBox(height: 24),
              const Divider(),
              SwitchListTile(title: const Text('Сотрудник доступен'), subtitle: const Text('Если выключено, его нельзя будет выбрать для записи'), value: _isAvailable, onChanged: (v) => setState(() => _isAvailable = v)),
              const Divider(),
              const SizedBox(height: 16),
              Text('График работы', style: Theme.of(context).textTheme.titleMedium),
              const SizedBox(height: 8),
              Row(children: [ Expanded(child: _buildTimePickerField('Начало работы', _workStartTime, (t) => setState(() => _workStartTime = t))), const SizedBox(width: 16), Expanded(child: _buildTimePickerField('Конец работы', _workEndTime, (t) => setState(() => _workEndTime = t)))]), 
              const SizedBox(height: 16),
               Row(children: [ Expanded(child: _buildTimePickerField('Начало перерыва', _breakStartTime, (t) => setState(() => _breakStartTime = t))), const SizedBox(width: 16), Expanded(child: _buildTimePickerField('Конец перерыва', _breakEndTime, (t) => setState(() => _breakEndTime = t)))]), 
              const SizedBox(height: 24),
              const Divider(),
              
              if (_isEditing && _hasAccount) ...[
                Text('Управление доступом', style: Theme.of(context).textTheme.titleMedium),
                const SizedBox(height: 16),
                DropdownButtonFormField<String>(value: _selectedRole, items: const [ DropdownMenuItem(value: 'EMPLOYEE', child: Text('Сотрудник')), DropdownMenuItem(value: 'MANAGER', child: Text('Менеджер'))], onChanged: (v) => setState(() => _selectedRole = v!), decoration: const InputDecoration(labelText: 'Роль', border: OutlineInputBorder())),
              ],

              if (!_isEditing) ...[
                SwitchListTile(title: const Text('Создать учетную запись'), value: _createUser, onChanged: (v) => setState(() => _createUser = v)),
                if (_createUser) ...[
                  const SizedBox(height: 16),
                  TextFormField(controller: _emailController, decoration: const InputDecoration(labelText: 'Email', border: OutlineInputBorder()), validator: (v) => _createUser && (v == null || !v.contains('@')) ? 'Введите email' : null),
                  const SizedBox(height: 16),
                  TextFormField(controller: _passwordController, decoration: const InputDecoration(labelText: 'Пароль', border: OutlineInputBorder()), obscureText: true, validator: (v) => _createUser && (v == null || v.length < 6) ? 'Минимум 6 символов' : null),
                  const SizedBox(height: 16),
                  DropdownButtonFormField<String>(value: _selectedRole, items: const [ DropdownMenuItem(value: 'EMPLOYEE', child: Text('Сотрудник')), DropdownMenuItem(value: 'MANAGER', child: Text('Менеджер'))], onChanged: (v) => setState(() => _selectedRole = v!), decoration: const InputDecoration(labelText: 'Роль', border: OutlineInputBorder())),
                ],
              ],
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildTimePickerField(String label, TimeOfDay? time, Function(TimeOfDay?) onTimeChanged) {
    return InkWell(
      onTap: () async {
        final newTime = await showTimePicker(context: context, initialTime: time ?? TimeOfDay.now());
        if (newTime != null) {
          onTimeChanged(newTime);
        }
      },
      child: InputDecorator(
        decoration: InputDecoration(labelText: label, border: const OutlineInputBorder()),
        child: Text(time?.format(context) ?? 'Не указано'),
      ),
    );
  }
}
