<script>
    import CalendarScreen from '$lib/components/calendar/CalendarScreen.svelte';
    import { activeTab, selectedDate } from '$lib/stores/dashboardStore.js';
    import { logout } from '$lib/stores/auth.js';
    import { goto } from '$app/navigation';
    import { page } from '$app/stores';
    import { createEventDispatcher } from 'svelte';

    const dispatch = createEventDispatcher();

    const menuItems = [
        { id: 'management', label: 'Главная', icon: '📊' },
        { id: 'timeline', label: 'Таймлайн', icon: '🕒' }
    ];

    function handleNav(id) {
        activeTab.set(id);
        if ($page.url.pathname !== '/admin') goto('/admin');
    }

    function handleDateSelected(event) {
        selectedDate.set(new Date(event.detail.date));
        activeTab.set('timeline');
        if ($page.url.pathname !== '/admin') goto('/admin');
        dispatch('dateChange', event.detail);
    }

    function handleLogout() {
        logout();
        goto('/');
    }
</script>

<aside class="sidebar">
    <div class="sidebar-content">
        <div class="logo-section">
            <div class="logo-icon">999</div>
            <div class="logo-text">
                <h1>CRM Система</h1>
                <span>ADMIN PANEL</span>
            </div>
        </div>

        <nav class="nav-menu">
            {#each menuItems as item}
                <button
                    class="nav-btn"
                    class:active={$activeTab === item.id}
                    on:click={() => handleNav(item.id)}
                >
                    <span class="icon">{item.icon}</span>
                    <span class="label">{item.label}</span>
                </button>
            {/each}
        </nav>

        <div class="sidebar-calendar-section">
            <div class="cal-label">ВЫБОР ДАТЫ</div>
            <div class="mini-calendar">
                <CalendarScreen on:dateSelected={handleDateSelected} />
            </div>
        </div>

        <div class="sidebar-footer">
            <button class="logout-btn-desktop" on:click={handleLogout}>
                <span class="icon">🚪</span>
                <span class="label">Выйти</span>
            </button>
        </div>
    </div>
</aside>

<style>
    .sidebar { width: 100%; height: 100%; background: white; display: flex; flex-direction: column; overflow: hidden; border-right: 1px solid #f1f5f9; }
    .sidebar-content { padding: 24px; display: flex; flex-direction: column; height: 100%; gap: 28px; }

    .logo-section { display: flex; align-items: center; gap: 12px; }
    .logo-icon { width: 40px; height: 40px; background: var(--primary-gradient); color: white; border-radius: 12px; display: flex; justify-content: center; align-items: center; font-weight: 900; font-size: 16px; flex-shrink: 0; }

    /* ФИКС РАСТЯНУТОСТИ */
    .logo-text h1 { font-size: 17px; margin: 0; color: #0f172a; font-weight: 800; letter-spacing: -0.2px; }
    .logo-text span { font-size: 9px; color: #94a3b8; font-weight: 700; letter-spacing: 0.5px; opacity: 0.8; }

    .nav-menu { display: flex; flex-direction: column; gap: 4px; }
    .nav-btn { display: flex; align-items: center; gap: 12px; padding: 10px 14px; border: none; background: none; border-radius: 12px; color: #64748b; font-weight: 700; cursor: pointer; transition: 0.2s; }
    .nav-btn:hover { background: #f8fafc; color: #0f172a; }
    .nav-btn.active { background: #eff6ff; color: var(--primary-color); }

    .sidebar-calendar-section { border-top: 1px solid #f1f5f9; padding-top: 20px; flex: 1; overflow-y: auto; scrollbar-width: none; }
    .sidebar-calendar-section::-webkit-scrollbar { display: none; }
    .cal-label { font-size: 10px; font-weight: 800; color: #cbd5e1; margin-bottom: 12px; letter-spacing: 0.5px; }

    .sidebar-footer { margin-top: auto; padding-top: 16px; border-top: 1px solid #f1f5f9; }
    .logout-btn-desktop { display: flex; align-items: center; gap: 10px; width: 100%; padding: 10px 14px; border: none; background: #fef2f2; color: #ef4444; border-radius: 10px; font-weight: 700; font-size: 13px; cursor: pointer; }

    :global(.sidebar .calendar-container) { padding: 0 !important; border: none !important; box-shadow: none !important; }
</style>
