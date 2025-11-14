
import 'package:try_neuro/features/staff/domain/staff_member_model.dart';

class StaffService {
  final List<StaffMember> _staff = [
    StaffMember(id: '1', name: 'Ирина Иванова', specialty: 'Парикмахер-стилист'),
    StaffMember(id: '2', name: 'Петр Петров', specialty: 'Автомеханик'),
    StaffMember(id: '3', name: 'Ольга Смирнова', specialty: 'Мастер маникюра'),
  ];

  Future<List<StaffMember>> getStaff() async {
    await Future.delayed(const Duration(milliseconds: 300));
    return List.unmodifiable(_staff);
  }

  Future<void> addStaffMember({
    required String name,
    String? specialty,
  }) async {
    await Future.delayed(const Duration(milliseconds: 500));
    final newId = DateTime.now().millisecondsSinceEpoch.toString();
    _staff.add(StaffMember(
      id: newId,
      name: name,
      specialty: specialty,
    ));
  }

  Future<void> updateStaffMember(StaffMember staffMember) async {
    await Future.delayed(const Duration(milliseconds: 500));
    final index = _staff.indexWhere((s) => s.id == staffMember.id);
    if (index != -1) {
      _staff[index] = staffMember;
    }
  }

  Future<void> deleteStaffMember(String staffMemberId) async {
    await Future.delayed(const Duration(milliseconds: 500));
    _staff.removeWhere((s) => s.id == staffMemberId);
  }
}
