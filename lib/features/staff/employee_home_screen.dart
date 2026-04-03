import 'package:flutter/material.dart';
import 'package:try_neuro/features/auth/domain/user_model.dart';
import 'package:try_neuro/features/calendar/calendar_screen.dart';
import 'package:try_neuro/features/staff/employee_schedule_screen.dart';
import 'package:try_neuro/features/staff/my_work_schedule_screen.dart';

class EmployeeHomeScreen extends StatefulWidget {
  final User user;
  const EmployeeHomeScreen({super.key, required this.user});

  @override
  State<EmployeeHomeScreen> createState() => _EmployeeHomeScreenState();
}

class _EmployeeHomeScreenState extends State<EmployeeHomeScreen> {
  int _currentIndex = 0;
  DateTime? _requestedDate; // Дата, переданная из календаря

  void _onDateSelectedFromCalendar(DateTime date) {
    setState(() {
      _requestedDate = date;
      _currentIndex = 0; // Переключаем на вкладку "Записи"
    });
  }

  @override
  Widget build(BuildContext context) {
    // Список экранов формируем динамически, чтобы передавать колбэки
    final List<Widget> screens = [
      EmployeeScheduleScreen(
        user: widget.user,
        initialDate: _requestedDate, // Передаем дату, если она была выбрана в календаре
      ),
      CalendarScreen(onDateSelected: _onDateSelectedFromCalendar), // Передаем функцию обратного вызова
      const MyWorkScheduleScreen(),
    ];

    return Scaffold(
      body: IndexedStack(
        index: _currentIndex,
        children: screens,
      ),
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _currentIndex,
        onTap: (index) {
          setState(() {
            _currentIndex = index;
            // Если переключаемся вручную, сбрасываем запрос даты
            if (index != 0) _requestedDate = null;
          });
        },
        items: const [
          BottomNavigationBarItem(
            icon: Icon(Icons.calendar_today),
            label: 'Записи',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.view_comfy),
            label: 'Календарь',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.settings),
            label: 'Мой график',
          ),
        ],
      ),
    );
  }
}
