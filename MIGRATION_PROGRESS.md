# Полный реестр миграции Flutter -> Svelte

## 0. Системное ядро (Core & Infrastructure)
| Flutter Файл | Svelte / JS Файлы | UI | Logic | API | Статус | Функции и Кнопки |
| :--- | :--- | :---: | :---: | :---: | :---: | :--- |
| `main.dart` | `app.html`, `+layout.svelte` | — | ✅ | — | **Done** | Инициализация приложения, TG SDK |
| `http_client.dart` | `$lib/api.js` | — | ✅ | ✅ | **Done** | Axios instance, BaseURL, Interceptors |
| `app_config.dart` | `$lib/api.js` (env vars) | — | ✅ | — | **Done** | Настройка Prod/Dev серверов |
| `session_service.dart`| `$lib/stores/auth.js` | — | ✅ | ✅ | **Done** | JWT, Role, TenantID, StaffID |
| `time_service.dart` | `$lib/utils/time.js` | — | ❌ | — | Pending | Часовые пояса, форматирование дат |
| `websocket_service.dart`| `$lib/services/ws.js` | — | ❌ | ✅ | Pending | Живые уведомления, Stomp/SockJS |
| `phone_utils.dart` | `$lib/utils/phone.js` | — | ✅ | — | **Done** | Форматирование +7 (___), очистка номера |
| `sync_service.dart`<br>`offline_queue_service.dart`| `$lib/services/sync.js`| — | ❌ | — | Pending | Офлайн режим, синхронизация данных |

## 1. Роль: АДМИНИСТРАТОР (Administrator)
| Flutter Файл | Svelte Файлы | UI | Logic | API | Статус | Функции и Кнопки |
| :--- | :--- | :---: | :---: | :---: | :---: | :--- |
| `admin_dashboard_screen.dart` | `admin/+page.svelte`<br>`BottomNav.svelte` | ✅ | ✅ | ✅ | **Done** | - [x] Tab-System (Бесшовность)<br>- [x] Переключение вкладок |
| `admin_management_tab.dart` | `admin/ManagementTab.svelte`| ✅ | ✅ | ✅ | **Done** | - [x] Карточки статистики<br>- [x] Справочники |
| `staff_screen.dart` | `admin/staff/+page.svelte` | ✅ | ✅ | ✅ | **Done** | - [x] Поиск (2 симв/6 цифр)<br>- [x] Пагинация бэкенда |
| `staff_edit_screen.dart` | `admin/staff/[id]/+page.svelte`| ✅ | ✅ | ✅ | **Done** | - [x] UUID/TenantID Sync<br>- [x] Account Creation |
| `contacts_screen.dart` | `admin/clients/+page.svelte` | ✅ | ✅ | ✅ | **Done** | - [x] Infinite Scroll<br>- [x] Smart Search |
| `contact_detail_screen.dart`<br>`contact_edit_screen.dart` | `admin/clients/[id]/+page.svelte`| ❌ | ❌ | ❌ | Pending | - [ ] История визитов клиента<br>- [ ] Общая сумма покупок |
| `services_screen.dart` | `admin/services/+page.svelte` | ❌ | ❌ | ❌ | Pending | - [ ] Список услуг, Длительность |
| `service_edit_screen.dart` | `admin/services/[id]/+page.svelte`| ❌ | ❌ | ❌ | Pending | - [ ] Редактирование услуги |
| `resources_screen.dart` | `admin/resources/+page.svelte` | ❌ | ❌ | ❌ | Pending | - [ ] Список кабинетов/ресурсов |
| `calendar_screen.dart` | `admin/CalendarTab.svelte` | ⏳ | ❌ | ❌ | *In Progress*| - [ ] Лента дат, Сетка времени |

## 2. Роль: СОТРУДНИК (Employee)
| Flutter Файл | Svelte Файлы | UI | Logic | API | Статус | Функции и Кнопки |
| :--- | :--- | :---: | :---: | :---: | :---: | :--- |
| `employee_home_screen.dart` | `employee/+page.svelte` | ❌ | ❌ | ❌ | Pending | - [ ] Записи на сегодня<br>- [ ] Кнопка "Начать смену" |
| `employee_schedule_screen.dart`| `employee/schedule/+page.svelte`| ❌ | ❌ | ❌ | Pending | - [ ] Мой календарь |
| `my_work_schedule_screen.dart` | `employee/shifts/+page.svelte` | ❌ | ❌ | ❌ | Pending | - [ ] Настройка графика работы |

## 3. Общие бизнес-экраны (Schedule & Appts)
| Flutter Файл | Svelte Файлы | UI | Logic | API | Статус | Функции и Кнопки |
| :--- | :--- | :---: | :---: | :---: | :---: | :--- |
| `appointment_detail_screen.dart`| `common/appt/[id]/+page.svelte`| ❌ | ❌ | ❌ | Pending | - [ ] Детали, Статусы, Оплата |
| `appointment_edit_screen.dart` | `common/appt/edit.svelte` | ❌ | ❌ | ❌ | Pending | - [ ] Выбор услуги/мастера/времени |
| `comment_thread_screen.dart` | `common/appt/Comments.svelte` | ❌ | ❌ | ❌ | Pending | - [ ] Лента комментариев к записи |
| `day_timeline.dart` | `common/Timeline.svelte` | ❌ | ❌ | ❌ | Pending | - [ ] Отрисовка временной сетки |
| `wappi_settings_screen.dart` | `admin/settings/wappi.svelte` | ❌ | ❌ | ❌ | Pending | - [ ] Настройка интеграции Wappi |

**Легенда:**
- ✅ - Синхронизировано (Flutter + Java + Svelte)
- ⏳ - В разработке
- ❌ - Ожидание
- 🔄 - Требуется обновление
