
import 'package:flutter/material.dart';
import 'package:try_neuro/features/contacts/contact_edit_screen.dart';
import 'package:try_neuro/features/contacts/domain/contact_model.dart';

class ContactDetailScreen extends StatefulWidget {
  final Contact contact;

  const ContactDetailScreen({super.key, required this.contact});

  @override
  State<ContactDetailScreen> createState() => _ContactDetailScreenState();
}

class _ContactDetailScreenState extends State<ContactDetailScreen> {
  late Contact _contact;

  @override
  void initState() {
    super.initState();
    _contact = widget.contact;
  }

  void _navigateToEditScreen() async {
    final result = await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => ContactEditScreen(initialContact: _contact),
      ),
    );

    if (result == true) {
      // Если экран редактирования вернул true, значит данные изменились.
      // Вместо того, чтобы перезагружать с сервера, мы просто закроем этот экран,
      // так как предыдущий экран (список клиентов) все равно обновится.
      if (mounted) {
        // Мы также передаем true назад, чтобы список клиентов обновился
        Navigator.of(context).pop(true);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_contact.name),
        actions: [
          IconButton(
            icon: const Icon(Icons.edit),
            onPressed: _navigateToEditScreen,
            tooltip: 'Редактировать',
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            _buildDetailCard('Телефон', _contact.phone, Icons.phone),
            if (_contact.email != null && _contact.email!.isNotEmpty)
              _buildDetailCard('Email', _contact.email!, Icons.email),
            if (_contact.notes != null && _contact.notes!.isNotEmpty)
              _buildDetailCard('Заметки', _contact.notes!, Icons.note),
          ],
        ),
      ),
    );
  }

  Widget _buildDetailCard(String title, String value, IconData icon) {
    return Card(
      margin: const EdgeInsets.only(bottom: 16.0),
      child: ListTile(
        leading: Icon(icon, size: 40),
        title: Text(title, style: const TextStyle(fontWeight: FontWeight.bold)),
        subtitle: Text(value, style: const TextStyle(fontSize: 16)),
      ),
    );
  }
}
