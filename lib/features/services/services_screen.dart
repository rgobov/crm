
import 'package:flutter/material.dart';
import 'package:try_neuro/features/services/data/app_service.dart';
import 'package:try_neuro/features/services/domain/service_model.dart';
import 'package:try_neuro/features/services/service_edit_screen.dart';
import 'package:try_neuro/service_locator.dart';

class ServicesScreen extends StatefulWidget {
  const ServicesScreen({super.key});

  @override
  State<ServicesScreen> createState() => _ServicesScreenState();
}

class _ServicesScreenState extends State<ServicesScreen> {
  final AppService _appService = sl<AppService>();
  late Future<List<Service>> _servicesFuture;

  @override
  void initState() {
    super.initState();
    _loadServices();
  }

  void _loadServices() {
    setState(() {
      _servicesFuture = _appService.getServices();
    });
  }

  void _navigateToEditScreen({Service? service}) async {
    final result = await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => ServiceEditScreen(initialService: service),
      ),
    );
    if (result == true) {
      _loadServices();
    }
  }

  void _deleteService(String serviceId) async {
     final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Подтверждение'),
        content: const Text('Вы уверены, что хотите удалить эту услугу?'),
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
      await _appService.deleteService(serviceId);
      _loadServices();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Услуги'),
      ),
      body: FutureBuilder<List<Service>>(
        future: _servicesFuture,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError) {
            return Center(child: Text('Ошибка загрузки: ${snapshot.error}'));
          }
          final services = snapshot.data;
          if (services == null || services.isEmpty) {
            return const Center(child: Text('У вас пока нет услуг'));
          }
          return RefreshIndicator(
            onRefresh: () async => _loadServices(),
            child: ListView.builder(
              itemCount: services.length,
              itemBuilder: (context, index) {
                final service = services[index];
                return ListTile(
                  leading: const CircleAvatar(child: Icon(Icons.cut)),
                  title: Text(service.name),
                  subtitle: Text('${service.durationInMinutes} мин.'),
                  trailing: IconButton(
                    icon: const Icon(Icons.delete_outline, color: Colors.redAccent),
                    onPressed: () => _deleteService(service.id),
                  ),
                  onTap: () => _navigateToEditScreen(service: service),
                );
              },
            ),
          );
        },
      ),
      floatingActionButton: FloatingActionButton(
        heroTag: 'services_fab', // ИЗМЕНЕНИЕ
        onPressed: () => _navigateToEditScreen(),
        tooltip: 'Добавить услугу',
        child: const Icon(Icons.add),
      ),
    );
  }
}
