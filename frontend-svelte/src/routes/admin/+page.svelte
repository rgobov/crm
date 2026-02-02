<script>
    import { onMount, onDestroy } from 'svelte';
    import { user, logout } from '$lib/stores/auth.js';
    import { activeTab } from '$lib/stores/dashboardStore.js';
    import { websocketService } from '$lib/services/websocketService.js';
    import { goto } from '$app/navigation';
    import ManagementTab from '$lib/components/admin/ManagementTab.svelte';
    import CalendarTab from '$lib/components/admin/CalendarTab.svelte';
    import CalendarScreen from '$lib/components/calendar/CalendarScreen.svelte';

    const tabs = {
        management: ManagementTab,
        calendar: CalendarTab,
        timeline: CalendarTab
    };

    let sidebarSelectedDate = null;

    onMount(() => {
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

<div class="admin-layout">
    <!-- ЛЕВОЕ МЕНЮ (Только для ПК) -->
    <aside class="desktop-sidebar">
        <div class="sidebar-header">
            <div class="logo">TN <span>Admin</span></div>
        </div>

        <nav class="sidebar-menu">
            <button class="menu-btn" class:active={$activeTab === 'management'} on:click={() => activeTab.set('management')}>
                <span class="icon">📊</span> База данных
            </button>
            <button class="menu-btn" class:active={$activeTab === 'calendar'} on:click={() => activeTab.set('calendar')}>
                <span class="icon">📅</span> Календарь
            </button>
            <button class="menu-btn" class:active={$activeTab === 'timeline'} on:click={() => activeTab.set('timeline')}>
                <span class="icon">🕒</span> Таймлайн
            </button>
        </nav>

        <div class="sidebar-calendar-box">
            <label>ВЫБОР ДАТЫ</label>
            <CalendarScreen on:dateSelected={handleDateChangeFromSidebar} />
        </div>

        <button class="sidebar-logout" on:click={handleLogout}>Выйти из системы</button>
    </aside>

    <!-- ОСНОВНОЙ КОНТЕНТ -->
    <div class="main-content">
        <header class="mobile-header">
            <div class="user-info">
                <div class="avatar">{$user?.name?.charAt(0) || 'A'}</div>
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

<style>
    .admin-layout { display: flex; min-height: 100vh; background: #f8fafc; }

    /* SIDEBAR (Desktop) */
    .desktop-sidebar {
        width: 300px;
        background: white;
        border-right: 1px solid #f1f5f9;
        display: none;
        flex-direction: column;
        padding: 24px;
        position: sticky;
        top: 0;
        height: 100vh;
    }

    .sidebar-header .logo { font-size: 20px; font-weight: 900; color: #0f172a; margin-bottom: 32px; }
    .sidebar-header span { color: var(--primary-color); }

    .sidebar-menu { display: flex; flex-direction: column; gap: 8px; margin-bottom: 32px; }
    .menu-btn {
        display: flex; align-items: center; gap: 12px;
        padding: 12px 16px; border: none; background: none;
        border-radius: 12px; color: #64748b; font-weight: 700;
        cursor: pointer; transition: all 0.2s; text-align: left;
    }
    .menu-btn:hover { background: #f8fafc; color: #0f172a; }
    .menu-btn.active { background: #eff6ff; color: var(--primary-color); }

    .sidebar-calendar-box { flex: 1; overflow-y: auto; scrollbar-width: none; }
    .sidebar-calendar-box::-webkit-scrollbar { display: none; }
    .sidebar-calendar-box label { display: block; font-size: 10px; font-weight: 800; color: #94a3b8; margin-bottom: 12px; letter-spacing: 1px; }

    .sidebar-logout { margin-top: 24px; padding: 14px; border-radius: 12px; border: 1.5px solid #f1f5f9; background: white; color: #ef4444; font-weight: 700; cursor: pointer; }

    /* MAIN CONTENT */
    .main-content { flex: 1; display: flex; flex-direction: column; min-width: 0; }

    .mobile-header {
        display: flex; justify-content: space-between; align-items: center;
        padding: 16px 20px; background: white; border-bottom: 1px solid #f1f5f9;
    }
    .user-info { display: flex; align-items: center; gap: 12px; }
    .avatar { width: 36px; height: 36px; background: var(--primary-gradient); color: white; border-radius: 10px; display: flex; justify-content: center; align-items: center; font-weight: 800; }
    h2 { font-size: 15px; margin: 0; }
    .text p { margin: 0; font-size: 11px; color: #94a3b8; font-weight: 700; text-transform: uppercase; }
    .logout-mini { background: #f1f5f9; border: none; padding: 6px 12px; border-radius: 8px; font-size: 11px; font-weight: 700; color: #64748b; cursor: pointer; }

    .tab-viewport { flex: 1; overflow-y: auto; padding-bottom: 80px; }

    .mobile-nav { display: flex; position: fixed; bottom: 0; left: 0; right: 0; background: white; padding: 10px 0; border-top: 1px solid #f1f5f9; z-index: 1000; }
    .nav-item { display: flex; flex-direction: column; align-items: center; gap: 4px; border: none; background: none; color: #94a3b8; cursor: pointer; flex: 1; }
    .nav-item.active { color: var(--primary-color); }
    .nav-item .label { font-size: 10px; font-weight: 800; text-transform: uppercase; }

    /* АДАПТИВНОСТЬ */
    @media (min-width: 1024px) {
        .desktop-sidebar { display: flex; }
        .mobile-nav { display: none; }
        .mobile-header { display: none; }
        .tab-viewport { padding-bottom: 0; }
    }
</style>
