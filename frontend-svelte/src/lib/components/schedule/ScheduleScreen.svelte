<script>
    import { onMount, onDestroy, createEventDispatcher } from 'svelte';
    import { adminService } from '$lib/services/adminService.js';
    import { scheduleRefreshSignal } from '$lib/services/websocketService.js';
    import { dbService } from '$lib/services/dbService.js';
    import { selectedDate, activeBranchId } from '$lib/stores/dashboardStore.js';
    import { branchStore } from '$lib/stores/branchStore.js';
    import DayTimeline from './DayTimeline.svelte';
    import api from '$lib/api.js';

    export let onlyBusyStaff = false;
    export let onlyWorkingStaff = false;
    export let branchId = null;

    const dispatch = createEventDispatcher();

    let appointments = [];
    let staff = [];
    let isLoading = false;
    let lastLoadTime = 0;
    let refreshTimeout;

    // ПРИОРИТЕТ: Проп, если он есть, иначе Стор.
    $: currentBranchId = branchId || $activeBranchId;

    async function fetchAppointments(date, bId, silent = false) {
        if (!date || !bId) return;
        if (!silent) isLoading = true;
        try {
            console.log(`📡 Fetching appointments for date: ${date}, branch: ${bId}`);
            const apptsData = await adminService.getAppointmentsForDay(date, bId);
            appointments = apptsData || [];
        } catch (e) {
            console.error('❌ Error loading appointments:', e);
        } finally {
            if (!silent) isLoading = false;
        }
    }

    async function fetchStaff(date, bId, silent = false) {
        if (!date || !bId) return;
        if (!silent) isLoading = true;
        try {
            console.log(`📡 Fetching staff for date: ${date}, branch: ${bId}`);
            const staffData = await adminService.getStaffForSchedule(date, bId);

            let staffArray = [];
            if (typeof staffData === 'string') {
                try { staffArray = JSON.parse(staffData); } catch (e) { staffArray = []; }
            } else {
                staffArray = Array.isArray(staffData) ? staffData : [];
            }

            // Soft update: сохраняем фото, если они уже есть в памяти
            staff = staffArray.map(ns => {
                const existing = staff.find(os => os.id === ns.id);
                if (existing && existing.photoData) {
                    return { ...ns, photoData: existing.photoData };
                }
                return ns;
            });

            // Ленивая загрузка недостающих фото
            staff.forEach(member => {
                // Если фото нет или оно могло устареть
                loadStaffPhoto(member.id, member.photoUpdatedAt).then(photo => {
                    if (photo && member.photoData !== photo) {
                        staff = staff.map(s => s.id === member.id ? {...s, photoData: photo} : s);
                    }
                });
            });
        } catch (e) {
            console.error('❌ Error loading staff:', e);
        } finally {
            if (!silent) isLoading = false;
        }
    }

    async function loadDayData(date, bId, silent = false) {
        if (!date || !bId) return;
        if (!silent) isLoading = true;
        await Promise.all([
            fetchAppointments(date, bId, true),
            fetchStaff(date, bId, true)
        ]);
        isLoading = false;
    }

    function debouncedRefresh() {
        clearTimeout(refreshTimeout);
        refreshTimeout = setTimeout(() => {
            if ($selectedDate && currentBranchId) {
                console.log('🔄 WS: Debounced refresh (appointments only)');
                fetchAppointments($selectedDate, currentBranchId, true);
            }
        }, 300);
    }

    async function loadStaffPhoto(staffId, updatedAtOnServer) {
        if (!staffId) return null;

        // 1. Проверяем кэш в IndexedDB
        const cachedRecord = await dbService.getPhoto(staffId);

        // 2. Если фото есть и оно актуальное
        if (cachedRecord && cachedRecord.photoData && updatedAtOnServer) {
            if (cachedRecord.updatedAt >= updatedAtOnServer) {
                return cachedRecord.photoData;
            }
        }
        
        try {
            // 3. Если нет в кэше или устарело, идем на сервер
            const response = await api.get(`/admin/schedule/staff/${staffId}/photo`);
            const photoData = response.data.photoData;

            if (photoData) {
                // Сохраняем в кэш с новой датой
                await dbService.savePhoto(staffId, photoData, updatedAtOnServer || Date.now());
                return photoData;
            }
            return cachedRecord ? cachedRecord.photoData : null;
        } catch (e) {
            console.error(`Error loading photo for ${staffId}:`, e);
            return cachedRecord ? cachedRecord.photoData : null;
        }
    }

    $: displayedStaff = (() => {
        let result = staff;
        if (onlyWorkingStaff) result = result.filter(s => !s.dayOff);
        if (onlyBusyStaff) {
            const busyStaffIds = new Set(appointments.map(a => a.staffMemberId));
            result = result.filter(s => busyStaffIds.has(s.id));
        }
        return result;
    })();

    // РЕАКТИВНАЯ ЗАГРУЗКА ПРИ СМЕНЕ ДАТЫ ИЛИ ФИЛИАЛА
    $: if ($selectedDate && currentBranchId) {
        const now = Date.now();
        if (now - lastLoadTime > 800) {
            lastLoadTime = now;
            loadDayData($selectedDate, currentBranchId);
        }
    }

    const unsubscribe = scheduleRefreshSignal.subscribe(signal => {
        if (signal && signal.ts > 0 && currentBranchId) {
            debouncedRefresh();
        }
    });

    onMount(async () => {
        if (branchStore && typeof $branchStore !== 'undefined' && $branchStore.length === 0) {
            branchStore.refresh();
        }
        if ($selectedDate && currentBranchId) {
            await loadDayData($selectedDate, currentBranchId);
        }
    });

    onDestroy(() => {
        unsubscribe();
        clearTimeout(refreshTimeout);
    });

    function handleEmptySlot(event) { dispatch('emptySlotTap', event.detail); }
    function handleAppointment(event) { dispatch('appointmentTap', event.detail); }
    function handleStaffTap(event) { dispatch('staffTap', event.detail); }
</script>

<div class="schedule-screen">
    <div class="timeline-body">
        {#if isLoading && staff.length === 0}
            <div class="center-box">
                <span class="spinner"></span>
                <p style="margin-top: 12px; font-size: 12px; color: #94a3b8;">Загрузка расписания...</p>
            </div>
        {:else if !currentBranchId}
             <div class="empty-state-msg">
                <span class="icon">🏢</span>
                <p>Выберите филиал в меню сверху</p>
                <button on:click={() => branchStore.refresh()} style="margin-top: 10px; font-size: 10px;">Обновить список филиалов</button>
            </div>
        {:else if staff.length === 0 && !isLoading}
            <div class="empty-state-msg">
                <span class="icon">👥</span>
                <p>В этом филиале нет сотрудников на выбранную дату</p>
            </div>
        {:else}
            <DayTimeline
                day={$selectedDate}
                {appointments}
                staff={displayedStaff}
                on:appointmentTap={handleAppointment}
                on:emptySlotTap={handleEmptySlot}
                on:staffTap={handleStaffTap}
            />
        {/if}
    </div>
</div>

<style>
    .schedule-screen { display: flex; flex-direction: column; height: 100%; width: 100%; background: #fdf6e3; overflow: hidden; }
    .timeline-body { flex: 1; overflow: hidden; width: 100%; position: relative; }
    .center-box { display: flex; flex-direction: column; justify-content: center; align-items: center; height: 100%; }
    .empty-state-msg { display: flex; flex-direction: column; justify-content: center; align-items: center; height: 100%; color: #94a3b8; font-weight: 600; padding: 20px; text-align: center; }
    .empty-state-msg .icon { font-size: 32px; margin-bottom: 12px; }
    .spinner { width: 30px; height: 30px; border: 3px solid #eee8d5; border-top-color: #268bd2; border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
</style>
