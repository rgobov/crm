<script>
    import { onMount, onDestroy, createEventDispatcher } from 'svelte';
    import { adminService } from '$lib/services/adminService.js';
    import { scheduleRefreshSignal } from '$lib/services/websocketService.js';
    import { selectedDate, activeBranchId } from '$lib/stores/dashboardStore.js';
    import DayTimeline from './DayTimeline.svelte';
    import api from '$lib/api.js';

    export let onlyBusyStaff = false;
    export let onlyWorkingStaff = false;

    const dispatch = createEventDispatcher();

    let appointments = [];
    let staff = [];
    let isLoading = false;
    let lastLoadTime = 0; // Track last load time to prevent duplicates

    // Lazy loading function for staff photos
    async function loadStaffPhoto(staffId) {
        const cacheKey = `staff_photo_${staffId}`;
        
        // Check cache first
        const cached = localStorage.getItem(cacheKey);
        if (cached) {
            return cached;
        }
        
        try {
            const response = await api.get(`/admin/schedule/staff/${staffId}/photo`);
            const photoData = response.data.photoData;
            
            // Cache the photo data
            if (photoData) {
                localStorage.setItem(cacheKey, photoData);
            }
            
            return photoData;
        } catch (e) {
            console.warn('Failed to load photo for staff:', staffId, e);
            return null;
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

    // РЕАКТИВНАЯ ЗАГРУЗКА
    $: if ($selectedDate && $activeBranchId) {
        const now = Date.now();
        if (now - lastLoadTime > 1000) { // Prevent duplicate loads within 1 second
            console.log('🔄 Schedule: Reactive load for branch:', $activeBranchId);
            loadDayData($selectedDate, $activeBranchId);
            lastLoadTime = now;
        }
    }

    const unsubscribe = scheduleRefreshSignal.subscribe(signal => {
        if (signal && signal.ts > 0 && $activeBranchId) {
            console.log('📥 WS: Refresh signal received, loading data');
            loadDayData($selectedDate, $activeBranchId, true);
            lastLoadTime = Date.now();
        }
    });

    onMount(async () => {
        console.log('🏁 ScheduleScreen mounted. BranchId:', $activeBranchId);
        if ($selectedDate && $activeBranchId) {
            await loadDayData($selectedDate, $activeBranchId);
        }
    });

    onDestroy(() => unsubscribe());

    async function loadDayData(date, bId, silent = false) {
        if (!date || !bId) return;
        if (!silent) isLoading = true;

        try {
            const [apptsData, staffData] = await Promise.all([
                adminService.getAppointmentsForDay(date, bId),
                adminService.getStaffForSchedule(date, bId)
            ]);
            
            console.log('🔍 [DEBUG] staffData type:', typeof staffData);
            console.log('🔍 [DEBUG] staffData isArray:', Array.isArray(staffData));
            console.log('🔍 [DEBUG] staffData length:', staffData?.length);
            console.log('🔍 [DEBUG] staffData sample:', staffData?.slice(0, 2));
            
            appointments = apptsData || [];
            
            // Fix: Handle case where backend returns JSON string instead of array
            let staffArray = staffData;
            if (typeof staffData === 'string') {
                try {
                    staffArray = JSON.parse(staffData);
                    console.log('🔧 [FIX] Parsed staffData from JSON string, count:', staffArray.length);
                } catch (e) {
                    console.error('❌ [ERROR] Failed to parse staffData JSON:', e);
                    staffArray = [];
                }
            } else if (!Array.isArray(staffData)) {
                console.warn('⚠️ [WARN] staffData is not array, using empty array');
                staffArray = [];
            }
            
            staff = staffArray.filter(s => s.role === 'ROLE_EMPLOYEE' || s.role === 'EMPLOYEE');
            console.log('Staff count after filtering:', staff.length);
            
            // Start lazy loading photos for all staff members
            if (staff.length > 0) {
                console.log('Starting lazy loading of photos for', staff.length, 'staff members');
                staff.forEach(async (staffMember, index) => {
                    const photoData = await loadStaffPhoto(staffMember.id);
                    if (photoData) {
                        // Update staff member with photo data
                        staff = staff.map(s => 
                            s.id === staffMember.id ? {...s, photoData} : s
                        );
                        console.log(`Loaded photo for staff ${index + 1}/${staff.length}:`, staffMember.name);
                    }
                });
            }
            
            console.log('Day data loaded. Staff count:', staff.length);
        } catch (e) {
            console.error('❌ Schedule Screen API Error:', e);
        } finally {
            isLoading = false;
        }
    }

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
        {:else if !$activeBranchId}
             <div class="empty-state-msg">
                <span class="icon">🏢</span>
                <p>Выберите филиал в меню сверху</p>
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
