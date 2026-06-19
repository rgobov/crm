<script>
    import CalendarScreen from '$lib/components/calendar/CalendarScreen.svelte';
    import { activeTab, selectedDate, activeBranchId } from '$lib/stores/dashboardStore.js';
    import { logout } from '$lib/stores/auth.js';
    import { branchStore } from '$lib/stores/branchStore.js';
    import { goto } from '$app/navigation';
    import { page } from '$app/stores';
    import { createEventDispatcher, onMount } from 'svelte';

    const dispatch = createEventDispatcher();

    let isLoadingBranches = true;

    $: branches = $branchStore;

    const menuItems = [
        { id: 'management', label: 'Главная', icon: '📊' },
        { id: 'timeline', label: 'Таймлайн', icon: '🕒' },
        { id: 'ai', label: 'AI', icon: '🤖' }
    ];

    async function loadBranches() {
        try {
            isLoadingBranches = true;
            await branchStore.refresh();
            if ($branchStore.length > 0 && !$activeBranchId) {
                activeBranchId.set($branchStore[0].id);
            }
        } catch (e) {
            console.error('Sidebar: Failed to load branches', e);
        } finally {
            isLoadingBranches = false;
        }
    }

    onMount(() => {
        loadBranches();
    });

    function handleNav(id) {
        if (id === 'ai') {
            goto('/admin/settings/ai');
            return;
        }
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
    <div class="sidebar-scroll-wrapper">
        <div class="logo-section">
            <div class="logo-icon">999</div>
            <div class="logo-text">
                <h1>CRM Система</h1>
                <span>ADMIN PANEL</span>
            </div>
        </div>

        <div class="branch-nav-section">
            <label class="section-micro-label">ТЕКУЩИЙ ФИЛИАЛ</label>
            <div class="branch-select-box">
                {#if isLoadingBranches}
                    <div class="branch-loading">...</div>
                {:else if branches && branches.length > 0}
                    <select class="branch-select" bind:value={$activeBranchId}>
                        {#each branches as b}
                            <option value={b.id}>{b.name}</option>
                        {/each}
                    </select>
                {:else}
                    <button class="no-branches-btn" on:click={loadBranches}>Обновить</button>
                {/if}
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
    .sidebar {
        width: 100%; height: 100%;
        background: #eee8d5;
        display: flex; flex-direction: column;
        border-right: 1.5px solid #ddd6c1;
        overflow: hidden;
    }

    /* НОВАЯ ОБЕРТКА ДЛЯ СКРОЛЛА ВСЕГО САЙДБАРА */
    .sidebar-scroll-wrapper {
        flex: 1;
        overflow-y: auto;
        display: flex;
        flex-direction: column;
        padding: 24px;
        gap: 24px;
        scrollbar-width: none; /* Скрываем полосу прокрутки для чистоты */
    }
    .sidebar-scroll-wrapper::-webkit-scrollbar { display: none; }

    .logo-section { display: flex; align-items: center; gap: 12px; }
    .logo-icon {
        width: 40px; height: 40px;
        background: var(--primary-gradient);
        color: white; border-radius: 12px;
        display: flex; justify-content: center; align-items: center;
        font-weight: 900; font-size: 16px; flex-shrink: 0;
    }
    .logo-text h1 { font-size: 17px; margin: 0; color: #073642; font-weight: 800; letter-spacing: -0.2px; }
    .logo-text span { font-size: 9px; color: #93a1a1; font-weight: 700; letter-spacing: 0.5px; }

    .branch-nav-section { background: #fdf6e3; padding: 12px; border-radius: 16px; border: 1px solid #ddd6c1; flex-shrink: 0; }
    .section-micro-label { display: block; font-size: 8px; font-weight: 900; color: #93a1a1; margin-bottom: 6px; letter-spacing: 0.5px; text-transform: uppercase; }

    .branch-select {
        width: 100%; background: transparent; border: none;
        font-size: 13px; font-weight: 800; color: #268bd2;
        outline: none; cursor: pointer;
    }
    .branch-loading { font-size: 12px; color: #93a1a1; }
    .no-branches-btn { background: none; border: none; color: #dc322f; font-size: 11px; font-weight: 800; cursor: pointer; }

    .nav-menu { display: flex; flex-direction: column; gap: 6px; flex-shrink: 0; }
    .nav-btn {
        display: flex; align-items: center; gap: 12px; padding: 12px 16px;
        border: none; background: none; border-radius: 14px;
        color: #586e75; font-weight: 750;
        cursor: pointer; transition: all 0.2s;
    }
    .nav-btn:hover { background: rgba(253, 246, 227, 0.5); }
    .nav-btn.active {
        background: #fdf6e3;
        color: #268bd2;
        box-shadow: 0 4px 12px rgba(0,0,0,0.03);
    }

    .sidebar-calendar-section { border-top: 1px solid #ddd6c1; padding-top: 20px; }
    .cal-label { font-size: 10px; font-weight: 800; color: #93a1a1; margin-bottom: 12px; letter-spacing: 0.5px; }

    .sidebar-footer { margin-top: auto; padding-top: 16px; border-top: 1px solid #ddd6c1; flex-shrink: 0; }
    .logout-btn-desktop {
        display: flex; align-items: center; gap: 10px; width: 100%; padding: 12px 16px;
        border: 1px solid #eee8d5; background: #fdf6e3;
        color: #dc322f; border-radius: 12px; font-weight: 800; font-size: 13px; cursor: pointer;
        transition: 0.2s;
    }
    .logout-btn-desktop:hover { background: #dc322f; color: white; }

    :global(.sidebar .calendar-container) { padding: 0 !important; border: none !important; box-shadow: none !important; background: transparent !important; }
    :global(.sidebar .day-cell) { color: #586e75 !important; }
    :global(.sidebar .day-cell.selected) { background: #268bd2 !important; color: white !important; }
</style>
