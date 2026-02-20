<script>
    import { onMount, onDestroy, createEventDispatcher } from 'svelte';
    import { adminService } from '$lib/services/adminService.js';
    import { scheduleRefreshSignal } from '$lib/services/websocketService.js';
    import { selectedDate } from '$lib/stores/dashboardStore.js';
    import HorizontalDatePicker from './HorizontalDatePicker.svelte';
    import DayTimeline from './DayTimeline.svelte';

    export let branchId = null;
    export let onlyBusyStaff = false; // <<< НОВЫЙ ПРОПС ДЛЯ ФИЛЬТРАЦИИ

    const dispatch = createEventDispatcher();

    let appointments = [];
    let staff = [];
    let isLoading = true;

    // РЕАКТИВНАЯ ФИЛЬТРАЦИЯ МАСТЕРОВ
    $: displayedStaff = (() => {
        if (!onlyBusyStaff) return staff;

        // Получаем набор ID мастеров, у которых есть записи сегодня
        const busyStaffIds = new Set(appointments.map(a => a.staffMemberId));
        return staff.filter(s => busyStaffIds.has(s.id));
    })();

    $: if ($selectedDate && branchId) {
        loadDayData($selectedDate, branchId);
    } else {
        isLoading = false;
    }

    const unsubscribe = scheduleRefreshSignal.subscribe(signal => {
        if (signal && signal.ts > 0 && branchId) {
            loadDayData($selectedDate, branchId, true);
        }
    });

    onMount(() => {
        if ($selectedDate && branchId) loadDayData($selectedDate, branchId);
    });

    onDestroy(() => unsubscribe());

    async function loadDayData(date, bId, silent = false) {
        if (!date || !bId) {
            isLoading = false;
            return;
        }

        if (!silent) {
            isLoading = true;
            appointments = [];
            staff = [];
        }

        try {
            const [apptsData, staffData] = await Promise.all([
                adminService.getAppointmentsForDay(date, bId),
                adminService.getStaffForSchedule(date, bId)
            ]);

            appointments = apptsData || [];
            staff = (staffData || []).filter(s => s.role === 'ROLE_EMPLOYEE' || s.role === 'EMPLOYEE');
        } catch (e) {
            console.error('❌ Schedule Screen API Error:', e);
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
        {:else if !branchId}
             <div class="empty-state-msg">
                <span class="icon">🏢</span>
                <p>Выберите филиал в меню слева</p>
            </div>
        {:else if staff.length === 0 && !isLoading}
            <div class="empty-state-msg">
                <span class="icon">👥</span>
                <p>В этом филиале пока нет работающих мастеров</p>
            </div>
        {:else}
            <!-- ПЕРЕДАЕМ ОТФИЛЬТРОВАННЫЙ СПИСОК displayedStaff -->
            <DayTimeline
                day={$selectedDate}
                {appointments}
                staff={displayedStaff}
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
