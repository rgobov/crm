<script>
    import { onMount, onDestroy, createEventDispatcher } from 'svelte';
    import { adminService } from '$lib/services/adminService.js';
    import { scheduleRefreshSignal } from '$lib/services/websocketService.js'; // Новый триггер
    import { selectedDate } from '$lib/stores/dashboardStore.js';
    import HorizontalDatePicker from './HorizontalDatePicker.svelte';
    import DayTimeline from './DayTimeline.svelte';

    const dispatch = createEventDispatcher();

    let appointments = [];
    let staff = [];
    let isLoading = true;

    // РЕАКТИВНОСТЬ: Загрузка при смене даты в календаре
    $: if ($selectedDate) {
        loadDayData($selectedDate);
    }

    // WEBSOCKET: Мгновенная реакция на сигнал обновления (через timestamp)
    const unsubscribe = scheduleRefreshSignal.subscribe(signal => {
        if (signal && signal.ts > 0) {
            console.log('🔄 Schedule: Global refresh signal received (Real-time update)');
            loadDayData($selectedDate, true); // silent update
        }
    });

    onMount(() => {
        if ($selectedDate) loadDayData($selectedDate);
    });

    onDestroy(() => unsubscribe());

    async function loadDayData(date, silent = false) {
        if (!date) return;
        if (!silent) isLoading = true;

        try {
            // Используем локальную дату для запроса
            const year = date.getFullYear();
            const month = String(date.getMonth() + 1).padStart(2, '0');
            const day = String(date.getDate()).padStart(2, '0');
            const dateStr = `${year}-${month}-${day}`;

            const [apptsData, staffData] = await Promise.all([
                adminService.getAppointmentsForDay(date),
                adminService.getStaffForSchedule(date)
            ]);

            appointments = apptsData || [];
            staff = (staffData || []).filter(s => s.role === 'ROLE_EMPLOYEE' || s.role === 'EMPLOYEE');
        } catch (e) {
            console.error('❌ Schedule API Error', e);
        } finally {
            isLoading = false;
        }
    }

    function handleHorizontalDate(event) {
        selectedDate.set(new Date(event.detail.date));
    }

    function handleEmptySlot(event) {
        dispatch('emptySlotTap', event.detail);
    }

    function handleAppointment(event) {
        dispatch('appointmentTap', event.detail);
    }
</script>

<div class="schedule-screen">
    <div class="date-picker-container">
        <HorizontalDatePicker selectedDate={$selectedDate} on:dateSelected={handleHorizontalDate} />
    </div>

    <div class="timeline-body">
        {#if isLoading && staff.length === 0}
            <div class="center-box"><span class="spinner"></span></div>
        {:else if staff.length === 0}
            <div class="empty-state-msg">
                <span class="icon">🔍</span>
                <p>Мастера на эту дату не найдены</p>
            </div>
        {:else}
            <DayTimeline
                day={$selectedDate}
                {appointments}
                {staff}
                on:appointmentTap={handleAppointment}
                on:emptySlotTap={handleEmptySlot}
            />
        {/if}
    </div>
</div>

<style>
    .schedule-screen { display: flex; flex-direction: column; height: 100%; width: 100%; background: white; overflow: hidden; }
    .date-picker-container { flex-shrink: 0; z-index: 30; background: white; border-bottom: 1px solid #f1f5f9; display: flex; justify-content: center; }
    @media (min-width: 1024px) { .date-picker-container { display: none; } }
    .timeline-body { flex: 1; overflow: hidden; width: 100%; position: relative; }
    .center-box { display: flex; justify-content: center; align-items: center; height: 100%; }
    .empty-state-msg { display: flex; flex-direction: column; justify-content: center; align-items: center; height: 100%; color: #94a3b8; font-weight: 600; padding: 20px; text-align: center; }
    .empty-state-msg .icon { font-size: 32px; margin-bottom: 12px; }
    .spinner { width: 30px; height: 30px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
</style>
