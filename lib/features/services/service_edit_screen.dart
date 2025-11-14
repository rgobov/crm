
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:try_neuro/features/services/data/app_service.dart';
import 'package:try_neuro/features/services/domain/service_model.dart';
import 'package:try_neuro/service_locator.dart';

class ServiceEditScreen extends StatefulWidget {
  final Service? initialService;

  const ServiceEditScreen({super.key, this.initialService});

  @override
  State<ServiceEditScreen> createState() => _ServiceEditScreenState();
}

class _ServiceEditScreenState extends State<ServiceEditScreen> {
  final _formKey = GlobalKey<FormState>();
  final _appService = sl<AppService>();

  late final TextEditingController _nameController;
  late final TextEditingController _durationController;

  bool _isSaving = false;
  bool get _isEditing => widget.initialService != null;

  @override
  void initState() {
    super.initState();
    _nameController = TextEditingController(text: widget.initialService?.name);
    _durationController = TextEditingController(text: widget.initialService?.durationInMinutes.toString());
  }

  Future<void> _saveForm() async {
    if (_formKey.currentState!.validate()) {
      setState(() {
        _isSaving = true;
      });

      final duration = int.tryParse(_durationController.text) ?? 0;

      if (_isEditing) {
        final updatedService = Service(
          id: widget.initialService!.id,
          name: _nameController.text,
          durationInMinutes: duration,
        );
        await _appService.updateService(updatedService);
      } else {
        await _appService.addService(
          name: _nameController.text,
          durationInMinutes: duration,
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
    _durationController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_isEditing ? 'Изменить услугу' : 'Новая услуга'),
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
                  labelText: 'Название услуги',
                  border: OutlineInputBorder(),
                  prefixIcon: Icon(Icons.cut),
                ),
                validator: (value) => (value == null || value.trim().isEmpty) ? 'Введите название' : null,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _durationController,
                decoration: const InputDecoration(
                  labelText: 'Длительность (в минутах)',
                  border: OutlineInputBorder(),
                  prefixIcon: Icon(Icons.timer),
                ),
                keyboardType: TextInputType.number,
                inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                validator: (value) {
                  if (value == null || value.isEmpty) return 'Укажите длительность';
                  if (int.tryParse(value) == null) return 'Введите число';
                  if (int.parse(value) <= 0) return 'Длительность должна быть больше 0';
                  return null;
                },
              ),
            ],
          ),
        ),
      ),
    );
  }
}
