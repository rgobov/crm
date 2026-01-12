import 'package:flutter/material.dart';
import 'package:try_neuro/core/network/time_service.dart'; // <<< ИМПОРТ
import 'package:try_neuro/features/contacts/data/contact_service.dart';
import 'package:try_neuro/features/contacts/domain/contact_model.dart';
import 'package:try_neuro/features/manager/data/manager_service.dart';
import 'package:try_neuro/features/resources/data/resource_service.dart';
import 'package:try_neuro/features/resources/domain/resource_model.dart';
import 'package:try_neuro/features/schedule/domain/appointment_model.dart';
import 'package:try_neuro/features/schedule/domain/workload_model.dart';
import 'package:try_neuro/features/staff/data/staff_service.dart';
import 'package:try_neuro/features/staff/domain/staff_member_model.dart';
import 'package:try_neuro/service_locator.dart';

class AdminDashboardViewModel extends ChangeNotifier {
  final StaffService _staffService = sl<StaffService>();
  final ManagerService _managerService = sl<ManagerService>();
  final ContactService _contactService = sl<ContactService>();
  final ResourceService _resourceService = sl<ResourceService>();
  final TimeService _timeService = sl<TimeService>(); // <<< ДОБАВЛЯЕМ

  bool _isLoading = true;
  bool get isLoading => _isLoading;

  List<StaffMember> _staff = [];
  List<StaffMember> get staff => _staff;

  List<Appointment> _todayAppointments = [];
  List<Appointment> get todayAppointments => _todayAppointments;

  List<Workload> _monthlyWorkload = [];
  List<Workload> get monthlyWorkload => _monthlyWorkload;

  List<Contact> _contacts = []; 
  List<Resource> _resources = []; 

  int get totalClients => _contacts.length;
  int get todaysAppointmentsCount => _todayAppointments.length;
  int get totalResources => _resources.length;

  Future<void> loadData() async {
    _isLoading = true;
    notifyListeners();

    try {
      // --- ИЗМЕНЕНИЕ: Используем серверное время ---
      final now = _timeService.now();
      
      final results = await Future.wait([
        _staffService.getStaff(),
        _managerService.getAppointmentsForDay(now),
        _managerService.getWorkloadForMonth(now.year, now.month),
        _contactService.getContacts(),
        _resourceService.getResources(),
      ]);

      _staff = results[0] as List<StaffMember>;
      _todayAppointments = results[1] as List<Appointment>;
      _monthlyWorkload = results[2] as List<Workload>;
      _contacts = results[3] as List<Contact>;
      _resources = results[4] as List<Resource>;

    } catch (e) {
      // Handle error
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }
}
