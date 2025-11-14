
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

class _ContactsScreenState extends State<ContactsScreen> {
  final ContactService _contactService = sl<ContactService>();
  final _searchController = TextEditingController();
  Timer? _debounce;

  List<Contact> _contacts = [];
  bool _isLoading = true;
  bool _isSearching = false;

  @override
  void initState() {
    super.initState();
    _loadContacts();
    _searchController.addListener(_onSearchChanged);
  }

  @override
  void dispose() {
    _searchController.removeListener(_onSearchChanged);
    _searchController.dispose();
    _debounce?.cancel();
    super.dispose();
  }

  Future<void> _loadContacts({String? query}) async {
    setState(() {
      _isLoading = true;
    });
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
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.toString())));
      }
    }
  }

  void _onSearchChanged() {
    if (_debounce?.isActive ?? false) _debounce!.cancel();
    _debounce = Timer(const Duration(milliseconds: 300), () {
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
    if (result == true) {
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

  AppBar _buildAppBar() {
    if (_isSearching) {
      return AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () {
            setState(() {
              _isSearching = false;
            });
            _searchController.clear(); // Это вызовет _onSearchChanged
          },
        ),
        title: TextField(
          controller: _searchController,
          autofocus: true,
          decoration: const InputDecoration(
            hintText: 'Поиск...',
            border: InputBorder.none,
          ),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.clear),
            onPressed: () {
              if (_searchController.text.isNotEmpty) {
                _searchController.clear();
              }
            },
          )
        ],
      );
    } else {
      return AppBar(
        title: const Text('Клиенты'),
        actions: [
          IconButton(
            icon: const Icon(Icons.search),
            onPressed: () {
              setState(() {
                _isSearching = true;
              });
            },
          )
        ],
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: _buildAppBar(),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: () => _loadContacts(query: _searchController.text),
              child: _contacts.isEmpty
                  ? Center(child: Text(_searchController.text.isEmpty ? 'Нет клиентов' : 'Клиенты не найдены'))
                  : ListView.builder(
                      itemCount: _contacts.length,
                      itemBuilder: (context, index) {
                        final contact = _contacts[index];
                        return ListTile(
                          leading: CircleAvatar(child: Text(contact.name.isNotEmpty ? contact.name[0] : '?')),
                          title: Text(contact.name),
                          subtitle: Text(contact.phone),
                          onTap: () => _navigateToDetailScreen(contact),
                        );
                      },
                    ),
            ),
      floatingActionButton: FloatingActionButton(
        heroTag: 'contacts_fab',
        onPressed: _navigateToEditScreen,
        tooltip: 'Добавить клиента',
        child: const Icon(Icons.add),
      ),
    );
  }
}
