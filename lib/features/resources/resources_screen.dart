
import 'package:flutter/material.dart';
import 'package:try_neuro/features/resources/data/resource_service.dart';
import 'package:try_neuro/features/resources/domain/resource_model.dart';
import 'package:try_neuro/features/resources/resource_edit_screen.dart';
import 'package:try_neuro/service_locator.dart';

class ResourcesScreen extends StatefulWidget {
  const ResourcesScreen({super.key});

  @override
  State<ResourcesScreen> createState() => _ResourcesScreenState();
}

class _ResourcesScreenState extends State<ResourcesScreen> {
  final ResourceService _resourceService = sl<ResourceService>();
  late Future<List<Resource>> _resourcesFuture;

  @override
  void initState() {
    super.initState();
    _loadResources();
  }

  void _loadResources() {
    setState(() {
      _resourcesFuture = _resourceService.getResources();
    });
  }

  void _navigateToEditScreen({Resource? resource}) async {
    final result = await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => ResourceEditScreen(initialResource: resource),
      ),
    );
    if (result == true) {
      _loadResources();
    }
  }

  void _deleteResource(String resourceId) async {
     final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Подтверждение'),
        content: const Text('Вы уверены, что хотите удалить этот ресурс?'),
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
      await _resourceService.deleteResource(resourceId);
      _loadResources();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Ресурсы'),
      ),
      body: FutureBuilder<List<Resource>>(
        future: _resourcesFuture,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError) {
            return Center(child: Text('Ошибка загрузки: ${snapshot.error}'));
          }
          final resources = snapshot.data;
          if (resources == null || resources.isEmpty) {
            return const Center(child: Text('У вас пока нет ресурсов'));
          }
          return RefreshIndicator(
            onRefresh: () async => _loadResources(),
            child: ListView.builder(
              itemCount: resources.length,
              itemBuilder: (context, index) {
                final resource = resources[index];
                return ListTile(
                  leading: const CircleAvatar(child: Icon(Icons.build)),
                  title: Text(resource.name),
                  subtitle: resource.description != null ? Text(resource.description!) : null,
                  trailing: IconButton(
                    icon: const Icon(Icons.delete_outline, color: Colors.redAccent),
                    onPressed: () => _deleteResource(resource.id),
                  ),
                  onTap: () => _navigateToEditScreen(resource: resource),
                );
              },
            ),
          );
        },
      ),
      floatingActionButton: FloatingActionButton(
        heroTag: 'resources_fab', // <-- ИЗМЕНЕНИЕ
        onPressed: () => _navigateToEditScreen(),
        tooltip: 'Добавить ресурс',
        child: const Icon(Icons.add),
      ),
    );
  }
}
