<script>
    import { onMount, onDestroy, createEventDispatcher } from 'svelte';
    import { adminService } from '$lib/services/adminService.js';
    import { scheduleUpdates } from '$lib/services/websocketService.js';
    import HorizontalDatePicker from './HorizontalDatePicker.svelte';
    import DayTimeline from './DayTimeline.svelte';

    const dispatch = createEventDispatcher();

    export let initialDate = new Date();
    export let forcedDate = null;

    let selectedDate = initialDate;
    let appointments = [];
    let staff = [];
    let isLoading = true;

    // Реактивно обновляем дату при выборе в Sidebar (Desktop)
    $: if (forcedDate) {
        selectedDate = new Date(forcedDate);
        loadDayData();
    }

    const unsubscribe = scheduleUpdates.subscribe(update => {
        if (update === 'refresh') loadDayData(true);
    });

    onMount(() => loadDayData());
    onDestroy(() => unsubscribe());

    async function loadDayData(silent = false) {
        if (!silent) isLoading = true;
        try {
            console.log('Schedule: Requesting data for', selectedDate.toDateString());

            const [apptsData, staffData] = await Promise.all([
                adminService.getAppointmentsForDay(selectedDate),
                adminService.getStaffForSchedule(selectedDate)
            ]);

            appointments = apptsData || [];
            staff = (staffData || []).filter(s => s.role === 'ROLE_EMPLOYEE' || s.role === 'EMPLOYEE');

            console.log('Schedule: Received', staff.length, 'masters');
        } catch (e) {
            console.error('Schedule: Failed to load data', e);
        } finally {
            isLoading = false;
        }
    }

    function handleDateChange(event) {
        selectedDate = event.detail.date;
        loadDayData();
    }

    // ИСПРАВЛЕНО: Явные обработчики для проброса событий родителю (CalendarTab)
    function handleEmptySlot(event) {
        console.log('Schedule: Forwarding emptySlotTap');
        dispatch('emptySlotTap', event.detail);
    }

    function handleAppointment(event) {
        console.log('Schedule: Forwarding appointmentTap');
        dispatch('appointmentTap', event.detail);
    }
</script>

<div class="schedule-screen">
    <div class="date-picker-container">
        <HorizontalDatePicker {selectedDate} on:dateSelected={handleDateChange} />
    </div>

    <div class="timeline-body">
        {#if isLoading && staff.length === 0}
            <div class="center-box"><span class="spinner"></span></div>
        {:else if staff.length === 0}
            <div class="empty-state-msg">Сотрудники не найдены. Проверьте базу данных.</div>
        {:else}
            <DayTimeline
                day={selectedDate}
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
    .empty-state-msg { display: flex; justify-content: center; align-items: center; height: 100%; color: #94a3b8; font-weight: 600; }
    .spinner { width: 30px; height: 30px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
</style>
