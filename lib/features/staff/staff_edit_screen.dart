import 'package:flutter/material.dart';
import 'package:try_neuro/core/session/session_service.dart';
import 'package:try_neuro/core/utils/keyboard_utils.dart'; // Добавили импорт
import 'package:try_neuro/core/utils/phone_utils.dart';
import 'package:try_neuro/features/auth/domain/user_model.dart';
import 'package:try_neuro/features/staff/data/staff_service.dart';
import 'package:try_neuro/features/staff/domain/staff_member_model.dart';
import 'package:try_neuro/service_locator.dart';
import 'dart:async';

class StaffEditScreen extends StatefulWidget {
  final StaffMember? staffMember;
  const StaffEditScreen({super.key, this.staffMember});

  @override
  State<StaffEditScreen> createState() => _StaffEditScreenState();
}

class _StaffEditScreenState extends State<StaffEditScreen> {
  final _formKey = GlobalKey<FormState>();
  final _staffService = sl<StaffService>();
  final _sessionService = sl<SessionService>();

  bool get _isEditing => widget.staffMember != null;

  final _nameController = TextEditingController();
  final _specialtyController = TextEditingController();
  final _phoneController = TextEditingController();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _newPasswordController = TextEditingController();

  // FocusNodes для всех полей
  final _nameFocusNode = FocusNode();
  final _specialtyFocusNode = FocusNode();
  final _phoneFocusNode = FocusNode();
  final _emailFocusNode = FocusNode();
  final _passwordFocusNode = FocusNode();
  final _newPasswordFocusNode = FocusNode();

  bool _hasAccount = false;
  bool _isAvailable = true;
  String _selectedRole = 'EMPLOYEE';
  TimeOfDay? _workStartTime, _workEndTime, _breakStartTime, _breakEndTime;
  bool _isLoading = true;
  User? _currentUser;

  @override
  void initState() {
    super.initState();
    _loadInitialData();
  }

  Future<void> _loadInitialData() async {
    final user = await _sessionService.getCurrentUser();
    final member = widget.staffMember;

    if (mounted) {
      setState(() {
        _currentUser = user;
        if (member != null) {
          _nameController.text = member.name;
          _specialtyController.text = member.specialty;
          _phoneController.text = member.phone != null ? PhoneUtils.format(member.phone) : '';
          _isAvailable = member.available;
          _selectedRole = member.role ?? 'EMPLOYEE';
          _workStartTime = member.workStartTime;
          _workEndTime = member.workEndTime;
          _breakStartTime = member.breakStartTime;
          _breakEndTime = member.breakEndTime;
          if (member.email != null) {
            _hasAccount = true;
            _emailController.text = member.email!;
          }
        }
        _isLoading = false;
      });
    }
  }

  @override
  void dispose() {
    _nameController.dispose();
    _specialtyController.dispose();
    _phoneController.dispose();
    _emailController.dispose();
    _passwordController.dispose();
    _newPasswordController.dispose();
    // Dispose FocusNodes
    _nameFocusNode.dispose();
    _specialtyFocusNode.dispose();
    _phoneFocusNode.dispose();
    _emailFocusNode.dispose();
    _passwordFocusNode.dispose();
    _newPasswordFocusNode.dispose();
    super.dispose();
  }

  String? _formatTime(TimeOfDay? time) {
    if (time == null) return null;
    return '${time.hour.toString().padLeft(2, '0')}:${time.minute.toString().padLeft(2, '0')}';
  }

  Future<void> _saveForm() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _isLoading = true);

    try {
      final String cleanPhone = PhoneUtils.clean(_phoneController.text);

      if (_isEditing) {
        await _staffService.updateStaffMember(
          id: widget.staffMember!.id,
          name: _nameController.text,
          specialty: _specialtyController.text,
          phone: cleanPhone,
          role: _selectedRole,
          available: _isAvailable,
          workStartTime: _formatTime(_workStartTime),
          workEndTime: _formatTime(_workEndTime),
          breakStartTime: _formatTime(_breakStartTime),
          breakEndTime: _formatTime(_breakEndTime),
          email: _emailController.text,
          password: _newPasswordController.text,
        );
      } else {
        await _staffService.addStaffMember(
          name: _nameController.text,
          specialty: _specialtyController.text,
          phone: cleanPhone,
          email: _hasAccount ? _emailController.text : null,
          password: _hasAccount ? _passwordController.text : null,
          role: _selectedRole,
          available: _isAvailable,
          workStartTime: _formatTime(_workStartTime),
          workEndTime: _formatTime(_workEndTime),
          breakStartTime: _formatTime(_breakStartTime),
          breakEndTime: _formatTime(_breakEndTime),
        );
      }
      if (mounted) Navigator.of(context).pop(true);
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final bool isAdmin = _currentUser?.role == UserRole.admin;
    final bool isEmailFieldEnabled = !_isEditing || isAdmin;

    return Scaffold(
      appBar: AppBar(title: Text(_isEditing ? 'Редактировать сотрудника' : 'Новый сотрудник')),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : Form(
              key: _formKey,
              child: ListView(
                padding: const EdgeInsets.all(16.0),
                children: [
                  TextFormField(
                    controller: _nameController, 
                    focusNode: _nameFocusNode,
                    onTap: () => KeyboardUtils.onTextFieldTap(_nameFocusNode),
                    decoration: const InputDecoration(labelText: 'Имя', border: OutlineInputBorder()), 
                    validator: (v) => v!.isEmpty ? 'Введите имя' : null
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _specialtyController, 
                    focusNode: _specialtyFocusNode,
                    onTap: () => KeyboardUtils.onTextFieldTap(_specialtyFocusNode),
                    decoration: const InputDecoration(labelText: 'Специальность', border: OutlineInputBorder()), 
                    validator: (v) => v!.isEmpty ? 'Введите специальность' : null
                  ),
                  const SizedBox(height: 16),
                  
                  TextFormField(
                    controller: _phoneController,
                    focusNode: _phoneFocusNode,
                    onTap: () => KeyboardUtils.onTextFieldTap(_phoneFocusNode),
                    decoration: const InputDecoration(
                      labelText: 'Телефон', 
                      border: OutlineInputBorder(),
                      hintText: '+7 (___) ___-__-__'
                    ),
                    keyboardType: TextInputType.phone,
                    inputFormatters: [InternationalPhoneInputFormatter()],
                  ),
                  const SizedBox(height: 16),

                  if (!_isEditing)
                    SwitchListTile(
                      title: const Text('Создать учетную запись'),
                      value: _hasAccount,
                      onChanged: (value) => setState(() => _hasAccount = value),
                    ),
                  if (_hasAccount) ...[
                    TextFormField(
                      controller: _emailController, 
                      focusNode: _emailFocusNode,
                      onTap: () => KeyboardUtils.onTextFieldTap(_emailFocusNode),
                      decoration: const InputDecoration(labelText: 'Email', border: OutlineInputBorder()), 
                      validator: (v) => v!.isEmpty ? 'Введите email' : null, 
                      enabled: isEmailFieldEnabled,
                    ),
                    const SizedBox(height: 16),
                    if (!_isEditing)
                      TextFormField(
                        controller: _passwordController, 
                        focusNode: _passwordFocusNode,
                        onTap: () => KeyboardUtils.onTextFieldTap(_passwordFocusNode),
                        decoration: const InputDecoration(labelText: 'Пароль', border: OutlineInputBorder()), 
                        obscureText: true, 
                        validator: (v) => (v!.isEmpty) ? 'Введите пароль' : null
                      ),
                    if (_isEditing && isAdmin)
                      TextFormField(
                        controller: _newPasswordController, 
                        focusNode: _newPasswordFocusNode,
                        onTap: () => KeyboardUtils.onTextFieldTap(_newPasswordFocusNode),
                        decoration: const InputDecoration(labelText: 'Новый пароль (оставьте пустым)', border: OutlineInputBorder()), 
                        obscureText: true
                      ),
                    const SizedBox(height: 16),
                  ],
                  DropdownButtonFormField<String>(
                    value: _selectedRole,
                    items: const [
                      DropdownMenuItem(value: 'EMPLOYEE', child: Text('Сотрудник')), 
                      DropdownMenuItem(value: 'MANAGER', child: Text('Менеджер'))
                    ],
                    onChanged: (v) => setState(() => _selectedRole = v!),
                    decoration: const InputDecoration(labelText: 'Роль', border: OutlineInputBorder()),
                  ),
                  const SizedBox(height: 16),
                  SwitchListTile(title: const Text('Доступен'), value: _isAvailable, onChanged: (v) => setState(() => _isAvailable = v)),
                  const SizedBox(height: 24),
                  ElevatedButton(
                    onPressed: _isLoading ? null : _saveForm, 
                    child: const Text('Сохранить')
                  ),
                ],
              ),
            ),
    );
  }
}
