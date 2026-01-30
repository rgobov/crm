import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:try_neuro/core/session/session_service.dart';
import 'package:try_neuro/core/utils/phone_utils.dart';
import 'package:try_neuro/features/auth/domain/user_model.dart';
import 'package:try_neuro/features/contacts/domain/contact_model.dart';
import 'package:try_neuro/features/admin/data/admin_service.dart';
import 'package:try_neuro/features/schedule/domain/appointment_model.dart';
import 'package:try_neuro/service_locator.dart';

class ContactDetailScreen extends StatefulWidget {
  final Contact contact;
  const ContactDetailScreen({super.key, required this.contact});

  @override
  State<ContactDetailScreen> createState() => _ContactDetailScreenState();
}

class _ContactDetailScreenState extends State<ContactDetailScreen> {
  final _adminService = sl<AdminService>();
  final _sessionService = sl<SessionService>();

  late Contact _contact;
  late Future<List<Appointment>> _historyFuture;
  User? _currentUser;

  // Состояния редактирования
  bool _isEditingName = false;
  final _nameController = TextEditingController();
  final _emailController = TextEditingController();
  final _notesController = TextEditingController();
  int _editingPhoneIdx = -1;
  final _phoneController = TextEditingController();

  @override
  void initState() {
    super.initState();
    _contact = widget.contact;
    _nameController.text = _contact.name;
    _emailController.text = _contact.email ?? '';
    _notesController.text = _contact.notes ?? '';
    _initialize();
  }

  Future<void> _initialize() async {
    _currentUser = await _sessionService.getCurrentUser();
    _loadHistory();
  }

  void _loadHistory() {
    setState(() {
      _historyFuture = _adminService.getContactAppointments(_contact.id);
    });
  }

  Future<void> _saveAll() async {
    final updated = Contact(
      id: _contact.id,
      name: _nameController.text.trim(),
      phones: [..._contact.phones],
      email: _emailController.text.trim().isEmpty ? null : _emailController.text.trim(),
      notes: _notesController.text.trim().isEmpty ? null : _notesController.text.trim(),
    );

    if (_editingPhoneIdx != -1) {
      updated.phones[_editingPhoneIdx] = PhoneUtils.clean(_phoneController.text);
    }

    try {
      final saved = await _adminService.updateContact(updated);
      setState(() {
        _contact = saved;
        _isEditingName = false;
        _editingPhoneIdx = -1;
      });
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Ошибка: $e')));
    }
  }

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 2,
      child: Scaffold(
        appBar: AppBar(
          title: _isEditingName
            ? TextField(
                controller: _nameController,
                autofocus: true,
                style: const TextStyle(color: Colors.white),
                decoration: const InputDecoration(hintText: 'Имя клиента'),
                onSubmitted: (_) => _saveAll(),
              )
            : GestureDetector(
                onTap: () => setState(() => _isEditingName = true),
                child: Text(_contact.name),
              ),
          actions: [
            if (_isEditingName) IconButton(icon: const Icon(Icons.check), onPressed: _saveAll)
          ],
          bottom: const TabBar(tabs: [Tab(text: 'ИНФО'), Tab(text: 'ИСТОРИЯ')]),
        ),
        body: TabBarView(children: [_buildInfoTab(), _buildHistoryTab()]),
      ),
    );
  }

  Widget _buildInfoTab() {
    return ListView(
      padding: const EdgeInsets.all(16.0),
      children: [
        const Text('КОНТАКТНЫЕ НОМЕРА', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: Colors.grey)),
        const SizedBox(height: 8),
        ..._contact.phones.asMap().entries.map((entry) {
          int idx = entry.key;
          String phone = entry.value;
          bool isEditing = _editingPhoneIdx == idx;

          return Card(
            child: ListTile(
              leading: const Icon(Icons.phone, color: Colors.blue),
              title: isEditing
                ? TextField(controller: _phoneController, autofocus: true, keyboardType: TextInputType.phone)
                : Text(PhoneUtils.format(phone)),
              trailing: isEditing
                ? Row(mainAxisSize: MainAxisSize.min, children: [
                    IconButton(icon: const Icon(Icons.close), onPressed: () => setState(() => _editingPhoneIdx = -1)),
                    IconButton(icon: const Icon(Icons.check, color: Colors.green), onPressed: _saveAll),
                  ])
                : IconButton(icon: const Icon(Icons.edit, size: 18), onPressed: () {
                    setState(() {
                      _editingPhoneIdx = idx;
                      _phoneController.text = PhoneUtils.format(phone);
                    });
                  }),
            ),
          );
        }),

        const SizedBox(height: 24),
        _buildEditableField('EMAIL', _emailController, Icons.email),

        const SizedBox(height: 24),
        _buildEditableField('ЗАМЕТКИ', _notesController, Icons.notes, maxLines: 3),
      ],
    );
  }

  Widget _buildEditableField(String label, TextEditingController ctrl, IconData icon, {int maxLines = 1}) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: Colors.grey)),
        Card(
          child: ListTile(
            leading: Icon(icon, color: Colors.blue),
            title: TextField(
              controller: ctrl,
              maxLines: maxLines,
              decoration: const InputDecoration(border: InputBorder.none),
              onSubmitted: (_) => _saveAll(),
            ),
            trailing: IconButton(icon: const Icon(Icons.save_outlined, size: 18), onPressed: _saveAll),
          ),
        ),
      ],
    );
  }

  Widget _buildHistoryTab() {
    return FutureBuilder<List<Appointment>>(
      future: _historyFuture,
      builder: (context, snapshot) {
        if (!snapshot.hasData) return const Center(child: CircularProgressIndicator());
        final history = snapshot.data!;
        if (history.isEmpty) return const Center(child: Text('История пуста'));
        return ListView.builder(
          itemCount: history.length,
          itemBuilder: (context, i) => ListTile(
            title: Text(history[i].service),
            subtitle: Text(DateFormat('dd.MM.yyyy HH:mm').format(history[i].startTime.toLocal())),
          ),
        );
      },
    );
  }
}
