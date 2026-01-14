import 'package:flutter/material.dart';
import 'package:try_neuro/core/session/session_service.dart';
import 'package:try_neuro/core/utils/phone_utils.dart';
import 'package:try_neuro/features/auth/domain/user_model.dart';
import 'package:try_neuro/features/contacts/add_contact_screen.dart';
import 'package:try_neuro/features/contacts/contact_detail_screen.dart';
import 'package:try_neuro/features/contacts/data/contact_service.dart';
import 'package:try_neuro/features/contacts/domain/contact_model.dart';
import 'package:try_neuro/service_locator.dart';

class ContactsScreen extends StatefulWidget {
  const ContactsScreen({super.key});

  @override
  State<ContactsScreen> createState() => _ContactsScreenState();
}

class _ContactsScreenState extends State<ContactsScreen> {
  final _contactService = sl<ContactService>();
  final _sessionService = sl<SessionService>();
  final _searchController = TextEditingController();
  
  List<Contact> _contacts = [];
  bool _isLoading = true;
  User? _currentUser;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      _currentUser = await _sessionService.getCurrentUser();
      final contacts = await _contactService.getContacts(query: _searchController.text);
      if (mounted) {
        setState(() {
          _contacts = contacts;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isLoading = false);
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Ошибка: $e')));
      }
    }
  }

  void _navigateToAddContact() async {
    final result = await Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => const AddContactScreen()),
    );
    if (result != null) {
      _loadData();
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    return Scaffold(
      backgroundColor: colorScheme.surfaceVariant.withOpacity(0.2),
      appBar: AppBar(
        title: const Text('Клиенты'),
        centerTitle: true,
        elevation: 0,
        backgroundColor: Colors.transparent,
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 16),
            child: TextField(
              controller: _searchController,
              decoration: InputDecoration(
                hintText: 'Поиск по имени или телефону...',
                prefixIcon: const Icon(Icons.search),
                suffixIcon: _searchController.text.isNotEmpty 
                  ? IconButton(icon: const Icon(Icons.clear), onPressed: () { _searchController.clear(); _loadData(); }) 
                  : null,
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(16)),
                filled: true,
                fillColor: Colors.white,
              ),
              onChanged: (v) => _loadData(),
            ),
          ),
          Expanded(
            child: _isLoading
                ? const Center(child: CircularProgressIndicator())
                : _contacts.isEmpty
                    ? Center(
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Icon(Icons.people_outline, size: 64, color: colorScheme.outline),
                            const SizedBox(height: 16),
                            const Text('Клиенты не найдены'),
                          ],
                        ),
                      )
                    : ListView.builder(
                        padding: const EdgeInsets.symmetric(horizontal: 16),
                        itemCount: _contacts.length,
                        itemBuilder: (context, index) {
                          final contact = _contacts[index];
                          return Card(
                            elevation: 0,
                            margin: const EdgeInsets.only(bottom: 12),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(16),
                              side: BorderSide(color: colorScheme.outlineVariant),
                            ),
                            child: ListTile(
                              contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                              leading: CircleAvatar(
                                backgroundColor: colorScheme.primaryContainer,
                                foregroundColor: colorScheme.onPrimaryContainer,
                                child: Text(contact.name.isNotEmpty ? contact.name[0].toUpperCase() : '?'),
                              ),
                              title: Text(contact.name, style: const TextStyle(fontWeight: FontWeight.bold)),
                              subtitle: Text(
                                contact.phones.isNotEmpty ? PhoneUtils.format(contact.phones.first) : 'Нет телефона',
                                style: TextStyle(color: colorScheme.onSurfaceVariant),
                              ),
                              trailing: const Icon(Icons.chevron_right),
                              onTap: () async {
                                final result = await Navigator.push(
                                  context,
                                  MaterialPageRoute(builder: (context) => ContactDetailScreen(contact: contact)),
                                );
                                if (result == true) _loadData();
                              },
                            ),
                          );
                        },
                      ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _navigateToAddContact,
        icon: const Icon(Icons.person_add),
        label: const Text('Новый клиент'),
      ),
    );
  }
}
