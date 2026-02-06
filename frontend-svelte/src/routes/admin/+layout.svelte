<script>
    import { onMount } from 'svelte';
    import { user, token, initAuth } from '$lib/stores/auth.js';
    import { activeTab } from '$lib/stores/dashboardStore.js';
    import { websocketService } from '$lib/services/websocketService.js';
    import AdminSidebar from '$lib/components/admin/AdminSidebar.svelte';
    import { goto } from '$app/navigation';
    import { get } from 'svelte/store';

    let sidebarWidth = 300;
    let isResizing = false;

    onMount(() => {
        initAuth();
        const currentToken = localStorage.getItem('token') || get(token);
        if (!currentToken) goto('/');
        websocketService.connect();
    });

    function startResizing() {
        isResizing = true;
        const move = (e) => { if (isResizing) sidebarWidth = Math.min(Math.max(e.clientX, 260), 450); };
        const up = () => { isResizing = false; window.removeEventListener('mousemove', move); window.removeEventListener('mouseup', up); };
        window.addEventListener('mousemove', move);
        window.addEventListener('mouseup', up);
    }

    function handleSidebarDate(event) {
        // При выборе даты - переходим на главную админки в режим Таймлайна
        activeTab.set('timeline');
        goto('/admin');
    }
</script>

<div class="admin-app-shell" style="--sidebar-width: {sidebarWidth}px">
    <!-- САЙДБАР ВСЕГДА ТУТ -->
    <aside class="desktop-aside">
        <AdminSidebar on:dateChange={handleSidebarDate} />
        <div class="drag-handle" on:mousedown={startResizing}></div>
    </aside>

    <main class="content-frame">
        <slot /> <!-- ТУТ БУДУТ СТРАНИЦЫ: /admin, /admin/staff и т.д. -->

        <!-- МОБИЛЬНОЕ МЕНЮ -->
        <nav class="mobile-bottom-nav">
            <button class="nav-btn" class:active={$activeTab === 'management'} on:click={() => { activeTab.set('management'); goto('/admin'); }}>
                <span>📊</span><span class="lbl">База</span>
            </button>
            <button class="nav-btn" class:active={$activeTab === 'calendar'} on:click={() => { activeTab.set('calendar'); goto('/admin'); }}>
                <span>📅</span><span class="lbl">График</span>
            </button>
        </nav>
    </main>
</div>

<style>
    .admin-app-shell { display: flex; min-height: 100vh; background: #f8fafc; }
    .desktop-aside { width: var(--sidebar-width); background: white; border-right: 1px solid #f1f5f9; position: sticky; top: 0; height: 100vh; display: none; }
    .drag-handle { position: absolute; top: 0; right: -4px; width: 8px; height: 100%; cursor: col-resize; z-index: 10; }
    .content-frame { flex: 1; display: flex; flex-direction: column; min-width: 0; position: relative; }
    .mobile-bottom-nav { display: flex; position: fixed; bottom: 0; left: 0; right: 0; background: white; padding: 10px 0; border-top: 1px solid #f1f5f9; z-index: 1000; }
    .nav-btn { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 4px; border: none; background: none; color: #94a3b8; }
    .nav-btn.active { color: var(--primary-color); }
    @media (min-width: 1024px) { .desktop-aside { display: block; } .mobile-bottom-nav { display: none; } }
</style>
