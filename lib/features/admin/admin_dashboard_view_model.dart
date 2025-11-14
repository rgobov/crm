
import 'package:try_neuro/features/contacts/data/contact_service.dart';
import 'package:try_neuro/features/resources/data/resource_service.dart';
import 'package:try_neuro/features/schedule/data/schedule_service.dart';
import 'package:try_neuro/features/schedule/domain/appointment_model.dart';
import 'package:try_neuro/service_locator.dart';

// Этот класс отвечает за загрузку и подготовку данных для экрана администратора
class AdminDashboardViewModel {
  final ContactService _contactService = sl<ContactService>();
  final ScheduleService _scheduleService = sl<ScheduleService>();
  final ResourceService _resourceService = sl<ResourceService>();

  // Свойства для хранения загруженных данных
  int totalClients = 0;
  int todaysAppointmentsCount = 0;
  int totalResources = 0;
  List<Appointment> todaysAppointments = [];

  // Метод, который загружает всю информацию параллельно
  Future<void> loadData() async {
    final results = await Future.wait([
      _contactService.getContacts(),
      _scheduleService.getAppointmentsForDay(DateTime.now()),
      _resourceService.getResources(),
    ]);

    totalClients = (results[0] as List).length;
    todaysAppointments = results[1] as List<Appointment>;
    todaysAppointmentsCount = todaysAppointments.length;
    totalResources = (results[2] as List).length;
  }
}
