
import 'package:flutter/material.dart';
import 'package:try_neuro/features/admin/admin_dashboard_view_model.dart';
import 'package:try_neuro/features/resources/resources_screen.dart';
import 'package:try_neuro/features/services/services_screen.dart';
import 'package:try_neuro/features/staff/staff_screen.dart';

class AdminDashboardScreen extends StatefulWidget {
  const AdminDashboardScreen({super.key});

  @override
  State<AdminDashboardScreen> createState() => _AdminDashboardScreenState();
}

class _AdminDashboardScreenState extends State<AdminDashboardScreen> {
  final AdminDashboardViewModel _viewModel = AdminDashboardViewModel();
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    await _viewModel.loadData();
    if (mounted) {
      setState(() {
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Панель администратора'),
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: () async => _loadData(),
              child: ListView(
                padding: const EdgeInsets.all(16.0),
                children: [
                  _buildStatsGrid(),
                  const SizedBox(height: 24),
                  Text('Управление справочниками', style: Theme.of(context).textTheme.headlineSmall),
                  const SizedBox(height: 8),
                  _buildManagementButton(
                    context,
                    title: 'Персонал',
                    icon: Icons.badge,
                    onTap: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const StaffScreen())),
                  ),
                  _buildManagementButton(
                    context,
                    title: 'Ресурсы',
                    icon: Icons.build_circle_outlined,
                    onTap: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const ResourcesScreen())),
                  ),
                  _buildManagementButton(
                    context,
                    title: 'Услуги',
                    icon: Icons.cut,
                    onTap: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const ServicesScreen())),
                  ),
                ],
              ),
            ),
    );
  }

  Widget _buildManagementButton(BuildContext context, {required String title, required IconData icon, required VoidCallback onTap}) {
    return Card(
      margin: const EdgeInsets.symmetric(vertical: 4.0),
      child: ListTile(
        leading: Icon(icon),
        title: Text(title),
        trailing: const Icon(Icons.chevron_right),
        onTap: onTap,
      ),
    );
  }

  Widget _buildStatsGrid() {
    return GridView.count(
      crossAxisCount: 3,
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      mainAxisSpacing: 16,
      crossAxisSpacing: 16,
      children: [
        _buildStatCard('Клиенты', _viewModel.totalClients.toString(), Icons.people, Colors.blue),
        _buildStatCard('Записи сегодня', _viewModel.todaysAppointmentsCount.toString(), Icons.calendar_today, Colors.orange),
        _buildStatCard('Ресурсы', _viewModel.totalResources.toString(), Icons.build, Colors.green),
      ],
    );
  }

  Widget _buildStatCard(String title, String value, IconData icon, Color color) {
    return Card(
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(12.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            CircleAvatar(backgroundColor: color, radius: 20, child: Icon(icon, color: Colors.white)),
            const SizedBox(height: 10),
            Text(value, style: Theme.of(context).textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.bold)),
            Text(title, textAlign: TextAlign.center, style: Theme.of(context).textTheme.bodySmall),
          ],
        ),
      ),
    );
  }
}
