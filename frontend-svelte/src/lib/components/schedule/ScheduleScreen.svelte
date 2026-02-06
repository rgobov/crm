<script>
    import { onMount, onDestroy, createEventDispatcher } from 'svelte';
    import { adminService } from '$lib/services/adminService.js';
    import { scheduleUpdates } from '$lib/services/websocketService.js';
    import { selectedDate } from '$lib/stores/dashboardStore.js';
    import HorizontalDatePicker from './HorizontalDatePicker.svelte';
    import DayTimeline from './DayTimeline.svelte';

    const dispatch = createEventDispatcher();

    let appointments = [];
    let staff = [];
    let isLoading = true;

    // РЕАКТИВНОСТЬ: Гарантируем срабатывание при изменении стора
    // Используем конструкцию, которая точно отследит изменение значения
    $: {
        const dateToLoad = $selectedDate;
        console.log('Schedule: Reactive trigger detected for date:', dateToLoad?.toDateString());
        if (dateToLoad) {
            loadDayData(dateToLoad);
        }
    }

    const unsubscribe = scheduleUpdates.subscribe(update => {
        if (update === 'refresh') loadDayData($selectedDate, true);
    });

    onMount(() => {
        if ($selectedDate) loadDayData($selectedDate);
    });

    onDestroy(() => unsubscribe());

    async function loadDayData(date, silent = false) {
        if (!date) return;
        if (!silent) isLoading = true;

        try {
            console.log('Schedule: Fetching data from API for', date.toISOString().split('T')[0]);

            const [apptsData, staffData] = await Promise.all([
                adminService.getAppointmentsForDay(date),
                adminService.getStaffForSchedule(date)
            ]);

            appointments = apptsData || [];
            staff = (staffData || []).filter(s => s.role === 'ROLE_EMPLOYEE' || s.role === 'EMPLOYEE');

            console.log('Schedule: Data loaded successfully. Masters count:', staff.length);
        } catch (e) {
            console.error('Schedule: API Error', e);
        } finally {
            isLoading = false;
        }
    }

    function handleHorizontalDate(event) {
        // При выборе даты в мобильном пикере - пишем в глобальный стор
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
                <p>Нет мастеров на {new Date($selectedDate).toLocaleDateString('ru-RU')}</p>
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

    @media (min-width: 1024px) {
        .date-picker-container { display: none; }
    }

    .timeline-body { flex: 1; overflow: hidden; width: 100%; position: relative; }
    .center-box { display: flex; justify-content: center; align-items: center; height: 100%; }
    .empty-state-msg { display: flex; flex-direction: column; justify-content: center; align-items: center; height: 100%; color: #94a3b8; font-weight: 600; padding: 20px; text-align: center; }
    .empty-state-msg .icon { font-size: 32px; margin-bottom: 12px; }
    .spinner { width: 30px; height: 30px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
</style>
