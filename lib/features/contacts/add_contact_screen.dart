import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:try_neuro/core/utils/phone_utils.dart';
import 'package:try_neuro/features/contacts/data/contact_service.dart';
import 'package:try_neuro/features/contacts/domain/contact_model.dart';
import 'package:try_neuro/service_locator.dart';

class AddContactScreen extends StatefulWidget {
  // --- ИЗМЕНЕНИЕ: Принимаем начальный телефон ---
  final String? initialPhone;
  const AddContactScreen({super.key, this.initialPhone});

  @override
  State<AddContactScreen> createState() => _AddContactScreenState();
}

class _AddContactScreenState extends State<AddContactScreen> {
  final _formKey = GlobalKey<FormState>();
  final _contactService = sl<ContactService>();
  
  final _nameController = TextEditingController();
  late final List<TextEditingController> _phoneControllers;
  final _emailController = TextEditingController();
  final _notesController = TextEditingController();
  
  bool _isSaving = false;

  @override
  void initState() {
    super.initState();
    // Инициализируем контроллер первым номером, если он передан
    _phoneControllers = [
      TextEditingController(
        text: widget.initialPhone != null ? PhoneUtils.format(widget.initialPhone) : null
      )
    ];
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

  Future<void> _saveContact() async {
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

    setState(() => _isSaving = true);
    try {
      final newContact = await _contactService.addContact(
        name: _nameController.text.trim(),
        phones: phones,
        email: _emailController.text.trim().isEmpty ? null : _emailController.text.trim(),
        notes: _notesController.text.trim().isEmpty ? null : _notesController.text.trim(),
      );
      if (mounted) {
        Navigator.of(context).pop(newContact);
      }
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
    return Scaffold(
      appBar: AppBar(
        title: const Text('Новый клиент'),
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
                textCapitalization: TextCapitalization.words,
                decoration: const InputDecoration(
                  labelText: 'Имя фамилия*',
                  prefixIcon: Icon(Icons.person),
                  border: OutlineInputBorder(),
                ),
                validator: (v) => v == null || v.trim().isEmpty ? 'Введите имя' : null,
              ),
              const SizedBox(height: 24),
              
              const Text('ТЕЛЕФОНЫ*', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14, color: Colors.blueGrey)),
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
                            hintText: '+7 (___) ___-__-__',
                            prefixIcon: const Icon(Icons.phone),
                            border: const OutlineInputBorder(),
                          ),
                          keyboardType: TextInputType.phone,
                          inputFormatters: [
                            RussianPhoneInputFormatter(),
                          ],
                          validator: (v) {
                            if (index == 0) {
                              if (v == null || v.trim().isEmpty) return 'Введите телефон';
                              if (PhoneUtils.clean(v).length < 10) return 'Номер слишком короткий';
                            }
                            return null;
                          },
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
                label: const Text('ДОБАВИТЬ НОМЕР'),
              ),
              
              const SizedBox(height: 16),
              TextFormField(
                controller: _emailController,
                decoration: const InputDecoration(
                  labelText: 'Email',
                  prefixIcon: Icon(Icons.email),
                  border: OutlineInputBorder(),
                ),
                keyboardType: TextInputType.emailAddress,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _notesController,
                decoration: const InputDecoration(
                  labelText: 'Заметки',
                  prefixIcon: Icon(Icons.note),
                  border: OutlineInputBorder(),
                ),
                maxLines: 3,
              ),
              const SizedBox(height: 32),
              ElevatedButton(
                onPressed: _isSaving ? null : _saveContact,
                style: ElevatedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 16),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                ),
                child: _isSaving 
                  ? const CircularProgressIndicator(color: Colors.white) 
                  : const Text('СОХРАНИТЬ КЛИЕНТА', style: TextStyle(fontWeight: FontWeight.bold)),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
