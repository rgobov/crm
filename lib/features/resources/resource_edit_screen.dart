
import 'package:flutter/material.dart';
import 'package:try_neuro/features/resources/data/resource_service.dart';
import 'package:try_neuro/features/resources/domain/resource_model.dart';
import 'package:try_neuro/service_locator.dart';

class ResourceEditScreen extends StatefulWidget {
  final Resource? initialResource;

  const ResourceEditScreen({super.key, this.initialResource});

  @override
  State<ResourceEditScreen> createState() => _ResourceEditScreenState();
}

class _ResourceEditScreenState extends State<ResourceEditScreen> {
  final _formKey = GlobalKey<FormState>();
  final _resourceService = sl<ResourceService>();

  late final TextEditingController _nameController;
  late final TextEditingController _descriptionController;

  bool _isSaving = false;
  bool get _isEditing => widget.initialResource != null;

  @override
  void initState() {
    super.initState();
    _nameController = TextEditingController(text: widget.initialResource?.name);
    _descriptionController = TextEditingController(text: widget.initialResource?.description);
  }

  Future<void> _saveForm() async {
    if (_formKey.currentState!.validate()) {
      setState(() {
        _isSaving = true;
      });

      if (_isEditing) {
        final updatedResource = Resource(
          id: widget.initialResource!.id,
          name: _nameController.text,
          description: _descriptionController.text,
        );
        await _resourceService.updateResource(updatedResource);
      } else {
        await _resourceService.addResource(
          name: _nameController.text,
          description: _descriptionController.text,
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
    _descriptionController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_isEditing ? 'Изменить ресурс' : 'Новый ресурс'),
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
                  labelText: 'Название ресурса',
                  border: OutlineInputBorder(),
                  prefixIcon: Icon(Icons.build),
                ),
                validator: (value) => (value == null || value.trim().isEmpty) ? 'Введите название' : null,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _descriptionController,
                decoration: const InputDecoration(
                  labelText: 'Описание (необязательно)',
                  border: OutlineInputBorder(),
                  prefixIcon: Icon(Icons.description),
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
