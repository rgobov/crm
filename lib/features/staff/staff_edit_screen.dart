
import 'package:flutter/material.dart';
import 'package:try_neuro/features/staff/data/staff_service.dart';
import 'package:try_neuro/features/staff/domain/staff_member_model.dart';
import 'package:try_neuro/service_locator.dart';

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
  
  // Новые контроллеры для пользователя
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  String _selectedRole = 'EMPLOYEE';

  bool _isSaving = false;
  bool _createUser = false; // Флаг, нужно ли создавать пользователя
  bool get _isEditing => widget.initialStaffMember != null;
  bool _hasAccount = false; // Есть ли уже аккаунт у сотрудника

  @override
  void initState() {
    super.initState();
    _nameController = TextEditingController(text: widget.initialStaffMember?.name);
    _specialtyController = TextEditingController(text: widget.initialStaffMember?.specialty);
    
    // Если редактируем и у сотрудника уже есть роль (не NONE/null), значит аккаунт есть
    if (_isEditing && widget.initialStaffMember?.role != null && widget.initialStaffMember!.role != 'NONE') {
      _hasAccount = true;
      _selectedRole = widget.initialStaffMember!.role!;
      if (_selectedRole == 'ADMIN') _selectedRole = 'MANAGER'; // Админа понизить нельзя через этот UI, но покажем как менеджера или добавим опцию
    } else {
      _hasAccount = false;
    }
  }

  Future<void> _saveForm() async {
    if (_formKey.currentState!.validate()) {
      setState(() {
        _isSaving = true;
      });

      try {
        if (_isEditing) {
          final updatedStaffMember = StaffMember(
            id: widget.initialStaffMember!.id,
            name: _nameController.text,
            specialty: _specialtyController.text,
            role: _hasAccount ? _selectedRole : null, // Передаем роль при обновлении
          );
          await _staffService.updateStaffMember(updatedStaffMember, role: _hasAccount ? _selectedRole : null);
          
          // Если аккаунта не было, но пользователь захотел создать (логика создания юзера при update пока не реализована полностью на клиенте, 
          // но на бэкенде мы добавили обработку email/pass. Если нужно добавить создание аккаунта существующему сотруднику - нужно открыть поля email/pass)
          // В текущей реализации мы разрешаем менять роль, если аккаунт УЖЕ есть.
        } else {
          await _staffService.addStaffMember(
            name: _nameController.text,
            specialty: _specialtyController.text,
            email: _createUser ? _emailController.text : null,
            password: _createUser ? _passwordController.text : null,
            role: _createUser ? _selectedRole : null,
          );
        }

        if (mounted) {
          Navigator.of(context).pop(true);
        }
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Ошибка: $e')));
        }
      } finally {
        if (mounted) {
          setState(() => _isSaving = false);
        }
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
          if (_isSaving)
            const Padding(padding: EdgeInsets.only(right: 16), child: CircularProgressIndicator())
          else
            IconButton(icon: const Icon(Icons.save), onPressed: _saveForm, tooltip: 'Сохранить')
        ],
      ),
      body: Form(
        key: _formKey,
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              TextFormField(
                controller: _nameController,
                decoration: const InputDecoration(
                  labelText: 'Имя сотрудника',
                  border: OutlineInputBorder(),
                  prefixIcon: Icon(Icons.person),
                ),
                validator: (value) => (value == null || value.trim().isEmpty) ? 'Введите имя' : null,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _specialtyController,
                decoration: const InputDecoration(
                  labelText: 'Специальность (необязательно)',
                  border: OutlineInputBorder(),
                  prefixIcon: Icon(Icons.work),
                ),
              ),
              
              const SizedBox(height: 24),
              const Divider(),
              
              // Если редактируем и аккаунт уже есть - даем сменить роль
              if (_isEditing && _hasAccount) ...[
                const Text('Управление доступом', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                const SizedBox(height: 16),
                DropdownButtonFormField<String>(
                  value: _selectedRole,
                  decoration: const InputDecoration(
                    labelText: 'Роль в системе',
                    border: OutlineInputBorder(),
                    prefixIcon: Icon(Icons.security),
                  ),
                  items: const [
                    DropdownMenuItem(value: 'EMPLOYEE', child: Text('Сотрудник (Обычный доступ)')),
                    DropdownMenuItem(value: 'MANAGER', child: Text('Менеджер (Управление)')),
                  ],
                  onChanged: (value) {
                    setState(() {
                      _selectedRole = value!;
                    });
                  },
                ),
                const Padding(
                  padding: EdgeInsets.only(top: 8.0),
                  child: Text('Смена роли применится немедленно.', style: TextStyle(color: Colors.grey, fontSize: 12)),
                ),
              ],

              // Если создаем нового - логика создания аккаунта
              if (!_isEditing) ...[
                SwitchListTile(
                  title: const Text('Создать учетную запись для входа'),
                  subtitle: const Text('Сотрудник сможет входить в приложение'),
                  value: _createUser,
                  onChanged: (value) {
                    setState(() {
                      _createUser = value;
                    });
                  },
                ),
                
                if (_createUser) ...[
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _emailController,
                    decoration: const InputDecoration(
                      labelText: 'Email (логин)',
                      border: OutlineInputBorder(),
                      prefixIcon: Icon(Icons.email),
                    ),
                    validator: (value) {
                      if (!_createUser) return null;
                      if (value == null || !value.contains('@')) return 'Введите корректный email';
                      return null;
                    },
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _passwordController,
                    decoration: const InputDecoration(
                      labelText: 'Пароль',
                      border: OutlineInputBorder(),
                      prefixIcon: Icon(Icons.lock),
                    ),
                    obscureText: true,
                    validator: (value) {
                      if (!_createUser) return null;
                      if (value == null || value.length < 6) return 'Минимум 6 символов';
                      return null;
                    },
                  ),
                  const SizedBox(height: 16),
                  DropdownButtonFormField<String>(
                    value: _selectedRole,
                    decoration: const InputDecoration(
                      labelText: 'Роль в системе',
                      border: OutlineInputBorder(),
                      prefixIcon: Icon(Icons.security),
                    ),
                    items: const [
                      DropdownMenuItem(value: 'EMPLOYEE', child: Text('Сотрудник (Обычный доступ)')),
                      DropdownMenuItem(value: 'MANAGER', child: Text('Менеджер (Управление)')),
                    ],
                    onChanged: (value) {
                      setState(() {
                        _selectedRole = value!;
                      });
                    },
                  ),
                ],
              ],
            ],
          ),
        ),
      ),
    );
  }
}
