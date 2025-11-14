
import 'package:flutter/material.dart';
import 'package:try_neuro/features/contacts/contacts_screen.dart';
import 'package:try_neuro/features/resources/resources_screen.dart';
import 'package:try_neuro/features/schedule/schedule_screen.dart';
import 'package:try_neuro/features/services/services_screen.dart';
import 'package:try_neuro/features/staff/staff_screen.dart';

class ManagerHomeScreen extends StatefulWidget {
  const ManagerHomeScreen({super.key});

  @override
  State<ManagerHomeScreen> createState() => _ManagerHomeScreenState();
}

class _ManagerHomeScreenState extends State<ManagerHomeScreen> {
  int _selectedIndex = 0;

  static const List<Widget> _widgetOptions = <Widget>[
    ContactsScreen(),
    ScheduleScreen(),
    ResourcesScreen(),
    StaffScreen(),
    ServicesScreen(), // <-- ДОБАВЛЕНО
  ];

  void _onItemTapped(int index) {
    setState(() {
      _selectedIndex = index;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(
        index: _selectedIndex,
        children: _widgetOptions,
      ),
      bottomNavigationBar: BottomNavigationBar(
        type: BottomNavigationBarType.fixed,
        items: const <BottomNavigationBarItem>[
          BottomNavigationBarItem(icon: Icon(Icons.people), label: 'Клиенты'),
          BottomNavigationBarItem(icon: Icon(Icons.calendar_today), label: 'Расписание'),
          BottomNavigationBarItem(icon: Icon(Icons.build_circle_outlined), label: 'Ресурсы'),
          BottomNavigationBarItem(icon: Icon(Icons.badge), label: 'Персонал'),
          BottomNavigationBarItem(icon: Icon(Icons.cut), label: 'Услуги'), // <-- ДОБАВЛЕНО
        ],
        currentIndex: _selectedIndex,
        onTap: _onItemTapped,
        selectedFontSize: 12,
        unselectedFontSize: 12,
      ),
    );
  }
}
