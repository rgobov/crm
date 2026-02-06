<script>
    import CalendarScreen from '$lib/components/calendar/CalendarScreen.svelte';
    import ScheduleScreen from '$lib/components/schedule/ScheduleScreen.svelte';
    import AppointmentEditScreen from '$lib/components/schedule/AppointmentEditScreen.svelte';
    import AppointmentDetailScreen from '$lib/components/schedule/AppointmentDetailScreen.svelte';
    import { activeTab, selectedDate } from '$lib/stores/dashboardStore.js'; // ИМПОРТ СТОРА ДАТЫ
    import { onMount } from 'svelte';

    export let forcedDate = null;

    let viewMode = 'month';
    let currentAppointment = null;
    let preselectedData = null;

    // Реактивность: Если пришла принудительная дата из пропсов - пишем её в стор
    $: if (forcedDate && viewMode !== 'edit' && viewMode !== 'detail') {
        selectedDate.set(new Date(forcedDate));
        viewMode = 'day';
    }

    // Реактивность: Переключение вкладок (Таймлайн / Календарь месяца)
    $: if ($activeTab === 'timeline' && viewMode !== 'edit' && viewMode !== 'detail') {
        viewMode = 'day';
    } else if ($activeTab === 'calendar' && viewMode !== 'edit' && viewMode !== 'detail') {
        viewMode = 'month';
    }

    function handleDateSelected(event) {
        // Записываем дату в глобальный стор
        selectedDate.set(event.detail.date);
        viewMode = 'day';
    }

    function openNewAppointment(event) {
        preselectedData = {
            date: $selectedDate,
            hour: event?.detail?.hour || 10,
            min: event?.detail?.min || 0,
            staffId: event?.detail?.staffId || null
        };
        currentAppointment = null;
        viewMode = 'edit';
    }

    function openDetail(event) {
        currentAppointment = event.detail;
        viewMode = 'detail';
    }

    function backToDay() {
        viewMode = 'day';
    }
</script>

<div class="calendar-tab">
    {#if viewMode === 'month'}
        <div class="tab-content">
            <div class="header-row">
                <h2>Сетка месяца</h2>
                <!-- При нажатии СЕГОДНЯ - обновляем стор -->
                <button class="today-btn" on:click={() => { selectedDate.set(new Date()); viewMode = 'day'; activeTab.set('timeline'); }}>СЕГОДНЯ</button>
            </div>
            <CalendarScreen on:dateSelected={handleDateSelected} />
        </div>

    {:else if viewMode === 'day'}
        <div class="day-view-container">
            <div class="day-header">
                <button class="back-link mobile-only" on:click={() => { viewMode = 'month'; activeTab.set('calendar'); }}>‹ Месяц</button>

                <div class="date-title">
                    <span class="day-num">{$selectedDate.getDate()}</span>
                    <span class="month-year">{$selectedDate.toLocaleDateString('ru-RU', { month: 'long', year: 'numeric' })}</span>
                </div>

                <button class="add-mini" on:click={() => openNewAppointment({ detail: {} })}>+ Запись</button>
            </div>

            <!-- ИСПОЛЬЗУЕМ ДАТУ ИЗ ГЛОБАЛЬНОГО СТОРА -->
            <ScheduleScreen
                initialDate={$selectedDate}
                forcedDate={$selectedDate}
                on:emptySlotTap={openNewAppointment}
                on:appointmentTap={openDetail}
            />
        </div>

    {:else if viewMode === 'edit'}
        <div class="edit-view-container">
            <div class="day-header">
                <button class="back-link" on:click={backToDay}>‹ Расписание</button>
                <div class="date-title">{currentAppointment ? 'Изменить запись' : 'Новая запись'}</div>
            </div>
            <div class="scrollable-form">
                <AppointmentEditScreen
                    appointment={currentAppointment}
                    preselected={preselectedData}
                    on:cancel={backToDay}
                    on:saved={backToDay}
                />
            </div>
        </div>

    {:else if viewMode === 'detail'}
        <div class="edit-view-container">
            <div class="day-header">
                <button class="back-link" on:click={backToDay}>‹ Расписание</button>
                <div class="date-title">Детали визита</div>
            </div>
            <div class="scrollable-form">
                <AppointmentDetailScreen
                    appointment={currentAppointment}
                    on:edit={(e) => { currentAppointment = e.detail; viewMode = 'edit'; }}
                    on:deleted={backToDay}
                />
            </div>
        </div>
    {/if}
</div>

<style>
    .calendar-tab { height: 100%; display: flex; flex-direction: column; overflow: hidden; background: white; }
    .tab-content { padding: 24px; animation: fadeIn 0.3s ease-out; }
    .header-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
    h2 { font-size: 24px; font-weight: 800; margin: 0; color: #0f172a; }
    .today-btn { background: var(--primary-gradient); color: white; border: none; padding: 10px 20px; border-radius: 14px; font-weight: 700; font-size: 13px; cursor: pointer; }
    .day-view-container, .edit-view-container { flex: 1; display: flex; flex-direction: column; animation: slideIn 0.2s ease-out; overflow: hidden; }
    .scrollable-form { flex: 1; overflow-y: auto; }
    .day-header { padding: 16px 24px; background: white; border-bottom: 1px solid #f1f5f9; display: flex; align-items: center; justify-content: space-between; flex-shrink: 0; }
    .date-title { display: flex; align-items: baseline; gap: 8px; }
    .date-title .day-num { font-size: 22px; font-weight: 900; color: var(--primary-color); }
    .date-title .month-year { font-size: 14px; font-weight: 700; color: #64748b; text-transform: uppercase; }
    .back-link { background: none; border: none; color: var(--primary-color); font-weight: 700; font-size: 14px; cursor: pointer; }
    .add-mini { background: #eff6ff; color: var(--primary-color); border: none; padding: 8px 16px; border-radius: 12px; font-size: 13px; font-weight: 800; cursor: pointer; }
    @media (min-width: 1024px) { .mobile-only { display: none; } }
    @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
    @keyframes slideIn { from { transform: translateX(30px); opacity: 0; } to { transform: translateX(0); opacity: 1; } }
</style>
