
import 'package:try_neuro/features/services/domain/service_model.dart';

class AppService {
  final List<Service> _services = [
    Service(id: '1', name: 'Стрижка мужская', durationInMinutes: 45),
    Service(id: '2', name: 'Замена масла', durationInMinutes: 60),
    Service(id: '3', name: 'Маникюр с покрытием', durationInMinutes: 90),
  ];

  Future<List<Service>> getServices() async {
    await Future.delayed(const Duration(milliseconds: 300));
    return List.unmodifiable(_services);
  }

  Future<void> addService({
    required String name,
    required int durationInMinutes,
  }) async {
    await Future.delayed(const Duration(milliseconds: 500));
    final newId = DateTime.now().millisecondsSinceEpoch.toString();
    _services.add(Service(
      id: newId,
      name: name,
      durationInMinutes: durationInMinutes,
    ));
  }

  Future<void> updateService(Service service) async {
    await Future.delayed(const Duration(milliseconds: 500));
    final index = _services.indexWhere((s) => s.id == service.id);
    if (index != -1) {
      _services[index] = service;
    }
  }

  Future<void> deleteService(String serviceId) async {
    await Future.delayed(const Duration(milliseconds: 500));
    _services.removeWhere((s) => s.id == serviceId);
  }
}
