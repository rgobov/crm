import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:try_neuro/core/session/session_service.dart';
import 'package:try_neuro/core/utils/phone_utils.dart';
import 'package:try_neuro/features/auth/domain/user_model.dart';
import 'package:try_neuro/features/contacts/contact_edit_screen.dart';
import 'package:try_neuro/features/contacts/domain/contact_model.dart';
import 'package:try_neuro/features/manager/data/manager_service.dart';
import 'package:try_neuro/features/schedule/domain/appointment_model.dart';
import 'package:try_neuro/features/staff/data/employee_service.dart';
import 'package:try_neuro/service_locator.dart';

class ContactDetailScreen extends StatefulWidget {
  final Contact contact;

  const ContactDetailScreen({super.key, required this.contact});

  @override
  State<ContactDetailScreen> createState() => _ContactDetailScreenState();
}

class _ContactDetailScreenState extends State<ContactDetailScreen> {
  final _sessionService = sl<SessionService>();
  final _managerService = sl<ManagerService>();
  final _employeeService = sl<EmployeeService>();

  late Contact _contact;
  late Future<List<Appointment>> _historyFuture;
  User? _currentUser;

  @override
  void initState() {
    super.initState();
    _contact = widget.contact;
    _initialize();
  }

  Future<void> _initialize() async {
    _currentUser = await _sessionService.getCurrentUser();
    _loadHistory();
  }

  void _loadHistory() {
    setState(() {
      if (_currentUser?.role == UserRole.manager) {
        _historyFuture = _managerService.getContactAppointments(_contact.id);
      } else {
        _historyFuture = _employeeService.getContactAppointments(_contact.id);
      }
    });
  }

  void _navigateToEditScreen() async {
    final result = await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => ContactEditScreen(initialContact: _contact),
      ),
    );

    if (result == true) {
      if (mounted) {
        Navigator.of(context).pop(true);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 2,
      child: Scaffold(
        appBar: AppBar(
          title: Text(_contact.name),
          actions: [
            IconButton(
              icon: const Icon(Icons.edit),
              onPressed: _navigateToEditScreen,
              tooltip: 'Редактировать',
            ),
          ],
          bottom: const TabBar(
            tabs: [
              Tab(text: 'ИНФО', icon: Icon(Icons.info_outline)),
              Tab(text: 'ИСТОРИЯ', icon: Icon(Icons.history)),
            ],
          ),
        ),
        body: TabBarView(
          children: [
            _buildInfoTab(),
            _buildHistoryTab(),
          ],
        ),
      ),
    );
  }

  Widget _buildInfoTab() {
    return SelectionArea(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Padding(
              padding: EdgeInsets.only(left: 4, bottom: 8),
              child: Text('КОНТАКТНЫЕ НОМЕРА', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: Colors.grey)),
            ),
            ..._contact.phones.map((phone) => _buildDetailCard(PhoneUtils.format(phone), Icons.phone)),
            
            const SizedBox(height: 16),
            const Padding(
              padding: EdgeInsets.only(left: 4, bottom: 8),
              child: Text('ПРОЧЕЕ', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: Colors.grey)),
            ),
            if (_contact.email != null && _contact.email!.isNotEmpty)
              _buildDetailCard(_contact.email!, Icons.email, title: 'Email'),
            if (_contact.notes != null && _contact.notes!.isNotEmpty)
              _buildDetailCard(_contact.notes!, Icons.note, title: 'Заметки'),
          ],
        ),
      ),
    );
  }

  Widget _buildHistoryTab() {
    return FutureBuilder<List<Appointment>>(
      future: _historyFuture,
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Center(child: CircularProgressIndicator());
        }
        if (snapshot.hasError) {
          return Center(child: Text('Ошибка загрузки истории: ${snapshot.error}'));
        }
        final history = snapshot.data ?? [];
        if (history.isEmpty) {
          return const Center(child: Text('История посещений пуста'));
        }

        return ListView.separated(
          padding: const EdgeInsets.all(16.0),
          itemCount: history.length,
          separatorBuilder: (context, index) => const Divider(),
          itemBuilder: (context, index) {
            final appointment = history[index];
            return _buildHistoryItem(appointment);
          },
        );
      },
    );
  }

  Widget _buildHistoryItem(Appointment appointment) {
    final dateFormat = DateFormat('dd.MM.yyyy');
    final timeFormat = DateFormat.Hm();
    final timeStr = timeFormat.format(DateTime(0).add(Duration(hours: appointment.time.hour, minutes: appointment.time.minute)));

    // --- ИСПРАВЛЕННЫЙ СВИТЧ С НОВОЙ ПАЛИТРОЙ ---
    Color statusColor;
    String statusText;
    switch (appointment.status) {
      case AppointmentStatus.scheduled:
        statusColor = const Color(0xFF42A5F5); // Синий
        statusText = 'Ожидает';
        break;
      case AppointmentStatus.confirmed:
        statusColor = const Color(0xFF26A69A); // Бирюзовый
        statusText = 'Подтверждено';
        break;
      case AppointmentStatus.needs_call:
        statusColor = const Color(0xFFFFA726); // Янтарный
        statusText = 'Перезвонить';
        break;
      case AppointmentStatus.completed:
        statusColor = const Color(0xFF90A4AE); // Серо-синий
        statusText = 'Выполнено';
        break;
      case AppointmentStatus.cancelled:
        statusColor = const Color(0xFFEF5350); // Красный
        statusText = 'Отменено';
        break;
    }

    return ListTile(
      contentPadding: EdgeInsets.zero,
      title: Text(
        appointment.service,
        style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
      ),
      subtitle: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const SizedBox(height: 4),
          Row(
            children: [
              const Icon(Icons.calendar_today, size: 14, color: Colors.grey),
              const SizedBox(width: 4),
              Text('${dateFormat.format(appointment.date)} в $timeStr'),
            ],
          ),
          const SizedBox(height: 2),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
            decoration: BoxDecoration(
              color: statusColor.withOpacity(0.1),
              borderRadius: BorderRadius.circular(4),
            ),
            child: Text(
              statusText,
              style: TextStyle(color: statusColor, fontSize: 12, fontWeight: FontWeight.bold),
            ),
          ),
        ],
      ),
      trailing: const Icon(Icons.chevron_right),
      onTap: () {},
    );
  }

  Widget _buildDetailCard(String value, IconData icon, {String? title}) {
    return Card(
      margin: const EdgeInsets.only(bottom: 8.0),
      elevation: 0,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12), side: BorderSide(color: Colors.grey.shade200)),
      child: ListTile(
        leading: Icon(icon, color: Colors.blue),
        title: title != null ? Text(title, style: const TextStyle(fontSize: 12, color: Colors.grey)) : null,
        subtitle: Text(value, style: const TextStyle(fontSize: 16, color: Colors.black87)),
      ),
    );
  }
}
