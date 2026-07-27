<script>
    import { onMount, onDestroy, createEventDispatcher } from 'svelte';
    import { adminService } from '$lib/services/adminService.js';
    import { clientService } from '$lib/services/clientService.js';
    import { resourceService } from '$lib/services/resourceService.js';
    import { scheduleRefreshSignal } from '$lib/services/websocketService.js';
    import { dbService } from '$lib/services/dbService.js';
    import { selectedDate, activeBranchId } from '$lib/stores/dashboardStore.js';
    import { branchStore } from '$lib/stores/branchStore.js';
    import { activeNiche } from '$lib/stores/nicheStore.js';
    import { timeUtils } from '$lib/utils/timeUtils.js';
    import DayTimeline from './DayTimeline.svelte';
    import api from '$lib/api.js';

    export let onlyBusyStaff = false;
    export let onlyWorkingStaff = false;
    export let branchId = null;
    export let isClient = false;

    $: service = isClient ? clientService : adminService;
    $: isRentMode = $activeNiche === 'RENT';
    $: columnKey = isRentMode ? 'resourceId' : 'staffMemberId';

    const dispatch = createEventDispatcher();

    let appointments = [];
    let staff = [];
    let resources = [];
    let isLoading = false;
    let lastLoadTime = 0;
    let refreshTimeout;
    let staffRefreshTimeout = null;

    // ПРИОРИТЕТ: Проп, если он есть, иначе Стор.
    $: currentBranchId = branchId || $activeBranchId;
    $: currentBranch = $branchStore.find(b => b.id === currentBranchId);
    $: branchTimezone = currentBranch?.timezone || 'Europe/Moscow';
    $: branchLocalDate = timeUtils.toBranchLocalDateStr($selectedDate, branchTimezone);

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
            isRentMode ? fetchResources(bId) : fetchStaff(date, bId, true)
        ]);
        isLoading = false;
    }

    async function fetchResources(bId) {
        if (!bId) return;
        try {
            const data = isClient ? await clientService.getResources(bId) : await resourceService.getResources(bId);
            const newResources = Array.isArray(data) ? data : [];
            // Soft update: сохраняем фото если уже есть в памяти
            resources = newResources.map(nr => {
                const existing = resources.find(or => or.id === nr.id);
                if (existing && existing.photoData) {
                    return { ...nr, photoData: existing.photoData };
                }
                return nr;
            });
            // Ленивая загрузка недостающих фото
            resources.forEach(r => {
                loadResourcePhoto(r.id, r.photoUpdatedAt).then(photo => {
                    if (photo && r.photoData !== photo) {
                        resources = resources.map(x => x.id === r.id ? {...x, photoData: photo} : x);
                    }
                });
            });
        } catch (e) {
            console.error('❌ Error loading resources:', e);
            resources = [];
        }
    }

    async function loadResourcePhoto(resourceId, updatedAtOnServer) {
        if (!resourceId) return null;
        const cachedRecord = await dbService.getPhoto(resourceId);
        if (cachedRecord && cachedRecord.photoData && updatedAtOnServer) {
            if (cachedRecord.updatedAt >= updatedAtOnServer) {
                return cachedRecord.photoData;
            }
        }
        try {
            const url = isClient ? `/client/resources/${resourceId}/photo` : `/admin/resources/${resourceId}/photo`;
            const response = await api.get(url);
            const photoData = response.data.photoData;
            if (photoData) {
                await dbService.savePhoto(resourceId, photoData, updatedAtOnServer || Date.now());
                return photoData;
            }
            return cachedRecord ? cachedRecord.photoData : null;
        } catch (e) {
            console.error(`Error loading resource photo ${resourceId}:`, e);
            return cachedRecord ? cachedRecord.photoData : null;
        }
    }

    function debouncedRefresh() {
        clearTimeout(refreshTimeout);
        refreshTimeout = setTimeout(() => {
            if (branchLocalDate && currentBranchId) {
                console.log('🔄 WS: Debounced refresh (appointments only)');
                fetchAppointments(branchLocalDate, currentBranchId, true);
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

    // В режиме Аренды колонки — это ресурсы (маппинг в fake-staff для DayTimeline).
    // Рабочие часы 00:00–23:59 чтобы все слоты были кликабельны (нет выходных у ресурсов).
    $: rentColumns = resources.map(r => ({
        id: r.id,
        name: r.name,
        specialty: r.description || 'Объект',
        photoData: r.photoData || null,
        dayOff: false,
        workStartTime: '00:00',
        workEndTime: '23:59'
    }));

    $: displayedColumns = isRentMode ? rentColumns : displayedStaff;

    // РЕАКТИВНАЯ ЗАГРУЗКА ПРИ СМЕНЕ ДАТЫ ИЛИ ФИЛИАЛА
    $: if (branchLocalDate && currentBranchId) {
        const now = Date.now();
        if (now - lastLoadTime > 800) {
            lastLoadTime = now;
            loadDayData(branchLocalDate, currentBranchId);
        }
    }
    // Переключение ниши (например, смена филиала на RENT) требует перезагрузки колонок
    $: if (typeof isRentMode !== 'undefined') {
        const now = Date.now();
        if (now - lastLoadTime > 800 && branchLocalDate && currentBranchId) {
            lastLoadTime = now;
            loadDayData(branchLocalDate, currentBranchId);
        }
    }

    const unsubscribe = scheduleRefreshSignal.subscribe(signal => {
        console.log('📥 WS: Signal received:', signal);
        if (signal && signal.ts > 0 && currentBranchId) {
            const { type, staffId, appointmentId, date, branchId } = signal;
            const currentLocalDate = branchLocalDate;

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
                    fetchStaff(branchLocalDate, currentBranchId, true);
                }, 500);
            } else if (type === 'STAFF_UPDATED' || type === 'STAFF_DELETED') {
                console.log(`🎯 WS: Staff profile updated (${type}) - refreshing staff`);
                fetchStaff(branchLocalDate, currentBranchId, true);
            } else if (type && type.startsWith('APPOINTMENT_')) {
                console.log(`🎯 WS: Appointment updated (${type}) - refreshing appointments only`);
                fetchAppointments(branchLocalDate, currentBranchId, true);
            } else {
                console.log(`📢 WS: General signal (${type || 'unknown'}) - full refresh`);
                loadDayData(branchLocalDate, currentBranchId, true);
            }
        }
    });

    onMount(async () => {
        if (branchStore && typeof $branchStore !== 'undefined' && $branchStore.length === 0) {
            branchStore.refresh();
        }
        if (branchLocalDate && currentBranchId) {
            await loadDayData(branchLocalDate, currentBranchId);
        }
    });

    onDestroy(() => {
        unsubscribe();
        clearTimeout(refreshTimeout);
        clearTimeout(staffRefreshTimeout);
    });

    function handleEmptySlot(event) {
        // В режиме Аренды staffId в событии — это id ресурса. Переводим в resourceId.
        if (isRentMode) {
            dispatch('emptySlotTap', {
                hour: event.detail.hour,
                min: event.detail.min,
                staffId: null,
                resourceId: event.detail.staffId
            });
        } else {
            dispatch('emptySlotTap', event.detail);
        }
    }
    function handleAppointment(event) { dispatch('appointmentTap', event.detail); }
    function handleStaffTap(event) { dispatch('staffTap', event.detail); }
    export function handleRefresh() {
        console.log('🔄 Refresh triggered from timeline component');
        if (branchLocalDate && currentBranchId) {
            loadDayData(branchLocalDate, currentBranchId, true);
        }
    }
</script>

<div class="schedule-screen">
    <div class="timeline-body">
        {#if isLoading && displayedColumns.length === 0}
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
        {:else if displayedColumns.length === 0 && !isLoading}
            <div class="empty-state-msg">
                <span class="icon">{isRentMode ? '🏠' : '👥'}</span>
                <p>{isRentMode ? 'В этом филиале нет объектов аренды' : 'В этом филиале нет сотрудников на выбранную дату'}</p>
            </div>
        {:else}
            <DayTimeline
                day={$selectedDate}
                {appointments}
                columns={displayedColumns}
                {columnKey}
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
