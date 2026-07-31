<script>
    import { onMount } from 'svelte';
    import { user, logout } from '$lib/stores/auth.js';
    import { goto } from '$app/navigation';
    import ScheduleScreen from '$lib/components/schedule/ScheduleScreen.svelte';
    import AppointmentEditScreen from '$lib/components/schedule/AppointmentEditScreen.svelte';
    import AppointmentEditMobile from '$lib/components/schedule/AppointmentEditMobile.svelte';
    import AppointmentDetailScreen from '$lib/components/schedule/AppointmentDetailScreen.svelte';
    import AppointmentDetailMobile from '$lib/components/schedule/AppointmentDetailMobile.svelte';
    import ResourceEditScreen from '$lib/components/resources/ResourceEditScreen.svelte';
    import { isMobile } from '$lib/stores/ui.js';
    import { selectedDate, activeBranchId } from '$lib/stores/dashboardStore.js';
    import { branchStore } from '$lib/stores/branchStore.js';
    import { activeNiche } from '$lib/stores/nicheStore.js';
    import { managerService } from '$lib/services/managerService.js';
    import { resourceService } from '$lib/services/resourceService.js';
    import { timeUtils } from '$lib/utils/timeUtils.js';
    import { fade, scale } from 'svelte/transition';
    import { portal } from '$lib/actions/portal.js';

    let showModal = null;
    let currentAppointment = null;
    let selectedResource = null;
    let preselectedData = null;
    let appointmentEditRef;
    let scheduleScreenRef;

    onMount(() => {
        if (window.Telegram && window.Telegram.WebApp) {
            window.Telegram.WebApp.BackButton.hide();
        }

        if ($user && $user.role !== 'MANAGER' && $user.role !== 'ADMIN') {
            goto('/');
        }
    });

    function openNewAppointment(event) {
        preselectedData = {
            date: $selectedDate,
            hour: event?.detail?.hour || 10,
            min: event?.detail?.min || 0,
            staffId: event?.detail?.staffId || null,
            resourceId: event?.detail?.resourceId || null
        };
        currentAppointment = null;
        showModal = 'edit';
    }

    function openDetail(event) {
        currentAppointment = event.detail;
        showModal = 'detail';
    }

    async function handleResourceTap(event) {
        const resource = await resourceService.getResourceById(event.detail.id);
        if (resource) {
            selectedResource = resource;
            showModal = 'resource';
        }
    }

    function closeModal() {
        showModal = null;
        selectedResource = null;
        if (scheduleScreenRef && typeof scheduleScreenRef.handleRefresh === 'function') {
            scheduleScreenRef.handleRefresh();
        }
    }

    // RENT: после сохранения с изменённой датой «перепрыгиваем» на дату записи.
    function handleSaved(event) {
        const startTime = event?.detail?.startTime;
        if ($activeNiche === 'RENT' && startTime) {
            const branch = $branchStore.find(b => b.id === $activeBranchId);
            const dayStr = timeUtils.toBranchLocalDateStr(startTime, branch?.timezone);
            if (dayStr) selectedDate.set(new Date(dayStr + 'T12:00:00'));
        }
        closeModal();
    }

    function handleLogout() {
        logout();
        goto('/');
    }
</script>

<div class="manager-shell">
    <header class="header">
        <div class="user-info">
            <div class="avatar">M</div>
            <div class="text">
                <h2>{$user?.name || 'Менеджер'}</h2>
                <p>Управление расписанием</p>
            </div>
        </div>
        <div class="header-actions">
            <button class="btn-add" on:click={() => openNewAppointment({ detail: {} })}>+ Запись</button>
            <button class="logout-btn" on:click={handleLogout}>Выйти</button>
        </div>
    </header>

    <main class="content">
        <ScheduleScreen
            bind:this={scheduleScreenRef}
            on:emptySlotTap={openNewAppointment}
            on:appointmentTap={openDetail}
            on:resourceTap={handleResourceTap}
        />
    </main>
</div>

{#if showModal}
    <div class="modal-backdrop" use:portal on:mousedown|self={closeModal} transition:fade={{duration: 200}}>
        <div class="modal-content" transition:scale={{start: 0.95, duration: 200}}>
            <header class="modal-header">
                <h3>
                    {#if showModal === 'edit'}
                        {currentAppointment ? 'Редактирование записи' : 'Создание записи'}
                    {:else if showModal === 'detail'} {$activeNiche === 'RENT' ? 'Детали аренды' : 'Детали визита'}
                    {/if}
                </h3>
                <button class="close-btn" on:click={closeModal}>✕</button>
            </header>
            <div class="modal-body">
                {#if showModal === 'edit'}
                    {#if $isMobile}
                        <AppointmentEditMobile bind:this={appointmentEditRef} appointment={currentAppointment} preselected={preselectedData} service={managerService} on:cancel={closeModal} on:saved={handleSaved} />
                    {:else}
                        <AppointmentEditScreen bind:this={appointmentEditRef} appointment={currentAppointment} preselected={preselectedData} service={managerService} on:cancel={closeModal} on:saved={handleSaved} />
                    {/if}
                {:else if showModal === 'detail'}
                    {#if $isMobile}
                        <AppointmentDetailMobile appointment={currentAppointment} service={managerService} on:edit={(e) => { currentAppointment = e.detail; showModal = 'edit'; }} on:deleted={closeModal} />
                    {:else}
                        <AppointmentDetailScreen appointment={currentAppointment} service={managerService} on:edit={(e) => { currentAppointment = e.detail; showModal = 'edit'; }} on:deleted={closeModal} />
                    {/if}
                {/if}
            </div>
            </div>
        </div>
    {/if}

    {#if showModal === 'resource'}
        <div use:portal>
            <ResourceEditScreen resource={selectedResource} on:cancel={closeModal} on:success={closeModal} on:photoUpdated={closeModal} />
        </div>
    {/if}

<style>
    .manager-shell { display: flex; flex-direction: column; min-height: 100vh; background: #fdf6e3; }

    .header {
        display: flex; justify-content: space-between; align-items: center;
        padding: 12px 20px; background: #eee8d5; border-bottom: 1.5px solid #ddd6c1;
    }
    .user-info { display: flex; align-items: center; gap: 12px; }
    .avatar {
        width: 36px; height: 36px; background: #8b5cf6; color: white;
        border-radius: 10px; display: flex; justify-content: center; align-items: center; font-weight: 800;
    }
    h2 { font-size: 15px; margin: 0; color: #073642; }
    .text p { margin: 0; font-size: 11px; color: #586e75; }
    .header-actions { display: flex; align-items: center; gap: 8px; }
    .btn-add {
        background: linear-gradient(135deg, #268bd2 0%, #2aa198 100%); color: white;
        border: none; padding: 10px 18px; border-radius: 14px; font-weight: 800; font-size: 13px; cursor: pointer;
    }
    .logout-btn { background: #fdf6e3; border: 1.5px solid #ddd6c1; padding: 8px 12px; border-radius: 10px; font-size: 12px; color: #586e75; cursor: pointer; }

    .content { flex: 1; overflow: hidden; }

    .modal-backdrop { position: fixed; inset: 0; background: rgba(7, 54, 66, 0.8); backdrop-filter: blur(4px); z-index: 99999; display: flex; align-items: center; justify-content: center; padding: 20px; padding-top: max(20px, calc(env(safe-area-inset-top, 20px) + 12px)); padding-bottom: max(20px, calc(env(safe-area-inset-bottom, 20px) + 12px)); box-sizing: border-box; }
    .modal-content { background: #fdf6e3; width: 100%; max-width: 550px; max-height: calc(100dvh - max(40px, env(safe-area-inset-top, 20px) + env(safe-area-inset-bottom, 20px)) - 40px); border-radius: 32px; display: flex; flex-direction: column; overflow: hidden; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.4); border: 1px solid #ddd6c1; }
    .modal-header { background: #eee8d5; padding: 24px 32px; border-bottom: 1.5px solid #ddd6c1; display: flex; justify-content: space-between; align-items: center; flex-shrink: 0; }
    .modal-header h3 { color: #073642; margin: 0; font-size: 18px; font-weight: 800; }
    .close-btn { background: #fdf6e3; border: 1px solid #ddd6c1; width: 32px; height: 32px; border-radius: 50%; cursor: pointer; color: #586e75; font-weight: bold; display: flex; align-items: center; justify-content: center; }
    .modal-body { flex: 1; overflow-y: auto; background: #fdf6e3; }
</style>
