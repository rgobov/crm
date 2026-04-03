import 'package:flutter/material.dart';
import 'package:try_neuro/core/utils/phone_utils.dart';
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

  final _firstNameController = TextEditingController();
  final _lastNameController = TextEditingController();
  final _middleNameController = TextEditingController();
  
  final List<TextEditingController> _phoneControllers = [];
  final _emailController = TextEditingController();
  final _notesController = TextEditingController();

  bool _isSaving = false;
  bool get _isEditing => widget.initialContact != null;

  @override
  void initState() {
    super.initState();
    
    if (_isEditing) {
      final parts = widget.initialContact!.name.split(' ');
      if (parts.isNotEmpty) _lastNameController.text = parts[0];
      if (parts.length > 1) _firstNameController.text = parts[1];
      if (parts.length > 2) _middleNameController.text = parts.sublist(2).join(' ');

      if (widget.initialContact!.phones.isNotEmpty) {
        for (var phone in widget.initialContact!.phones) {
          _phoneControllers.add(TextEditingController(text: PhoneUtils.format(phone)));
        }
      }
      _emailController.text = widget.initialContact?.email ?? '';
      _notesController.text = widget.initialContact?.notes ?? '';
    }

    if (_phoneControllers.isEmpty) {
      _phoneControllers.add(TextEditingController());
    }
  }

  @override
  void dispose() {
    _firstNameController.dispose();
    _lastNameController.dispose();
    _middleNameController.dispose();
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
        .map((c) => PhoneUtils.clean(c.text))
        .where((text) => text.isNotEmpty)
        .toList();

    if (phones.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Добавьте хотя бы один номер телефона')),
      );
      return;
    }

    final String fullName = [
      _lastNameController.text.trim(),
      _firstNameController.text.trim(),
      _middleNameController.text.trim(),
    ].where((s) => s.isNotEmpty).join(' ');

    setState(() => _isSaving = true);

    try {
      final contactData = Contact(
        id: _isEditing ? widget.initialContact!.id : '',
        name: fullName,
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

      if (mounted) Navigator.of(context).pop(true);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Ошибка сохранения: ${e.toString()}'), backgroundColor: Colors.red),
        );
      }
    } finally {
      if (mounted) setState(() => _isSaving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    return Scaffold(
      backgroundColor: colorScheme.surfaceVariant.withOpacity(0.3),
      appBar: AppBar(
        title: Text(_isEditing ? 'Изменить клиента' : 'Новый клиент'),
        centerTitle: true,
        elevation: 0,
        backgroundColor: Colors.transparent,
      ),
      body: SafeArea(
        child: Form(
          key: _formKey,
          child: ListView(
            padding: const EdgeInsets.all(24.0),
            children: [
              _buildFormSection(
                title: 'Личные данные',
                icon: Icons.person_outline,
                colorScheme: colorScheme,
                children: [
                  _buildTextField(
                    controller: _lastNameController,
                    label: 'Фамилия *',
                    hint: 'Введите фамилию',
                    validator: (v) => v?.trim().isEmpty ?? true ? 'Введите фамилию' : null,
                  ),
                  const SizedBox(height: 16),
                  _buildTextField(
                    controller: _firstNameController,
                    label: 'Имя *',
                    hint: 'Введите имя',
                    validator: (v) => v?.trim().isEmpty ?? true ? 'Введите имя' : null,
                  ),
                  const SizedBox(height: 16),
                  _buildTextField(
                    controller: _middleNameController,
                    label: 'Отчество',
                    hint: 'Необязательно',
                  ),
                ],
              ),
              const SizedBox(height: 24),
              _buildFormSection(
                title: 'Контакты',
                icon: Icons.contact_phone_outlined,
                colorScheme: colorScheme,
                children: [
                  ..._phoneControllers.asMap().entries.map((entry) {
                    final index = entry.key;
                    final controller = entry.value;
                    return Padding(
                      padding: const EdgeInsets.only(bottom: 12.0),
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Expanded(
                            child: TextFormField(
                              key: ValueKey('edit_phone_field_$index'),
                              controller: controller,
                              decoration: InputDecoration(
                                labelText: index == 0 ? 'Основной телефон' : 'Дополнительный',
                                prefixIcon: const Icon(Icons.phone, size: 20),
                                border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                                filled: true,
                                fillColor: Colors.white,
                              ),
                              keyboardType: TextInputType.phone,
                              // --- ИСПРАВЛЕНО: Используем актуальный форматтер ---
                              inputFormatters: [InternationalPhoneInputFormatter()],
                              validator: (v) {
                                final cleaned = PhoneUtils.clean(v ?? '');
                                if (index == 0 && cleaned.isEmpty) return 'Обязательное поле';
                                if (cleaned.isNotEmpty && cleaned.length < 10) return 'Номер слишком короткий';
                                return null;
                              },
                            ),
                          ),
                          if (_phoneControllers.length > 1)
                            IconButton(
                              icon: Icon(Icons.remove_circle_outline, color: colorScheme.error),
                              onPressed: () => _removePhoneField(index),
                            ),
                        ],
                      ),
                    );
                  }),
                  TextButton.icon(
                    onPressed: _addPhoneField,
                    icon: const Icon(Icons.add),
                    label: const Text('ДОБАВИТЬ НОМЕР'),
                  ),
                  const Divider(height: 32),
                  _buildTextField(
                    controller: _emailController,
                    label: 'Email',
                    hint: 'example@mail.com',
                    keyboardType: TextInputType.emailAddress,
                  ),
                ],
              ),
              const SizedBox(height: 24),
              _buildFormSection(
                title: 'Дополнительно',
                icon: Icons.note_alt_outlined,
                colorScheme: colorScheme, 
                children: [
                  _buildTextField(
                    controller: _notesController,
                    label: 'Заметки',
                    hint: 'Дополнительная информация о клиенте...',
                    maxLines: 3,
                  ),
                ],
              ),
              const SizedBox(height: 40),
              FilledButton(
                onPressed: _isSaving ? null : _saveForm,
                style: FilledButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 18),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                  minimumSize: const Size(double.infinity, 56),
                ),
                child: _isSaving 
                  ? const SizedBox(height: 24, width: 24, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2))
                  : Text(_isEditing ? 'ОБНОВИТЬ КЛИЕНТА' : 'СОЗДАТЬ ЗАПИСЬ', style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
              ),
              const SizedBox(height: 20),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildFormSection({
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

  Widget _buildTextField({
    required TextEditingController controller,
    required String label,
    String? hint,
    TextInputType? keyboardType,
    int maxLines = 1,
    String? Function(String?)? validator,
  }) {
    return TextFormField(
      controller: controller,
      textCapitalization: TextCapitalization.words,
      keyboardType: keyboardType,
      maxLines: maxLines,
      validator: validator,
      decoration: InputDecoration(
        labelText: label,
        hintText: hint,
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
        filled: true,
        fillColor: Colors.white,
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
      ),
    );
  }
}
