<script>
import { onMount } from 'svelte';
import { get } from 'svelte/store';
import { activeTab, selectedDate, activeBranchId } from '$lib/stores/dashboardStore.js';
    import { branchStore } from '$lib/stores/branchStore.js';
    import { websocketService } from '$lib/services/websocketService.js';
    import AdminSidebar from '$lib/components/admin/AdminSidebar.svelte';
    import MobileAdminShell from '$lib/components/mobile/MobileAdminShell.svelte';
    import { isMobile, isTablet } from '$lib/stores/ui.js';
    import { goto } from '$app/navigation';
    import { initAuth } from '$lib/stores/auth.js';
    import { activeModal, closeModal } from '$lib/stores/modalStore.js'; // ИМПОРТ
    import TelegramSettingsModal from '$lib/components/admin/TelegramSettingsModal.svelte';
    import NotificationTemplatesModal from '$lib/components/admin/NotificationTemplatesModal.svelte';
    import { fade, scale } from 'svelte/transition';

    let sidebarWidth = 300;
    let isResizing = false;

    onMount(async () => {
        initAuth();
        websocketService.connect();
        document.body.style.overflow = 'hidden';
        document.body.style.height = '100vh';

        try {
            await branchStore.refresh();
            const branches = get(branchStore);
            const savedBranchId = get(activeBranchId);
            const hasSavedBranch = savedBranchId && branches.some(branch => String(branch.id) === String(savedBranchId));

            // Не выбираем филиал молча: пользователь должен явно подтвердить контекст работы.
            if (savedBranchId && !hasSavedBranch) {
                activeBranchId.set(null);
            }
        } catch (e) { /* ignore */ }
    });

    function startResizing() {
        isResizing = true;
        const move = (e) => { if (isResizing) sidebarWidth = Math.min(Math.max(e.clientX, 260), 450); };
        const up = () => { isResizing = false; window.removeEventListener('mousemove', move); window.removeEventListener('mouseup', up); };
        window.addEventListener('mousemove', move);
        window.addEventListener('mouseup', up);
    }

    function handleDateChange(event) {
        selectedDate.set(new Date(event.detail.date));
        activeTab.set('timeline');
        goto('/admin');
    }
</script>

{#if $isMobile || $isTablet}
    <MobileAdminShell>
        <slot />
    </MobileAdminShell>
{:else}
    <div class="admin-shell" style="--sidebar-width: {sidebarWidth}px">
        <aside class="sidebar-aside">
            <AdminSidebar on:dateChange={handleDateChange} />
            <div class="resize-handle" on:mousedown={startResizing}></div>
        </aside>
        <main class="content-body">
            <slot />
        </main>
    </div>
{/if}

<!-- ГЛОБАЛЬНЫЕ МОДАЛЬНЫЕ ОКНА -->
{#if $activeModal}
    <div class="global-modal-backdrop" on:mousedown|self={closeModal} transition:fade={{duration: 200}}>
        <div class="global-modal-content" transition:scale={{start: 0.95, duration: 200}}>
            {#if $activeModal === 'telegram'}
                <TelegramSettingsModal on:close={closeModal} />
            {:else if $activeModal === 'templates'}
                <NotificationTemplatesModal on:close={closeModal} />
            {/if}
        </div>
    </div>
{/if}

<style>
    :global(html, body) { margin: 0; padding: 0; height: 100vh; width: 100vw; overflow: hidden; }
    .admin-shell {
        display: flex;
        height: 100vh;
        width: 100vw;
        background: #fdf6e3;
        overflow: hidden;
    }
    .sidebar-aside {
        width: var(--sidebar-width);
        background: #eee8d5;
        border-right: 1.5px solid #ddd6c1;
        height: 100%;
        display: flex;
        flex-shrink: 0;
        position: relative;
    }
    .resize-handle { position: absolute; top: 0; right: -4px; width: 8px; height: 100%; cursor: col-resize; z-index: 10; }
    .content-body { flex: 1; display: flex; flex-direction: column; height: 100%; overflow-y: auto; position: relative; }

    .global-modal-backdrop { position: fixed; inset: 0; background: rgba(0, 43, 54, 0.6); backdrop-filter: blur(4px); z-index: 3000; display: flex; align-items: center; justify-content: center; padding: 20px; }
    .global-modal-content { background: #fdf6e3; width: 100%; max-width: 550px; border-radius: 32px; overflow: hidden; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.4); border: 1px solid #ddd6c1; }
</style>
