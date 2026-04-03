import 'package:flutter/material.dart';
import 'package:try_neuro/features/staff/data/staff_service.dart';
import 'package:try_neuro/features/staff/domain/staff_member_model.dart';
import 'package:try_neuro/features/staff/staff_edit_screen.dart';
import 'package:try_neuro/service_locator.dart';
import 'dart:async';

class StaffScreen extends StatefulWidget {
  const StaffScreen({super.key});

  @override
  State<StaffScreen> createState() => _StaffScreenState();
}

class _StaffScreenState extends State<StaffScreen> {
  final StaffService _staffService = sl<StaffService>();
  final TextEditingController _searchController = TextEditingController();

  List<StaffMember> _staff = [];
  bool _isLoading = true;
  int _totalElements = 0;
  Timer? _debounce;

  @override
  void initState() {
    super.initState();
    _loadStaff();
  }

  @override
  void dispose() {
    _searchController.dispose();
    _debounce?.cancel();
    super.dispose();
  }

  // Логика "умного" поиска как в Svelte
  void _onSearchChanged(String query) {
    if (_debounce?.isActive ?? false) _debounce!.cancel();
    _debounce = Timer(const Duration(milliseconds: 500), () {
      final isPhone = RegExp(r'^\d+$').hasMatch(query);
      if (query.isEmpty || (isPhone && query.length >= 6) || (!isPhone && query.length >= 2)) {
        _loadStaff(query: query);
      }
    });
  }

  Future<void> _loadStaff({String? query}) async {
    setState(() => _isLoading = true);
    try {
      final result = await _staffService.getStaffPaged(query: query);
      setState(() {
        _staff = result['members'];
        _totalElements = result['totalElements'];
        _isLoading = false;
      });
    } catch (e) {
      setState(() => _isLoading = false);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Ошибка загрузки: $e')));
      }
    }
  }

  void _navigateToEditScreen({StaffMember? staffMember}) async {
    final result = await Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => StaffEditScreen(staffMember: staffMember)),
    );
    if (result == true) _loadStaff();
  }

  void _deleteStaffMember(String staffMemberId) async {
     final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Подтверждение'),
        content: const Text('Вы уверены, что хотите удалить этого сотрудника?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Отмена')),
          TextButton(onPressed: () => Navigator.pop(context, true), child: const Text('Удалить', style: TextStyle(color: Colors.red))),
        ],
      ),
    );

    if (confirmed == true) {
      await _staffService.deleteStaffMember(staffMemberId);
      _loadStaff();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Персонал'),
            Text('Всего: $_totalElements', style: const TextStyle(fontSize: 12)),
          ],
        ),
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: TextField(
              controller: _searchController,
              onChanged: _onSearchChanged,
              decoration: InputDecoration(
                hintText: 'Поиск (от 2 букв или 6 цифр)...',
                prefixIcon: const Icon(Icons.search),
                suffixIcon: _searchController.text.isNotEmpty
                  ? IconButton(icon: const Icon(Icons.clear), onPressed: () {
                      _searchController.clear();
                      _loadStaff();
                    })
                  : null,
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(16)),
                filled: true,
                fillColor: Colors.grey[100],
              ),
            ),
          ),
          Expanded(
            child: _isLoading && _staff.isEmpty
                ? const Center(child: CircularProgressIndicator())
                : _staff.isEmpty
                    ? const Center(child: Text('Ничего не найдено'))
                    : RefreshIndicator(
                        onRefresh: () => _loadStaff(query: _searchController.text),
                        child: ListView.builder(
                          itemCount: _staff.length,
                          itemBuilder: (context, index) {
                            final member = _staff[index];
                            return Card(
                              margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
                              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                              child: ListTile(
                                leading: CircleAvatar(
                                  backgroundColor: Theme.of(context).primaryColor.withOpacity(0.1),
                                  child: Text(member.name[0].toUpperCase(), style: TextStyle(color: Theme.of(context).primaryColor)),
                                ),
                                title: Text(member.name, style: const TextStyle(fontWeight: FontWeight.bold)),
                                subtitle: Text(member.specialty),
                                trailing: IconButton(
                                  icon: const Icon(Icons.delete_outline, color: Colors.redAccent),
                                  onPressed: () => _deleteStaffMember(member.id),
                                ),
                                onTap: () => _navigateToEditScreen(staffMember: member),
                              ),
                            );
                          },
                        ),
                      ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _navigateToEditScreen(),
        child: const Icon(Icons.add),
      ),
    );
  }
}
