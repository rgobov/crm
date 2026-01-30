<script>
    import { onMount, onDestroy } from 'svelte';
    import { adminService } from '$lib/services/adminService.js';
    import { scheduleUpdates } from '$lib/services/websocketService.js';
    import HorizontalDatePicker from './HorizontalDatePicker.svelte';
    import DayTimeline from './DayTimeline.svelte';
    import { goto } from '$app/navigation';

    export let initialDate = new Date();

    let selectedDate = initialDate;
    let appointments = [];
    let staff = [];
    let isLoading = true;

    // Подписка на обновления через WebSocket (как во Flutter)
    const unsubscribe = scheduleUpdates.subscribe(update => {
        if (update === 'refresh') {
            loadDayData(true);
        }
    });

    onMount(() => {
        loadDayData();
    });

    onDestroy(() => {
        unsubscribe();
    });

    async function loadDayData(silent = false) {
        if (!silent) isLoading = true;
        try {
            // Загружаем записи и смены мастеров одновременно
            const [apptsData, staffData] = await Promise.all([
                adminService.getAppointmentsForDay(selectedDate),
                adminService.getStaffForSchedule(selectedDate)
            ]);

            appointments = apptsData;
            // Фильтруем только сотрудников (как во Flutter)
            staff = staffData.filter(s => s.role === 'EMPLOYEE');
        } catch (e) {
            console.error('Failed to load schedule data', e);
        } finally {
            isLoading = false;
        }
    }

    function handleDateChange(event) {
        selectedDate = event.detail.date;
        loadDayData();
    }

    function goToToday() {
        selectedDate = new Date();
        loadDayData();
    }

    function handleAppointmentTap(appt) {
        goto(`/admin/appointments/${appt.id}`);
    }

    function handleEmptySlotTap(event) {
        const { time, staffId } = event.detail;
        console.log('Open add appointment for:', time, staffId);
        // В будущем: переход на создание записи
    }
</script>

<div class="schedule-wrapper">
    <!-- ЛЕНТА ДАТ (Сверху, чтобы всегда была под рукой) -->
    <div class="date-picker-section">
        <HorizontalDatePicker {selectedDate} on:dateSelected={handleDateChange} />
    </div>

    <!-- ОСНОВНОЙ ТАЙМЛАЙН -->
    <div class="timeline-section">
        {#if isLoading && appointments.length === 0}
            <div class="center"><span class="spinner"></span></div>
        {:else}
            <DayTimeline
                day={selectedDate}
                {appointments}
                {staff}
                on:appointmentTap={(e) => handleAppointmentTap(e.detail)}
                on:emptySlotTap={handleEmptySlotTap}
            />
        {/if}
    </div>

    <!-- КНОПКА "ЗАПИСАТЬ" (FAB) -->
    <button class="fab" on:click={() => console.log('Add new')}>
        <span class="icon">+</span>
        <span class="label">ЗАПИСАТЬ</span>
    </button>
</div>

<style>
    .schedule-wrapper {
        display: flex;
        flex-direction: column;
        height: calc(100vh - 130px); /* Вычитаем высоту хедера и нижнего меню */
        background: white;
        max-width: 500px; /* Адаптивность под ПК */
        margin: 0 auto;
        position: relative;
    }

    .date-picker-section {
        flex-shrink: 0;
        z-index: 30;
        box-shadow: 0 4px 10px rgba(0,0,0,0.02);
    }

    .timeline-section {
        flex: 1;
        overflow: hidden;
    }

    .fab {
        position: absolute;
        bottom: 20px;
        right: 20px;
        background: var(--primary-gradient);
        color: white;
        border: none;
        border-radius: 30px;
        padding: 12px 24px;
        display: flex;
        align-items: center;
        gap: 8px;
        font-weight: 800;
        font-size: 14px;
        box-shadow: 0 10px 25px rgba(56, 151, 240, 0.4);
        cursor: pointer;
        z-index: 100;
    }
    .fab .icon { font-size: 20px; }

    .center { display: flex; justify-content: center; align-items: center; height: 100%; }
    .spinner { width: 30px; height: 30px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
</style>
