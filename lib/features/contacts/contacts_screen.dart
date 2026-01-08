import 'dart:async';
import 'package:flutter/material.dart';
import 'package:try_neuro/features/contacts/contact_edit_screen.dart';
import 'package:try_neuro/features/contacts/contact_detail_screen.dart';
import 'package:try_neuro/features/contacts/data/contact_service.dart';
import 'package:try_neuro/features/contacts/domain/contact_model.dart';
import 'package:try_neuro/service_locator.dart';

class ContactsScreen extends StatefulWidget {
  const ContactsScreen({super.key});

  @override
  State<ContactsScreen> createState() => _ContactsScreenState();
}

class _ContactsScreenState extends State<ContactsScreen> with RouteAware {
  final ContactService _contactService = sl<ContactService>();
  final _searchController = TextEditingController();
  Timer? _debounce;

  List<Contact> _contacts = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadContacts();
    _searchController.addListener(_onSearchChanged);
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _loadContacts(query: _searchController.text, silent: true);
  }

  @override
  void dispose() {
    _searchController.removeListener(_onSearchChanged);
    _searchController.dispose();
    _debounce?.cancel();
    super.dispose();
  }

  Future<void> _loadContacts({String? query, bool silent = false}) async {
    if (!silent) {
      setState(() {
        _isLoading = true;
      });
    }
    try {
      final contacts = await _contactService.getContacts(query: query);
      if (mounted) {
        setState(() {
          _contacts = contacts;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isLoading = false);
        if (!silent) {
          ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Ошибка загрузки: ${e.toString()}')));
        }
      }
    }
  }

  void _onSearchChanged() {
    if (_debounce?.isActive ?? false) _debounce!.cancel();
    _debounce = Timer(const Duration(milliseconds: 500), () {
      _loadContacts(query: _searchController.text);
    });
  }

  void _navigateToEditScreen({Contact? contact}) async {
    final result = await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => ContactEditScreen(initialContact: contact),
      ),
    );
    if (result != null) {
      _loadContacts(query: _searchController.text);
    }
  }

  void _navigateToDetailScreen(Contact contact) async {
    final result = await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => ContactDetailScreen(contact: contact),
      ),
    );
    if (result == true) {
      _loadContacts(query: _searchController.text);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Клиенты'),
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(12.0),
            child: TextField(
              controller: _searchController,
              decoration: InputDecoration(
                hintText: 'Поиск по имени или телефону...',
                prefixIcon: const Icon(Icons.search),
                suffixIcon: _searchController.text.isNotEmpty
                    ? IconButton(
                        icon: const Icon(Icons.clear),
                        onPressed: () => _searchController.clear(),
                      )
                    : null,
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
                filled: true,
                fillColor: Colors.grey.shade100,
              ),
            ),
          ),
          Expanded(
            child: _isLoading && _contacts.isEmpty 
                ? const Center(child: CircularProgressIndicator())
                : RefreshIndicator(
                    onRefresh: () => _loadContacts(query: _searchController.text),
                    // Оборачиваем только список в SelectionArea
                    child: SelectionArea(
                      child: _contacts.isEmpty
                          ? Center(
                              child: Text(_searchController.text.isEmpty 
                                  ? 'Список клиентов пуст' 
                                  : 'Клиенты не найдены'))
                          : ListView.builder(
                              itemCount: _contacts.length,
                              itemBuilder: (context, index) {
                                final contact = _contacts[index];
                                return ListTile(
                                  leading: CircleAvatar(
                                    backgroundColor: Theme.of(context).primaryColor.withOpacity(0.1),
                                    child: Text(contact.name.isNotEmpty ? contact.name[0].toUpperCase() : '?'),
                                  ),
                                  title: Text(contact.name, style: const TextStyle(fontWeight: FontWeight.bold)),
                                  subtitle: Text(contact.phone),
                                  trailing: const Icon(Icons.chevron_right),
                                  onTap: () => _navigateToDetailScreen(contact),
                                );
                              },
                            ),
                    ),
                  ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        heroTag: 'contacts_fab',
        onPressed: () => _navigateToEditScreen(),
        tooltip: 'Добавить клиента',
        child: const Icon(Icons.add),
      ),
    );
  }
}
