import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:try_neuro/core/utils/phone_utils.dart';
import 'package:try_neuro/features/contacts/data/contact_service.dart';
import 'package:try_neuro/service_locator.dart';

class AddContactScreen extends StatefulWidget {
  final String? initialPhone;
  const AddContactScreen({super.key, this.initialPhone});

  @override
  State<AddContactScreen> createState() => _AddContactScreenState();
}

class _AddContactScreenState extends State<AddContactScreen> {
  final _formKey = GlobalKey<FormState>();
  final _contactService = sl<ContactService>();
  
  final _nameController = TextEditingController();
  final List<TextEditingController> _phoneControllers = [];
  final _emailController = TextEditingController();
  final _notesController = TextEditingController();
  
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    _phoneControllers.add(TextEditingController(text: widget.initialPhone));
  }

  @override
  void dispose() {
    _nameController.dispose();
    for (var c in _phoneControllers) {
      c.dispose();
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

  Future<void> _save() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() => _isLoading = true);
    try {
      final phones = _phoneControllers
          .map((c) => PhoneUtils.clean(c.text))
          .where((p) => p.isNotEmpty)
          .toList();

      final contact = await _contactService.addContact(
        name: _nameController.text.trim(),
        phones: phones,
        email: _emailController.text.trim().isEmpty ? null : _emailController.text.trim(),
        notes: _notesController.text.trim().isEmpty ? null : _notesController.text.trim(),
      );

      if (mounted) {
        Navigator.of(context).pop(contact);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Ошибка: $e'), backgroundColor: Colors.red),
        );
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Новый клиент')),
      body: _isLoading 
        ? const Center(child: CircularProgressIndicator())
        : Form(
            key: _formKey,
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                children: [
                  TextFormField(
                    controller: _nameController,
                    decoration: const InputDecoration(labelText: 'Имя*', border: OutlineInputBorder()),
                    validator: (v) => v == null || v.isEmpty ? 'Введите имя' : null,
                  ),
                  const SizedBox(height: 16),
                  ..._phoneControllers.asMap().entries.map((entry) {
                    return Padding(
                      padding: const EdgeInsets.only(bottom: 8.0),
                      child: TextFormField(
                        controller: entry.value,
                        decoration: InputDecoration(
                          labelText: 'Телефон ${entry.key + 1}*',
                          border: const OutlineInputBorder(),
                          suffixIcon: entry.key > 0 
                            ? IconButton(icon: const Icon(Icons.remove_circle_outline), onPressed: () => setState(() => _phoneControllers.removeAt(entry.key)))
                            : null,
                        ),
                        keyboardType: TextInputType.phone,
                        // --- ИЗМЕНЕНИЕ: Международный форматтер ---
                        inputFormatters: [InternationalPhoneInputFormatter()],
                        validator: (v) => v == null || v.isEmpty ? 'Введите телефон' : null,
                      ),
                    );
                  }),
                  TextButton.icon(
                    onPressed: _addPhoneField, 
                    icon: const Icon(Icons.add), 
                    label: const Text('Добавить телефон'),
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _emailController,
                    decoration: const InputDecoration(labelText: 'Email', border: OutlineInputBorder()),
                    keyboardType: TextInputType.emailAddress,
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _notesController,
                    decoration: const InputDecoration(labelText: 'Заметки', border: OutlineInputBorder()),
                    maxLines: 3,
                  ),
                  const SizedBox(height: 32),
                  ElevatedButton(
                    onPressed: _save,
                    style: ElevatedButton.styleFrom(minimumSize: const Size(double.infinity, 50)),
                    child: const Text('СОХРАНИТЬ'),
                  ),
                ],
              ),
            ),
          ),
    );
  }
}
