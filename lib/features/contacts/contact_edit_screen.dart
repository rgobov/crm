import 'package:flutter/material.dart';
import 'package:try_neuro/features/contacts/data/contact_service.dart';
import 'package:try_neuro/features/contacts/domain/contact_model.dart';
import 'package:try_neuro/service_locator.dart';

class ContactEditScreen extends StatefulWidget {
  final Contact? initialContact;

  const ContactEditScreen({super.key, this.initialContact});

  @override
  State<ContactEditScreen> createState() => _ContactEditScreenState();
}

class _ContactEditScreenState extends State<ContactEditScreen> {
  final _formKey = GlobalKey<FormState>();
  final _contactService = sl<ContactService>();

  late final TextEditingController _nameController;
  final List<TextEditingController> _phoneControllers = [];
  late final TextEditingController _emailController;
  late final TextEditingController _notesController;

  bool _isSaving = false;
  bool get _isEditing => widget.initialContact != null;

  @override
  void initState() {
    super.initState();
    _nameController = TextEditingController(text: widget.initialContact?.name);
    
    // Инициализируем контроллеры для всех существующих телефонов
    if (_isEditing && widget.initialContact!.phones.isNotEmpty) {
      for (var phone in widget.initialContact!.phones) {
        _phoneControllers.add(TextEditingController(text: phone));
      }
    } else {
      _phoneControllers.add(TextEditingController());
    }

    _emailController = TextEditingController(text: widget.initialContact?.email);
    _notesController = TextEditingController(text: widget.initialContact?.notes);
  }

  @override
  void dispose() {
    _nameController.dispose();
    for (var controller in _phoneControllers) {
      controller.dispose();
    }
    _emailController.dispose();
    _notesController.dispose();
    super.dispose();
  }

  void _addPhoneField() {
    setState(() {
      _phoneControllers.add(TextEditingController());
    });
  }

  void _removePhoneField(int index) {
    if (_phoneControllers.length > 1) {
      setState(() {
        _phoneControllers[index].dispose();
        _phoneControllers.removeAt(index);
      });
    }
  }

  Future<void> _saveForm() async {
    if (!_formKey.currentState!.validate()) return;

    final phones = _phoneControllers
        .map((c) => c.text.trim())
        .where((text) => text.isNotEmpty)
        .toList();

    if (phones.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Добавьте хотя бы один номер телефона')),
      );
      return;
    }

    setState(() {
      _isSaving = true;
    });

    try {
      final contactData = Contact(
        id: _isEditing ? widget.initialContact!.id : '',
        name: _nameController.text.trim(),
        phones: phones,
        email: _emailController.text.trim().isEmpty ? null : _emailController.text.trim(),
        notes: _notesController.text.trim().isEmpty ? null : _notesController.text.trim(),
      );

      if (_isEditing) {
        await _contactService.updateContact(contactData);
      } else {
        await _contactService.addContact(
          name: contactData.name,
          phones: contactData.phones,
          email: contactData.email,
          notes: contactData.notes,
        );
      }

      if (mounted) {
        Navigator.of(context).pop(true);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Ошибка сохранения: ${e.toString()}'), backgroundColor: Colors.red),
        );
      }
    } finally {
      if (mounted) {
        setState(() => _isSaving = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_isEditing ? 'Изменить клиента' : 'Новый клиент'),
        actions: [
          if (_isSaving)
            const Padding(
              padding: EdgeInsets.only(right: 16.0),
              child: Center(child: SizedBox(width: 24, height: 24, child: CircularProgressIndicator(strokeWidth: 3))),
            )
          else
            IconButton(
              icon: const Icon(Icons.save),
              onPressed: _saveForm,
              tooltip: 'Сохранить',
            ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              TextFormField(
                controller: _nameController,
                decoration: const InputDecoration(
                  labelText: 'Имя фамилия*',
                  border: OutlineInputBorder(),
                  prefixIcon: Icon(Icons.person),
                ),
                validator: (v) => v == null || v.trim().isEmpty ? 'Введите имя' : null,
              ),
              const SizedBox(height: 24),
              
              const Text('ТЕЛЕФОНЫ*', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12, color: Colors.grey)),
              const SizedBox(height: 8),
              ..._phoneControllers.asMap().entries.map((entry) {
                int index = entry.key;
                TextEditingController controller = entry.value;
                return Padding(
                  padding: const EdgeInsets.only(bottom: 12.0),
                  child: Row(
                    children: [
                      Expanded(
                        child: TextFormField(
                          controller: controller,
                          decoration: InputDecoration(
                            labelText: index == 0 ? 'Основной телефон' : 'Дополнительный',
                            prefixIcon: const Icon(Icons.phone),
                            border: const OutlineInputBorder(),
                          ),
                          keyboardType: TextInputType.phone,
                          validator: (v) => index == 0 && (v == null || v.trim().isEmpty) ? 'Введите телефон' : null,
                        ),
                      ),
                      if (_phoneControllers.length > 1)
                        IconButton(
                          icon: const Icon(Icons.remove_circle_outline, color: Colors.red),
                          onPressed: () => _removePhoneField(index),
                        ),
                    ],
                  ),
                );
              }),
              TextButton.icon(
                onPressed: _addPhoneField,
                icon: const Icon(Icons.add),
                label: const Text('ДОБАВИТЬ ЕЩЕ НОМЕР'),
              ),
              
              const SizedBox(height: 16),
              TextFormField(
                controller: _emailController,
                decoration: const InputDecoration(
                  labelText: 'Email',
                  border: OutlineInputBorder(),
                  prefixIcon: Icon(Icons.email),
                ),
                keyboardType: TextInputType.emailAddress,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _notesController,
                decoration: const InputDecoration(
                  labelText: 'Заметки',
                  border: OutlineInputBorder(),
                  prefixIcon: Icon(Icons.note),
                ),
                maxLines: 3,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
