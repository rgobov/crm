import 'package:flutter/material.dart';
import 'package:try_neuro/features/auth/domain/user_model.dart';
import 'package:try_neuro/features/calendar/calendar_screen.dart';
import 'package:try_neuro/features/staff/employee_schedule_screen.dart'; // Импортируем новый экран

class EmployeeHomeScreen extends StatefulWidget {
  final User user;
  const EmployeeHomeScreen({super.key, required this.user});

  @override
  State<EmployeeHomeScreen> createState() => _EmployeeHomeScreenState();
}

class _EmployeeHomeScreenState extends State<EmployeeHomeScreen> {
  int _currentIndex = 0;

  // Объявляем поле, но не инициализируем сразу
  late final List<Widget> _screens; 

  // --- ИСПРАВЛЕНИЕ ЗДЕСЬ ---
  // Инициализируем список в initState, где у нас есть доступ к `widget.user`
  @override
  void initState() {
    super.initState();
    _screens = [
      EmployeeScheduleScreen(user: widget.user),
      const CalendarScreen(),
    ];
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(
        index: _currentIndex,
        children: _screens,
      ),
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _currentIndex,
        onTap: (index) => setState(() => _currentIndex = index),
        items: const [
          BottomNavigationBarItem(
            icon: Icon(Icons.calendar_today),
            label: 'Расписание', 
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.view_comfy),
            label: 'Календарь',
          ),
        ],
      ),
    );
  }
}
