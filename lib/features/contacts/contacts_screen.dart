import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:try_neuro/core/utils/phone_utils.dart';
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
  final _searchController = TextEditingController();
  final _scrollController = ScrollController();
  Timer? _debounce;
  
  List<Contact> _contacts = [];
  bool _isLoading = false;
  bool _isLoadMore = false;
  int _currentPage = 0;
  bool _isLastPage = false;
  String _currentQuery = '';

  @override
  void initState() {
    super.initState();
    _loadInitialData();
    _scrollController.addListener(_onScroll);
  }

  @override
  void dispose() {
    _debounce?.cancel();
    _scrollController.dispose();
    _searchController.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (_scrollController.position.pixels >= _scrollController.position.maxScrollExtent - 300) {
      if (!_isLoading && !_isLoadMore && !_isLastPage) {
        _loadMoreData();
      }
    }
  }

  void _onSearchChanged(String query) {
    if (_debounce?.isActive ?? false) _debounce!.cancel();
    _debounce = Timer(const Duration(milliseconds: 600), () {
      if (_currentQuery != query) {
        _loadInitialData();
      }
    });
  }

  Future<void> _loadInitialData() async {
    if (!mounted) return;
    setState(() {
      _isLoading = true;
      _currentPage = 0;
      _isLastPage = false;
      _currentQuery = _searchController.text;
    });
    
    try {
      final searchQuery = _isPhoneQuery(_currentQuery) 
          ? PhoneUtils.clean(_currentQuery) 
          : _currentQuery;

      final result = await _contactService.getContactsPaged(
        query: searchQuery,
        page: 0,
        size: 25,
      );
      
      if (mounted) {
        setState(() {
          _contacts = List<Contact>.from(result['contacts']);
          _isLastPage = result['isLast'] as bool;
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

  Future<void> _loadMoreData() async {
    if (_isLoadMore || _isLastPage) return;
    setState(() => _isLoadMore = true);
    
    try {
      final nextPage = _currentPage + 1;
      final searchQuery = _isPhoneQuery(_currentQuery) 
          ? PhoneUtils.clean(_currentQuery) 
          : _currentQuery;

      final result = await _contactService.getContactsPaged(
        query: searchQuery,
        page: nextPage,
        size: 25,
      );
      
      if (mounted) {
        final List<Contact> newContacts = result['contacts'];
        setState(() {
          for (var contact in newContacts) {
            if (!_contacts.any((existing) => existing.id == contact.id)) {
              _contacts.add(contact);
            }
          }
          _isLastPage = result['isLast'] as bool;
          _currentPage = nextPage;
          _isLoadMore = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isLoadMore = false);
      }
    }
  }

  bool _isPhoneQuery(String v) => v.contains(RegExp(r'[0-9]'));

  void _navigateToAddContact() async {
    final result = await Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => const AddContactScreen()),
    );
    if (result != null) {
      _loadInitialData();
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;

    return Scaffold(
      backgroundColor: colorScheme.surface,
      appBar: AppBar(
        title: const Text('Клиенты'),
        centerTitle: true,
        backgroundColor: Colors.transparent,
        elevation: 0,
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: TextField(
              controller: _searchController,
              // Переключаем тип клавиатуры динамически
              keyboardType: _isPhoneQuery(_searchController.text) ? TextInputType.phone : TextInputType.text,
              inputFormatters: [
                // Если ввод похож на номер телефона, применяем маску
                if (_isPhoneQuery(_searchController.text)) RussianPhoneInputFormatter(),
              ],
              decoration: InputDecoration(
                hintText: 'Имя или +7 (___) ___-__-__',
                prefixIcon: const Icon(Icons.person_search_outlined),
                suffixIcon: _searchController.text.isNotEmpty 
                  ? IconButton(
                      icon: const Icon(Icons.cancel_rounded), 
                      onPressed: () { 
                        _searchController.clear(); 
                        _loadInitialData(); 
                      }
                    ) 
                  : null,
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(16),
                  borderSide: BorderSide(color: colorScheme.outlineVariant),
                ),
                filled: true,
                fillColor: colorScheme.surfaceVariant.withOpacity(0.3),
                contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
              ),
              onChanged: (v) {
                // Маленький хак: заставляем TextField перерисоваться для смены клавиатуры/форматтера
                setState(() {});
                _onSearchChanged(v);
              },
            ),
          ),

          Expanded(
            child: _isLoading
                ? const Center(child: CircularProgressIndicator())
                : RefreshIndicator(
                    onRefresh: _loadInitialData,
                    child: _contacts.isEmpty
                        ? _buildEmptyState(colorScheme)
                        : ListView.separated(
                            controller: _scrollController,
                            padding: const EdgeInsets.fromLTRB(16, 8, 16, 100),
                            itemCount: _contacts.length + 1,
                            separatorBuilder: (_, __) => const SizedBox(height: 8),
                            itemBuilder: (context, index) {
                              if (index == _contacts.length) {
                                return _buildFooter();
                              }
                              
                              final contact = _contacts[index];
                              return _buildContactTile(contact, colorScheme);
                            },
                          ),
                  ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _navigateToAddContact,
        icon: const Icon(Icons.person_add_rounded),
        label: const Text('Новый клиент'),
        elevation: 2,
      ),
    );
  }

  Widget _buildContactTile(Contact contact, ColorScheme colorScheme) {
    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(color: colorScheme.outlineVariant.withOpacity(0.5)),
      ),
      child: ListTile(
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
        leading: CircleAvatar(
          radius: 24,
          backgroundColor: colorScheme.primaryContainer.withOpacity(0.7),
          foregroundColor: colorScheme.onPrimaryContainer,
          child: Text(
            contact.name.isNotEmpty ? contact.name[0].toUpperCase() : '?',
            style: const TextStyle(fontWeight: FontWeight.bold),
          ),
        ),
        title: Text(contact.name, style: const TextStyle(fontWeight: FontWeight.bold)),
        subtitle: Text(
          contact.phones.isNotEmpty ? PhoneUtils.format(contact.phones.first) : 'Нет телефона',
          style: TextStyle(color: colorScheme.onSurfaceVariant),
        ),
        trailing: Icon(Icons.arrow_forward_ios, size: 14, color: colorScheme.outline),
        onTap: () async {
          final result = await Navigator.push(
            context,
            MaterialPageRoute(builder: (context) => ContactDetailScreen(contact: contact)),
          );
          if (result == true) _loadInitialData();
        },
      ),
    );
  }

  Widget _buildEmptyState(ColorScheme colorScheme) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.person_search_rounded, size: 80, color: colorScheme.outline.withOpacity(0.3)),
          const SizedBox(height: 16),
          Text(
            'Клиенты не найдены',
            style: TextStyle(fontSize: 16, color: colorScheme.outline, fontWeight: FontWeight.w500),
          ),
        ],
      ),
    );
  }

  Widget _buildFooter() {
    if (_isLastPage) {
      return Padding(
        padding: const EdgeInsets.symmetric(vertical: 32),
        child: Center(
          child: Text(
            'Всего: ${_contacts.length}',
            style: TextStyle(color: Colors.grey.shade500, fontSize: 12, letterSpacing: 1.1),
          ),
        ),
      );
    }
    return const Padding(
      padding: EdgeInsets.symmetric(vertical: 32),
      child: Center(child: CircularProgressIndicator(strokeWidth: 2)),
    );
  }
}
