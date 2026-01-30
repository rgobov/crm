# Реестр миграции Flutter -> Svelte

| Flutter Экран | Svelte Путь | UI | Logic | API | Статус | Заметки |
| :--- | :--- | :---: | :---: | :---: | :---: | :--- |
| `login_screen.dart` | `/` | ✅ | ✅ | ✅ | **Done** | Бесшовный вход, поддержка TG |
| `register_company_screen.dart` | `/register` | ✅ | ✅ | ✅ | **Done** | |
| `admin_dashboard_screen.dart` | `/admin` (Shell) | ✅ | ✅ | ✅ | **Done** | Tab-система (бесшовная) |
| `admin_management_tab.dart` | `ManagementTab.svelte` | ✅ | ✅ | ✅ | **Done** | Статистика синхронизирована |
| `staff_screen.dart` | `/admin/staff` | ✅ | ✅ | ✅ | **Done** | Поиск (2 симв/6 цифр), Пагинация |
| `staff_edit_screen.dart` | `/admin/staff/[id]` | ✅ | ✅ | ✅ | **Done** | Проверка прав админа |
| `contacts_screen.dart` | `/admin/clients` | ✅ | ✅ | ✅ | **Done** | Infinite Scroll, Smart Search |
| `calendar_screen.dart` | `CalendarTab.svelte` | ⏳ | ❌ | ❌ | *In Progress* | Нужно: Лента дат, Сетка |
| `services_screen.dart` | `/admin/services` | ❌ | ❌ | ❌ | Pending | |
| `resources_screen.dart` | `/admin/resources` | ❌ | ❌ | ❌ | Pending | |
| `appointment_edit_screen.dart` | TBD | ❌ | ❌ | ❌ | Pending | |

**Легенда:**
- ✅ - Полностью синхронизировано с Flutter и Java
- ⏳ - В процессе разработки
- ❌ - Еще не начато
- 🔄 - Требуется обновление после изменений в Java
