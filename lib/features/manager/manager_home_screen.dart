import 'package:flutter/material.dart';
import 'package:try_neuro/features/calendar/calendar_screen.dart';
import 'package:try_neuro/features/contacts/contacts_screen.dart';
import 'package:try_neuro/features/schedule/schedule_screen.dart';

class ManagerHomeScreen extends StatefulWidget {
  const ManagerHomeScreen({super.key});

  @override
  State<ManagerHomeScreen> createState() => _ManagerHomeScreenState();
}

class _ManagerHomeScreenState extends State<ManagerHomeScreen> {
  int _selectedIndex = 0;

  // Убираем статический список, чтобы виджеты могли пересоздаваться
  Widget _getSelectedScreen(int index) {
    switch (index) {
      case 0:
        return const ScheduleScreen();
      case 1:
        return const CalendarScreen();
      case 2:
        return const ContactsScreen(); // Теперь будет создаваться заново при клике
      default:
        return const ScheduleScreen();
    }
  }

  void _onItemTapped(int index) {
    setState(() {
      _selectedIndex = index;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      // Заменяем IndexedStack на простое отображение текущего виджета
      body: _getSelectedScreen(_selectedIndex),
      bottomNavigationBar: BottomNavigationBar(
        type: BottomNavigationBarType.fixed,
        items: const <BottomNavigationBarItem>[
          BottomNavigationBarItem(
            icon: Icon(Icons.calendar_today),
            label: 'Расписание',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.calendar_view_month),
            label: 'Календарь',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.people),
            label: 'Клиенты',
          ),
        ],
        currentIndex: _selectedIndex,
        onTap: _onItemTapped,
      ),
    );
  }
}
