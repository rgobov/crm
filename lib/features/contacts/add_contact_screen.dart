import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:try_neuro/core/utils/keyboard_utils.dart'; // Добавили импорт
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

  // FocusNodes для полей
  final _nameFocusNode = FocusNode();
  final List<FocusNode> _phoneFocusNodes = [];
  final _emailFocusNode = FocusNode();
  final _notesFocusNode = FocusNode();
  
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    _phoneControllers.add(TextEditingController(text: widget.initialPhone));
    _phoneFocusNodes.add(FocusNode());
  }

  @override
  void dispose() {
    _nameController.dispose();
    _nameFocusNode.dispose();
    for (var c in _phoneControllers) {
      c.dispose();
    }
    for (var f in _phoneFocusNodes) {
      f.dispose();
    }
    _emailController.dispose();
    _emailFocusNode.dispose();
    _notesController.dispose();
    _notesFocusNode.dispose();
    super.dispose();
  }

  void _addPhoneField() {
    setState(() {
      _phoneControllers.add(TextEditingController());
      _phoneFocusNodes.add(FocusNode());
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
                    focusNode: _nameFocusNode,
                    onTap: () => KeyboardUtils.onTextFieldTap(_nameFocusNode), // ХАК
                    decoration: const InputDecoration(labelText: 'Имя*', border: OutlineInputBorder()),
                    validator: (v) => v == null || v.isEmpty ? 'Введите имя' : null,
                  ),
                  const SizedBox(height: 16),
                  ..._phoneControllers.asMap().entries.map((entry) {
                    final index = entry.key;
                    return Padding(
                      padding: const EdgeInsets.only(bottom: 8.0),
                      child: TextFormField(
                        controller: entry.value,
                        focusNode: _phoneFocusNodes[index],
                        onTap: () => KeyboardUtils.onTextFieldTap(_phoneFocusNodes[index]), // ХАК
                        decoration: InputDecoration(
                          labelText: 'Телефон ${index + 1}*',
                          border: const OutlineInputBorder(),
                          suffixIcon: index > 0 
                            ? IconButton(icon: const Icon(Icons.remove_circle_outline), onPressed: () => setState(() {
                                _phoneControllers.removeAt(index);
                                _phoneFocusNodes.removeAt(index);
                              }))
                            : null,
                        ),
                        keyboardType: TextInputType.phone,
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
                    focusNode: _emailFocusNode,
                    onTap: () => KeyboardUtils.onTextFieldTap(_emailFocusNode), // ХАК
                    decoration: const InputDecoration(labelText: 'Email', border: OutlineInputBorder()),
                    keyboardType: TextInputType.emailAddress,
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _notesController,
                    focusNode: _notesFocusNode,
                    onTap: () => KeyboardUtils.onTextFieldTap(_notesFocusNode), // ХАК
                    decoration: const InputDecoration(labelText: 'Заметки', border: OutlineInputBorder()),
                    maxLines: 3,
                  ),
                  const SizedBox(height: 32),
                  ElevatedButton(
                    onPressed: _save,
                    child: const Text('СОХРАНИТЬ'),
                  ),
                ],
              ),
            ),
          ),
    );
  }
}
