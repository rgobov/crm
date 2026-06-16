<script>
    import { onMount, onDestroy, createEventDispatcher } from 'svelte';
    import { adminService } from '$lib/services/adminService.js';
    import { clientService } from '$lib/services/clientService.js';
    import { scheduleRefreshSignal } from '$lib/services/websocketService.js';
    import { dbService } from '$lib/services/dbService.js';
    import { selectedDate, activeBranchId } from '$lib/stores/dashboardStore.js';
    import { branchStore } from '$lib/stores/branchStore.js';
    import DayTimeline from './DayTimeline.svelte';
    import api from '$lib/api.js';

    export let onlyBusyStaff = false;
    export let onlyWorkingStaff = false;
    export let branchId = null;
    export let isClient = false;

    $: service = isClient ? clientService : adminService;

    const dispatch = createEventDispatcher();

    let appointments = [];
    let staff = [];
    let isLoading = false;
    let lastLoadTime = 0;
    let refreshTimeout;
    let staffRefreshTimeout = null; // Дебаунсинг для обновления сотрудников

    // ПРИОРИТЕТ: Проп, если он есть, иначе Стор.
    $: currentBranchId = branchId || $activeBranchId;

    async function fetchAppointments(date, bId, silent = false) {
        if (!date || !bId) return;
        if (!silent) isLoading = true;
        try {
            console.log(`📡 Fetching appointments for date: ${date}, branch: ${bId}`);
            // Используем bypassCache для обновлений по сигналу или смене даты,
            // чтобы точно получить актуальные данные из БД
            const apptsData = await service.getAppointmentsForDay(date, bId, { bypassCache: true });
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

        let attempts = 0;
        const maxAttempts = 2;

        while (attempts < maxAttempts) {
            try {
                console.log(`📡 Fetching staff for date: ${date}, branch: ${bId} (Attempt ${attempts + 1})`);
                const staffData = await service.getStaffForSchedule(date, bId, { bypassCache: true });

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

                console.log('📊 Staff loaded:', staff.map(s => ({
                    id: s.id,
                    name: s.name,
                    workStartTime: s.workStartTime,
                    workEndTime: s.workEndTime,
                    dayOff: s.dayOff
                })));

                // Ленивая загрузка недостающих фото
                staff.forEach(member => {
                    loadStaffPhoto(member.id, member.photoUpdatedAt).then(photo => {
                        if (photo && member.photoData !== photo) {
                            staff = staff.map(s => s.id === member.id ? {...s, photoData: photo} : s);
                        }
                    });
                });

                // Если успешно - выходим из цикла
                break;
            } catch (e) {
                attempts++;
                console.error(`❌ Error loading staff (Attempt ${attempts}):`, e);
                if (attempts < maxAttempts) {
                    console.log('⏳ Retrying in 1s...');
                    await new Promise(resolve => setTimeout(resolve, 1000));
                }
            } finally {
                if (!silent && attempts === maxAttempts) isLoading = false;
                if (attempts === maxAttempts) {
                   // Optional: show user-friendly error if all attempts failed
                }
            }
        }
        if (!silent) isLoading = false;
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
            const photoUrl = isClient ? `/client/schedule/staff/${staffId}/photo` : `/admin/schedule/staff/${staffId}/photo`;
            const response = await api.get(photoUrl);
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
        console.log('📥 WS: Signal received:', signal);
        if (signal && signal.ts > 0 && currentBranchId) {
            const { type, staffId, appointmentId, date, branchId } = signal;
            const currentLocalDate = toLocalDbDate($selectedDate);

            console.log('🔍 WS: Current state:', {
                currentBranchId,
                currentLocalDate,
                signalType: type,
                signalBranchId: branchId,
                signalDate: date
            });

            // Проверка релевантности: только если наш филиал и наш день
            // Если в сигнале нет branchId или date — считаем его глобальным
            const isRelevantBranch = !branchId || branchId === currentBranchId;
            const isRelevantDate = !date || date === currentLocalDate;

            console.log('🔍 WS: Relevance check:', {
                isRelevantBranch,
                isRelevantDate
            });

            if (!isRelevantBranch || !isRelevantDate) {
                console.log('⏭️ WS: Signal ignored - not relevant');
                return;
            }

            if (type === 'STAFF_SHIFT_UPDATED') {
                console.log(`🎯 WS: Staff shift updated - debounced refresh`);
                clearTimeout(staffRefreshTimeout);
                staffRefreshTimeout = setTimeout(() => {
                    fetchStaff($selectedDate, currentBranchId, true);
                }, 500);
            } else if (type === 'STAFF_UPDATED' || type === 'STAFF_DELETED') {
                console.log(`🎯 WS: Staff profile updated (${type}) - refreshing staff`);
                fetchStaff($selectedDate, currentBranchId, true);
            } else if (type && type.startsWith('APPOINTMENT_')) {
                console.log(`🎯 WS: Appointment updated (${type}) - refreshing appointments only`);
                fetchAppointments($selectedDate, currentBranchId, true);
            } else {
                console.log(`📢 WS: General signal (${type || 'unknown'}) - full refresh`);
                loadDayData($selectedDate, currentBranchId, true);
            }
        }
    });

    function toLocalDbDate(date) {
        if (!date) return '';
        const d = new Date(date);
        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    }

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
        clearTimeout(staffRefreshTimeout);
    });

    function handleEmptySlot(event) { dispatch('emptySlotTap', event.detail); }
    function handleAppointment(event) { dispatch('appointmentTap', event.detail); }
    function handleStaffTap(event) { dispatch('staffTap', event.detail); }
    export function handleRefresh() {
        console.log('🔄 Refresh triggered from timeline component');
        if ($selectedDate && currentBranchId) {
            loadDayData($selectedDate, currentBranchId, true);
        }
    }
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
                on:refresh={handleRefresh}
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
