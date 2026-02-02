<script>
    import { onMount, onDestroy } from 'svelte';
    import { adminService } from '$lib/services/adminService.js';
    import { scheduleUpdates } from '$lib/services/websocketService.js';
    import HorizontalDatePicker from './HorizontalDatePicker.svelte';
    import DayTimeline from './DayTimeline.svelte';

    export let initialDate = new Date();
    export let forcedDate = null;

    let selectedDate = initialDate;
    let appointments = [];
    let staff = [];
    let isLoading = true;

    // Реактивно обновляем дату при выборе в Sidebar (Desktop)
    $: if (forcedDate) {
        selectedDate = forcedDate;
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
            const [apptsData, staffData] = await Promise.all([
                adminService.getAppointmentsForDay(selectedDate),
                adminService.getStaffForSchedule(selectedDate)
            ]);
            appointments = apptsData;
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
</script>

<div class="schedule-screen">
    <!-- СКРЫВАЕМ ЛЕНТУ ДАТ НА ДЕСКТОПЕ (когда ширина > 1024px) -->
    <div class="date-picker-container">
        <HorizontalDatePicker {selectedDate} on:dateSelected={handleDateChange} />
    </div>

    <div class="timeline-body">
        {#if isLoading && appointments.length === 0}
            <div class="center"><span class="spinner"></span></div>
        {:else}
            <DayTimeline
                day={selectedDate}
                {appointments}
                {staff}
                on:appointmentTap
                on:emptySlotTap
            />
        {/if}
    </div>
</div>

<style>
    .schedule-screen { display: flex; flex-direction: column; height: 100%; width: 100%; background: white; overflow: hidden; }

    .date-picker-container { flex-shrink: 0; z-index: 30; background: white; border-bottom: 1px solid #f1f5f9; display: flex; justify-content: center; }

    /* ПРЯЧЕМ ПОЛОСУ НА ПК */
    @media (min-width: 1024px) {
        .date-picker-container { display: none; }
    }

    .timeline-body { flex: 1; overflow: hidden; width: 100%; }
    .center { display: flex; justify-content: center; align-items: center; height: 100%; }
    .spinner { width: 30px; height: 30px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
</style>
