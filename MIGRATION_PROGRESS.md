# Реестр миграции Flutter -> Svelte (Синхронизация имен)

## 0. Системное ядро (Core)
| Flutter Файл | Svelte / JS Аналог | Статус | Заметки |
| :--- | :--- | :---: | :--- |
| `main.dart` | `src/app.html` + `+layout.svelte` | ✅ | Инициализация и TG SDK |
| `http_client.dart` | `src/lib/api.js` | ✅ | Axios клиент |
| `session_service.dart`| `src/lib/stores/auth.js` | ✅ | JWT и роли |
| `phone_utils.dart` | `src/lib/utils/PhoneUtils.js` | ✅ | Форматирование телефона |

## 1. Роль: АДМИНИСТРАТОР (Administrator)
| Flutter Файл (Source) | Svelte Компонент (Analogy) | Svelte Роут (Path) | Статус |
| :--- | :--- | :--- | :---: |
| `admin_dashboard_screen.dart`| `AdminDashboardScreen.svelte` | `routes/admin/+page.svelte` | ✅ |
| `admin_management_tab.dart` | `AdminManagementTab.svelte` | (Tab inside Admin) | ✅ |
| `staff_screen.dart` | `StaffScreen.svelte` | `routes/admin/staff/+page.svelte` | ✅ |
| `staff_edit_screen.dart` | `StaffEditScreen.svelte` | `routes/admin/staff/[id]/+page.svelte` | ✅ |
| `contacts_screen.dart` | `ContactsScreen.svelte` | `routes/admin/clients/+page.svelte` | ✅ |
| `contact_detail_screen.dart` | `ContactDetailScreen.svelte` | `routes/admin/clients/[id]/+page.svelte`| ✅ |
| `contact_edit_screen.dart` | `ContactEditScreen.svelte` | `routes/admin/clients/[id]/edit` | ✅ |
| `services_screen.dart` | `ServicesScreen.svelte` | `routes/admin/services/+page.svelte` | ❌ |
| `service_edit_screen.dart` | `ServiceEditScreen.svelte` | `routes/admin/services/[id]/+page.svelte`| ❌ |
| `resources_screen.dart` | `ResourcesScreen.svelte` | `routes/admin/resources/+page.svelte` | ❌ |
| `calendar_screen.dart` | `CalendarScreen.svelte` | (Tab inside Admin) | ⏳ |

## 2. Роль: СОТРУДНИК (Employee)
| Flutter Файл (Source) | Svelte Компонент (Analogy) | Svelte Роут (Path) | Статус |
| :--- | :--- | :--- | :---: |
| `employee_home_screen.dart` | `EmployeeHomeScreen.svelte` | `routes/employee/+page.svelte` | ❌ |
| `my_work_schedule_screen.dart`| `MyWorkScheduleScreen.svelte`| `routes/employee/shifts/+page.svelte`| ❌ |

## 3. Общие элементы (Common)
| Flutter Файл (Source) | Svelte Компонент (Analogy) | Статус |
| :--- | :--- | :---: |
| `appointment_detail_screen.dart`| `AppointmentDetailScreen.svelte` | ❌ |
| `comment_thread_screen.dart` | `CommentThreadScreen.svelte` | ❌ |
| `day_timeline.dart` | `DayTimeline.svelte` | ❌ |

**Легенда:**
- ✅ - Синхронизировано
- ⏳ - В разработке
- ❌ - Ожидание
