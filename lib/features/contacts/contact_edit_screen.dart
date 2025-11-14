
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
  final _contactService = sl<ContactService>(); // <--- ИЗМЕНЕНИЕ

  late final _nameController;
  late final _phoneController;
  late final _emailController;
  late final _notesController;

  bool _isSaving = false;
  bool get _isEditing => widget.initialContact != null;

  @override
  void initState() {
    super.initState();
    _nameController = TextEditingController(text: widget.initialContact?.name);
    _phoneController = TextEditingController(text: widget.initialContact?.phone);
    _emailController = TextEditingController(text: widget.initialContact?.email);
    _notesController = TextEditingController(text: widget.initialContact?.notes);
  }

  Future<void> _saveForm() async {
    if (_formKey.currentState!.validate()) {
      setState(() {
        _isSaving = true;
      });

      if (_isEditing) {
        final updatedContact = Contact(
          id: widget.initialContact!.id,
          name: _nameController.text,
          phone: _phoneController.text,
          email: _emailController.text,
          notes: _notesController.text,
        );
        await _contactService.updateContact(updatedContact);
      } else {
        await _contactService.addContact(
          name: _nameController.text,
          phone: _phoneController.text,
          email: _emailController.text,
          notes: _notesController.text,
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
    _phoneController.dispose();
    _emailController.dispose();
    _notesController.dispose();
    super.dispose();
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
                  labelText: 'Имя',
                  border: OutlineInputBorder(),
                  prefixIcon: Icon(Icons.person),
                ),
                validator: (value) {
                  if (value == null || value.trim().isEmpty) {
                    return 'Пожалуйста, введите имя';
                  }
                  return null;
                },
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _phoneController,
                decoration: const InputDecoration(
                  labelText: 'Телефон',
                  border: OutlineInputBorder(),
                  prefixIcon: Icon(Icons.phone),
                ),
                keyboardType: TextInputType.phone,
                validator: (value) {
                  if (value == null || value.trim().isEmpty) {
                    return 'Пожалуйста, введите телефон';
                  }
                  return null;
                },
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _emailController,
                decoration: const InputDecoration(
                  labelText: 'Email (необязательно)',
                  border: OutlineInputBorder(),
                  prefixIcon: Icon(Icons.email),
                ),
                keyboardType: TextInputType.emailAddress,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _notesController,
                decoration: const InputDecoration(
                  labelText: 'Заметки (необязательно)',
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
