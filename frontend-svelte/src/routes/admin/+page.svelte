<script>
    import { onMount, onDestroy } from 'svelte';
    import { user, logout, token } from '$lib/stores/auth.js';
    import { activeTab } from '$lib/stores/dashboardStore.js';
    import { websocketService } from '$lib/services/websocketService.js';
    import { goto } from '$app/navigation';
    import { get } from 'svelte/store';

    import ManagementTab from '$lib/components/admin/ManagementTab.svelte';
    import CalendarTab from '$lib/components/admin/CalendarTab.svelte';
    import AdminSidebar from '$lib/components/admin/AdminSidebar.svelte';

    const tabs = {
        management: ManagementTab,
        calendar: CalendarTab,
        timeline: CalendarTab
    };

    let sidebarSelectedDate = null;
    let isInitialized = false;

    onMount(() => {
        // 1. ПРОВЕРКА АВТОРИЗАЦИИ (GUARD)
        const currentToken = localStorage.getItem('token') || get(token);
        const currentUser = JSON.parse(localStorage.getItem('user') || 'null') || get(user);

        if (!currentToken || !currentUser || currentUser.role !== 'ADMIN') {
            console.error('Access denied: Unauthorized or not Admin');
            goto('/');
            return;
        }

        isInitialized = true;

        if (window.Telegram && window.Telegram.WebApp) {
            window.Telegram.WebApp.BackButton.hide();
        }

        websocketService.connect();
    });

    onDestroy(() => {
        websocketService.disconnect();
    });

    function handleDateChangeFromSidebar(event) {
        sidebarSelectedDate = event.detail.date;
        activeTab.set('timeline');
    }

    function handleLogout() {
        logout();
        goto('/');
    }
</script>

{#if isInitialized}
<div class="admin-layout">
    <!-- ЛЕВОЕ МЕНЮ (ПК) -->
    <aside class="desktop-sidebar">
        <AdminSidebar on:dateChange={handleDateChangeFromSidebar} />
    </aside>

    <!-- ОСНОВНОЙ КОНТЕНТ -->
    <div class="main-content">
        <header class="mobile-header">
            <div class="user-info">
                <div class="avatar">999</div>
                <div class="text">
                    <h2>{$user?.name || 'Администратор'}</h2>
                    <p>{$activeTab === 'management' ? 'Управление' : 'Расписание'}</p>
                </div>
            </div>
            <button class="logout-mini" on:click={handleLogout}>Выйти</button>
        </header>

        <div class="tab-viewport">
            <svelte:component
                this={tabs[$activeTab]}
                forcedDate={sidebarSelectedDate}
            />
        </div>

        <!-- МОБИЛЬНАЯ НАВИГАЦИЯ -->
        <nav class="mobile-nav">
            <button class="nav-item" class:active={$activeTab === 'management'} on:click={() => activeTab.set('management')}>
                <span class="icon">📊</span><span class="label">База</span>
            </button>
            <button class="nav-item" class:active={$activeTab === 'calendar'} on:click={() => activeTab.set('calendar')}>
                <span class="icon">📅</span><span class="label">Календарь</span>
            </button>
            <button class="nav-item" class:active={$activeTab === 'timeline'} on:click={() => activeTab.set('timeline')}>
                <span class="icon">🕒</span><span class="label">Таймлайн</span>
            </button>
        </nav>
    </div>
</div>
{/if}

<style>
    /* Стили идентичны предыдущим, но добавили плавность появления */
    .admin-layout { display: flex; min-height: 100vh; background: #f8fafc; animation: fadeIn 0.3s ease-in; }
    @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }

    .desktop-sidebar { width: 320px; background: white; border-right: 1px solid #f1f5f9; display: none; position: sticky; top: 0; height: 100vh; }
    .main-content { flex: 1; display: flex; flex-direction: column; min-width: 0; }
    .mobile-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; background: white; border-bottom: 1px solid #f1f5f9; }
    .user-info { display: flex; align-items: center; gap: 12px; }
    .avatar { width: 36px; height: 36px; background: var(--primary-gradient); color: white; border-radius: 10px; display: flex; justify-content: center; align-items: center; font-weight: 900; font-size: 14px; }
    h2 { font-size: 15px; margin: 0; }
    .text p { margin: 0; font-size: 11px; color: #94a3b8; font-weight: 700; text-transform: uppercase; }
    .logout-mini { background: #f1f5f9; border: none; padding: 6px 12px; border-radius: 8px; font-size: 11px; font-weight: 700; color: #64748b; cursor: pointer; }
    .tab-viewport { flex: 1; overflow-y: auto; padding-bottom: 80px; }
    .mobile-nav { display: flex; position: fixed; bottom: 0; left: 0; right: 0; background: white; padding: 10px 0; border-top: 1px solid #f1f5f9; z-index: 1000; }
    .nav-item { display: flex; flex-direction: column; align-items: center; gap: 4px; border: none; background: none; color: #94a3b8; cursor: pointer; flex: 1; }
    .nav-item.active { color: var(--primary-color); }
    @media (min-width: 1024px) {
        .desktop-sidebar { display: block; }
        .mobile-nav, .mobile-header { display: none; }
        .tab-viewport { padding-bottom: 0; }
    }
</style>
