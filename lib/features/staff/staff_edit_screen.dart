
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

  bool _isSaving = false;
  bool get _isEditing => widget.initialStaffMember != null;

  @override
  void initState() {
    super.initState();
    _nameController = TextEditingController(text: widget.initialStaffMember?.name);
    _specialtyController = TextEditingController(text: widget.initialStaffMember?.specialty);
  }

  Future<void> _saveForm() async {
    if (_formKey.currentState!.validate()) {
      setState(() {
        _isSaving = true;
      });

      if (_isEditing) {
        final updatedStaffMember = StaffMember(
          id: widget.initialStaffMember!.id,
          name: _nameController.text,
          specialty: _specialtyController.text,
        );
        await _staffService.updateStaffMember(updatedStaffMember);
      } else {
        await _staffService.addStaffMember(
          name: _nameController.text,
          specialty: _specialtyController.text,
        );
      }

      if (mounted) {
        Navigator.of(context).pop(true);
      }
    }
  }

 @override
  void dispose() {
    _nameController.dispose();
    _specialtyController.dispose();
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
            ],
          ),
        ),
      ),
    );
  }
}
