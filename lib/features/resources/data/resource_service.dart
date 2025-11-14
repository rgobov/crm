
import 'package:try_neuro/features/resources/domain/resource_model.dart';

class ResourceService {
  final List<Resource> _resources = [
    Resource(id: '1', name: 'Подъемник #1', description: 'Двухстоечный'),
    Resource(id: '2', name: 'Аппарат для чистки лица', description: 'Модель SuperClean 2000'),
    Resource(id: '3', name: 'Парикмахерское кресло #2'),
  ];

  Future<List<Resource>> getResources() async {
    await Future.delayed(const Duration(milliseconds: 300));
    return List.unmodifiable(_resources);
  }

  Future<void> addResource({
    required String name,
    String? description,
  }) async {
    await Future.delayed(const Duration(milliseconds: 500));
    final newId = DateTime.now().millisecondsSinceEpoch.toString();
    _resources.add(Resource(
      id: newId,
      name: name,
      description: description,
    ));
  }

  Future<void> updateResource(Resource resource) async {
    await Future.delayed(const Duration(milliseconds: 500));
    final index = _resources.indexWhere((r) => r.id == resource.id);
    if (index != -1) {
      _resources[index] = resource;
    }
  }

  Future<void> deleteResource(String resourceId) async {
    await Future.delayed(const Duration(milliseconds: 500));
    _resources.removeWhere((r) => r.id == resourceId);
  }
}
