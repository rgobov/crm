# Реестр миграции Flutter -> Svelte (Синхронизация имен)

## 0. Системное ядро (Core)
| Flutter Файл | Svelte / JS Аналог | Статус | Заметки |
| :--- | :--- | :---: | :--- |
| `main.dart` | `src/app.html`, `+layout.svelte` | ✅ | Инициализация и TG SDK |
| `http_client.dart` | `src/lib/api.js` | ✅ | Axios клиент, Interceptors |
| `app_config.dart` | `src/lib/api.js` | ✅ | Настройка базовых URL |
| `session_service.dart`| `src/lib/stores/auth.js` | ✅ | JWT, Role, TenantID, StaffID |
| `phone_utils.dart` | `src/lib/utils/phoneUtils.js` | ✅ | Форматирование телефона |
| `websocket_service.dart`| `src/lib/services/websocketService.js`| ✅ | Live updates (Stomp/SockJS) |

## 1. Роль: АДМИНИСТРАТОР (Administrator)
| Flutter Файл (Source) | Svelte Компонент (Analogy) | Svelte Роут (Path) | Статус |
| :--- | :--- | :--- | :---: |
| `admin_dashboard_screen.dart`| `AdminDashboardScreen.svelte` | `routes/admin/+page.svelte` | ✅ |
| `admin_management_tab.dart` | `AdminManagementTab.svelte` | (Tab inside Admin) | ✅ |
| `staff_screen.dart` | `StaffScreen.svelte` | `routes/admin/staff/+page.svelte` | ✅ |
| `staff_edit_screen.dart` | `StaffEditScreen.svelte` | `routes/admin/staff/[id]/+page.svelte` | ✅ |
| `contacts_screen.dart` | `ContactsScreen.svelte` | `routes/admin/clients/+page.svelte` | ✅ |
| `contact_detail_screen.dart` | `ContactDetailScreen.svelte` | `routes/admin/clients/[id]/+page.svelte`| ✅ |
| `services_screen.dart` | `ServicesScreen.svelte` | `routes/admin/services/+page.svelte` | ✅ |
| `resources_screen.dart` | `ResourcesScreen.svelte` | `routes/admin/resources/+page.svelte` | ✅ |
| `calendar_screen.dart` | `CalendarScreen.svelte` | (Tab inside Admin) | ✅ |

## 2. Роль: МЕНЕДЖЕР (Manager)
| Flutter Файл (Source) | Svelte Компонент (Analogy) | Svelte Роут (Path) | Статус |
| :--- | :--- | :--- | :---: |
| `manager_home_screen.dart` | `ManagerHomeScreen.svelte` | `routes/manager/+page.svelte` | ❌ |
| `wappi_settings_screen.dart` | `WappiSettingsScreen.svelte` | `routes/manager/settings/wappi` | ✅ |
| `schedule_screen.dart` | `ScheduleScreen.svelte` | (Shared with Admin) | ✅ |

## 3. Роль: СОТРУДНИК (Employee)
| Flutter Файл (Source) | Svelte Компонент (Analogy) | Svelte Роут (Path) | Статус |
| :--- | :--- | :--- | :---: |
| `employee_home_screen.dart` | `EmployeeHomeScreen.svelte` | `routes/employee/+page.svelte` | ✅ |
| `my_work_schedule_screen.dart`| `MyWorkScheduleScreen.svelte`| `routes/employee/shifts/+page.svelte`| ⏳ |

## 4. Общие экраны (Common)
| Flutter Файл (Source) | Svelte Компонент (Analogy) | Svelte Роут (Path) | Статус |
| :--- | :--- | :--- | :---: |
| `appointment_edit_screen.dart` | `AppointmentEditScreen.svelte` | (Modal in Calendar) | ✅ |
| `appointment_detail_screen.dart`| `AppointmentDetailScreen.svelte`| (Modal in Calendar) | ✅ |
| `comment_thread_screen.dart` | `CommentThreadScreen.svelte` | (Inside Details) | ✅ |

**Легенда:**
- ✅ - Синхронизировано
- ⏳ - В разработке
- ❌ - Ожидание
