
import 'package:flutter/material.dart';
import 'package:try_neuro/features/staff/data/staff_service.dart';
import 'package:try_neuro/features/staff/domain/staff_member_model.dart';
import 'package:try_neuro/features/staff/staff_edit_screen.dart';
import 'package:try_neuro/service_locator.dart';

class StaffScreen extends StatefulWidget {
  const StaffScreen({super.key});

  @override
  State<StaffScreen> createState() => _StaffScreenState();
}

class _StaffScreenState extends State<StaffScreen> {
  final StaffService _staffService = sl<StaffService>();
  late Future<List<StaffMember>> _staffFuture;

  @override
  void initState() {
    super.initState();
    _loadStaff();
  }

  void _loadStaff() {
    setState(() {
      _staffFuture = _staffService.getStaff();
    });
  }

  void _navigateToEditScreen({StaffMember? staffMember}) async {
    final result = await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => StaffEditScreen(initialStaffMember: staffMember),
      ),
    );
    if (result == true) {
      _loadStaff();
    }
  }

  void _deleteStaffMember(String staffMemberId) async {
     final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Подтверждение'),
        content: const Text('Вы уверены, что хотите удалить этого сотрудника?'),
        actions: [
          TextButton(onPressed: () => Navigator.of(context).pop(false), child: const Text('Отмена')),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('Удалить', style: TextStyle(color: Colors.red)),
          ),
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
        title: const Text('Персонал'),
      ),
      body: FutureBuilder<List<StaffMember>>(
        future: _staffFuture,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError) {
            return Center(child: Text('Ошибка загрузки: ${snapshot.error}'));
          }
          final staff = snapshot.data;
          if (staff == null || staff.isEmpty) {
            return const Center(child: Text('У вас пока нет сотрудников'));
          }
          return RefreshIndicator(
            onRefresh: () async => _loadStaff(),
            child: ListView.builder(
              itemCount: staff.length,
              itemBuilder: (context, index) {
                final staffMember = staff[index];
                return ListTile(
                  leading: const CircleAvatar(child: Icon(Icons.badge)),
                  title: Text(staffMember.name),
                  subtitle: staffMember.specialty != null ? Text(staffMember.specialty!) : null,
                  trailing: IconButton(
                    icon: const Icon(Icons.delete_outline, color: Colors.redAccent),
                    onPressed: () => _deleteStaffMember(staffMember.id),
                  ),
                  onTap: () => _navigateToEditScreen(staffMember: staffMember),
                );
              },
            ),
          );
        },
      ),
      floatingActionButton: FloatingActionButton(
        heroTag: 'staff_fab', 
        onPressed: () => _navigateToEditScreen(),
        tooltip: 'Добавить сотрудника',
        child: const Icon(Icons.add),
      ),
    );
  }
}
