<script>
    import CalendarScreen from '$lib/components/calendar/CalendarScreen.svelte';
    import ScheduleScreen from '$lib/components/schedule/ScheduleScreen.svelte';
    import AppointmentEditScreen from '$lib/components/schedule/AppointmentEditScreen.svelte';
    import { onMount } from 'svelte';

    // Режимы: 'month', 'day', 'edit'
    let viewMode = 'month';
    let selectedDate = new Date();
    let currentAppointment = null; // Для редактирования
    let preselectedData = null;    // Для новой записи из таймлайна

    function handleDateSelected(event) {
        selectedDate = event.detail.date;
        viewMode = 'day';
    }

    function openNewAppointment(event) {
        preselectedData = {
            date: selectedDate,
            hour: event?.detail?.hour || 10,
            min: event?.detail?.min || 0,
            staffId: event?.detail?.staffId || null
        };
        currentAppointment = null;
        viewMode = 'edit';
    }

    function openEditAppointment(event) {
        currentAppointment = event.detail;
        preselectedData = null;
        viewMode = 'edit';
    }

    function backToDay() {
        viewMode = 'day';
    }

    function handleSaved() {
        viewMode = 'day';
        // Расписание обновится само через WebSocket или onMount
    }
</script>

<div class="calendar-tab">
    {#if viewMode === 'month'}
        <div class="tab-content">
            <div class="header-row">
                <h2>Календарь загрузки</h2>
                <button class="today-btn" on:click={() => { selectedDate = new Date(); viewMode = 'day'; }}>СЕГОДНЯ</button>
            </div>
            <CalendarScreen on:dateSelected={handleDateSelected} />
        </div>

    {:else if viewMode === 'day'}
        <div class="day-view-container">
            <div class="day-header">
                <button class="back-link" on:click={() => viewMode = 'month'}>‹ Месяц</button>
                <div class="date-title">
                    {selectedDate.toLocaleDateString('ru-RU', { day: 'numeric', month: 'long' })}
                </div>
                <button class="add-mini" on:click={() => openNewAppointment()}>+ Запись</button>
            </div>
            <ScheduleScreen
                initialDate={selectedDate}
                on:emptySlotTap={openNewAppointment}
                on:appointmentTap={openEditAppointment}
            />
        </div>

    {:else if viewMode === 'edit'}
        <div class="edit-view-container">
            <div class="day-header">
                <button class="back-link" on:click={backToDay}>‹ Расписание</button>
                <div class="date-title">{currentAppointment ? 'Изменить запись' : 'Новая запись'}</div>
            </div>
            <AppointmentEditScreen
                appointment={currentAppointment}
                preselected={preselectedData}
                on:cancel={backToDay}
                on:saved={handleSaved}
            />
        </div>
    {/if}
</div>

<style>
    .calendar-tab { height: 100%; display: flex; flex-direction: column; overflow: hidden; }
    .tab-content { padding: 20px; animation: fadeIn 0.3s ease-out; }

    .header-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
    h2 { font-size: 24px; font-weight: 800; margin: 0; color: #0f172a; }

    .today-btn { background: var(--primary-gradient); color: white; border: none; padding: 10px 20px; border-radius: 14px; font-weight: 700; font-size: 13px; box-shadow: 0 4px 15px rgba(56, 151, 240, 0.2); }

    .day-view-container, .edit-view-container { flex: 1; display: flex; flex-direction: column; animation: slideIn 0.2s ease-out; overflow: hidden; }
    .day-header { padding: 14px 20px; background: white; border-bottom: 1px solid #f1f5f9; display: flex; align-items: center; justify-content: space-between; }
    .back-link { background: none; border: none; color: var(--primary-color); font-weight: 700; font-size: 14px; cursor: pointer; }
    .date-title { font-weight: 800; color: #1e293b; font-size: 15px; }
    .add-mini { background: #eff6ff; color: var(--primary-color); border: none; padding: 6px 12px; border-radius: 10px; font-size: 12px; font-weight: 800; }

    @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
    @keyframes slideIn { from { transform: translateX(30px); opacity: 0; } to { transform: translateX(0); opacity: 1; } }
</style>
