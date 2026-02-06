<script>
    import { onMount } from 'svelte';
    import { activeTab, selectedDate } from '$lib/stores/dashboardStore.js';
    import { websocketService } from '$lib/services/websocketService.js';
    import AdminSidebar from '$lib/components/admin/AdminSidebar.svelte';
    import { goto } from '$app/navigation';
    import { initAuth } from '$lib/stores/auth.js';

    let sidebarWidth = 300;
    let isResizing = false;

    onMount(() => {
        initAuth();
        websocketService.connect();

        // БЛОКИРУЕМ СКРОЛЛ ВСЕГО ТЕЛА БРАУЗЕРА
        document.body.style.overflow = 'hidden';
        document.body.style.height = '100vh';
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

<div class="admin-shell" style="--sidebar-width: {sidebarWidth}px">
    <aside class="sidebar-aside">
        <AdminSidebar on:dateChange={handleDateChange} />
        <div class="resize-handle" on:mousedown={startResizing}></div>
    </aside>

    <main class="content-body">
        <slot />

        <nav class="mobile-nav">
            <button class="nav-item" class:active={$activeTab === 'management'} on:click={() => { activeTab.set('management'); goto('/admin'); }}>
                <span>📊</span><span class="label">Главная</span>
            </button>
            <button class="nav-item" class:active={$activeTab === 'timeline'} on:click={() => { activeTab.set('timeline'); goto('/admin'); }}>
                <span>🕒</span><span class="label">Таймлайн</span>
            </button>
        </nav>
    </main>
</div>

<style>
    /* Глобальные фиксы для SPA */
    :global(html, body) {
        margin: 0; padding: 0;
        height: 100vh; width: 100vw;
        overflow: hidden; /* ГАРАНТИРУЕМ УДАЛЕНИЕ ВТОРОГО СКРОЛЛА */
    }

    .admin-shell { display: flex; height: 100vh; width: 100vw; background: #f8fafc; overflow: hidden; }

    .sidebar-aside { width: var(--sidebar-width); background: white; border-right: 1px solid #f1f5f9; height: 100%; display: none; flex-shrink: 0; position: relative; }
    .resize-handle { position: absolute; top: 0; right: -4px; width: 8px; height: 100%; cursor: col-resize; z-index: 10; }

    .content-body { flex: 1; display: flex; flex-direction: column; height: 100%; overflow: hidden; position: relative; }

    .mobile-nav { display: flex; position: fixed; bottom: 0; left: 0; right: 0; background: white; padding: 10px 0; border-top: 1px solid #f1f5f9; z-index: 1000; }
    .nav-item { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 4px; border: none; background: none; color: #94a3b8; }
    .nav-item.active { color: var(--primary-color); }

    @media (min-width: 1024px) { .sidebar-aside { display: flex; } .mobile-nav { display: none; } }
</style>
