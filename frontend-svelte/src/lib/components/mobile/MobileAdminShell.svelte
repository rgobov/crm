<script>
    import { activeTab, selectedDate, activeBranchId } from '$lib/stores/dashboardStore.js';
    import { openModal } from '$lib/stores/modalStore.js';
    import { logout } from '$lib/stores/auth.js';
    import { goto } from '$app/navigation';
    import { page } from '$app/stores';
    import { fade, slide, scale } from 'svelte/transition';
    import { createEventDispatcher } from 'svelte';
    import HorizontalDatePicker from '$lib/components/schedule/HorizontalDatePicker.svelte';
    import CalendarScreen from '$lib/components/calendar/CalendarScreen.svelte'; // ИМПОРТ

    const dispatch = createEventDispatcher();
    export let branches = [];

    let showMoreMenu = false;
    let showCalendarModal = false; // Состояние для мобильного календаря

    async function handleNav(tab) {
        showMoreMenu = false;
        showCalendarModal = false;
        activeTab.set(tab);
        if ($page.url.pathname !== '/admin') {
            await goto('/admin');
        }
    }

    function handleDateSelected(event) {
        selectedDate.set(new Date(event.detail.date));
        // При выборе даты из ленты ничего не закрываем, просто скроллим
    }

    function handleFullCalendarSelect(event) {
        selectedDate.set(new Date(event.detail.date));
        activeTab.set('timeline');
        showCalendarModal = false;
    }

    function toggleMoreMenu() {
        showMoreMenu = !showMoreMenu;
        showCalendarModal = false;
    }

    function openCalendar() {
        showCalendarModal = true;
        showMoreMenu = false;
    }

    function handleLogout() {
        logout();
        goto('/');
    }

    function selectBranch(id) {
        activeBranchId.set(id);
        showMoreMenu = false;
    }
</script>

<div class="mobile-shell">
    <header class="mobile-header">
        <div class="brand">999 CRM</div>
        <div class="current-branch-tag">
            {branches.find(b => b.id === $activeBranchId)?.name || 'Филиал'}
        </div>
    </header>

    <main class="mobile-content">
        <slot />
    </main>

    <div class="mobile-bottom-ui">
        {#if $activeTab === 'timeline'}
            <div class="bottom-date-picker" transition:slide={{duration: 200}}>
                <HorizontalDatePicker selectedDate={$selectedDate} on:dateSelected={handleDateSelected} />
            </div>
        {/if}

        <nav class="bottom-nav">
            <button class:active={$activeTab === 'management'} on:click={() => handleNav('management')}>
                <span class="icon">📊</span>
                <span class="label">Главная</span>
            </button>

            <!-- НОВАЯ КНОПКА КАЛЕНДАРЬ -->
            <button class:active={showCalendarModal} on:click={openCalendar}>
                <span class="icon">🗓</span>
                <span class="label">Календарь</span>
            </button>

            <button class:active={$activeTab === 'timeline'} on:click={() => handleNav('timeline')}>
                <span class="icon">🕒</span>
                <span class="label">Таймлайн</span>
            </button>

            <button class:active={showMoreMenu} on:click={toggleMoreMenu}>
                <span class="icon">{showMoreMenu ? '✕' : '⚙️'}</span>
                <span class="label">Ещё</span>
            </button>
        </nav>
    </div>

    <!-- ПОЛНОЭКРАННЫЙ МОБИЛЬНЫЙ КАЛЕНДАРЬ -->
    {#if showCalendarModal}
        <div class="mobile-full-modal" transition:fade={{duration: 200}}>
            <header class="modal-head">
                <button class="back-btn" on:click={() => showCalendarModal = false}>✕</button>
                <h3>Выбор даты</h3>
                <div style="width: 40px"></div>
            </header>
            <div class="modal-scroll-body">
                <CalendarScreen on:dateSelected={handleFullCalendarSelect} />
            </div>
        </div>
    {/if}

    <!-- ШТОРКА МЕНЮ "ЕЩЁ" -->
    {#if showMoreMenu}
        <div class="more-menu-backdrop" on:click|self={toggleMoreMenu} transition:fade={{duration: 200}}>
            <div class="more-menu-sheet" in:slide={{axis: 'y', duration: 300}}>
                <div class="sheet-handle"></div>

                <section class="sheet-section">
                    <label>ВЫБОР ФИЛИАЛА</label>
                    <div class="branch-grid">
                        {#each branches as b}
                            <button
                                class="branch-card"
                                class:active={$activeBranchId === b.id}
                                on:click={() => selectBranch(b.id)}>
                                <span class="b-icon">🏢</span>
                                <span class="b-name">{b.name}</span>
                                {#if $activeBranchId === b.id}
                                    <span class="active-dot">✓</span>
                                {/if}
                            </button>
                        {/each}
                    </div>
                </section>

                <section class="sheet-section">
                    <label>БЫСТРЫЕ НАСТРОЙКИ</label>
                    <div class="action-list">
                        <button class="action-row" on:click={() => { openModal('telegram'); showMoreMenu = false; }}>
                            <span class="a-icon">📱</span>
                            <div class="a-text">
                                <b>Telegram Уведомления</b>
                                <p>QR-код и статус подключения</p>
                            </div>
                        </button>
                        <button class="action-row" on:click={() => { openModal('templates'); showMoreMenu = false; }}>
                            <span class="a-icon">✉️</span>
                            <div class="a-text">
                                <b>Шаблоны сообщений</b>
                                <p>Настройка текстов напоминаний</p>
                            </div>
                        </button>
                    </div>
                </section>

                <div class="sheet-footer">
                    <button class="btn-logout-full" on:click={handleLogout}>
                        ВЫЙТИ ИЗ АККАУНТА 🚪
                    </button>
                </div>
            </div>
        </div>
    {/if}
</div>

<style>
    :global(html, body) { position: fixed; width: 100%; height: 100%; overflow: hidden; }

    .mobile-shell { display: flex; flex-direction: column; height: 100dvh; width: 100vw; background: #fdf6e3; overflow: hidden; position: relative; }

    .mobile-header { flex-shrink: 0; display: flex; justify-content: space-between; align-items: center; padding: 12px 20px; background: #eee8d5; border-bottom: 1.5px solid #ddd6c1; z-index: 100; }
    .brand { font-weight: 900; color: #268bd2; font-size: 14px; letter-spacing: 1px; }
    .current-branch-tag { font-size: 11px; font-weight: 800; color: #586e75; text-transform: uppercase; background: #fdf6e3; padding: 4px 10px; border-radius: 8px; border: 1px solid #ddd6c1; }

    .mobile-content { flex: 1; overflow-y: auto; padding-bottom: 160px; -webkit-overflow-scrolling: touch; }

    .mobile-bottom-ui { position: fixed; bottom: 0; left: 0; right: 0; background: #eee8d5; border-top: 1.5px solid #ddd6c1; z-index: 2000; padding-bottom: env(safe-area-inset-bottom); box-shadow: 0 -5px 25px rgba(0,0,0,0.05); }

    .bottom-date-picker { background: white; border-bottom: 1px solid #ddd6c1; }
    :global(.bottom-date-picker .date-picker-wrapper) { border-top: none !important; background: #eee8d5 !important; }
    :global(.bottom-date-picker .day-btn) { background: #fdf6e3 !important; border-color: #ddd6c1 !important; height: 60px !important; min-width: 50px !important; }
    :global(.bottom-date-picker .day-btn.is-selected) { background: #268bd2 !important; border-color: #268bd2 !important; }

    .bottom-nav { display: flex; align-items: center; height: 65px; padding: 0 10px; }
    .bottom-nav button { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; background: none; border: none; color: #93a1a1; gap: 2px; height: 100%; transition: color 0.2s; }
    .bottom-nav button.active { color: #268bd2; }
    .bottom-nav .icon { font-size: 22px; }
    .bottom-nav .label { font-size: 9px; font-weight: 800; text-transform: uppercase; }

    /* МОБИЛЬНОЕ МОДАЛЬНОЕ ОКНО КАЛЕНДАРЯ */
    .mobile-full-modal { position: fixed; inset: 0; background: #fdf6e3; z-index: 4000; display: flex; flex-direction: column; }
    .modal-head { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; background: #eee8d5; border-bottom: 1.5px solid #ddd6c1; }
    .modal-head h3 { margin: 0; font-size: 18px; font-weight: 800; color: #073642; }
    .back-btn { background: none; border: none; font-size: 24px; color: #586e75; cursor: pointer; }
    .modal-scroll-body { flex: 1; overflow-y: auto; padding: 20px; }

    /* ШТОРКА */
    .more-menu-backdrop { position: fixed; inset: 0; background: rgba(7, 54, 66, 0.7); backdrop-filter: blur(8px); z-index: 3000; display: flex; align-items: flex-end; }
    .more-menu-sheet { width: 100%; background: #fdf6e3; border-radius: 32px 32px 0 0; padding: 24px; padding-bottom: calc(30px + env(safe-area-inset-bottom)); border-top: 2px solid #ddd6c1; }
    .sheet-handle { width: 40px; height: 4px; background: #ddd6c1; border-radius: 2px; margin: -10px auto 24px auto; }

    .sheet-section { margin-bottom: 24px; }
    .sheet-section label { display: block; font-size: 10px; font-weight: 900; color: #93a1a1; letter-spacing: 1px; margin-bottom: 12px; }

    .branch-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
    .branch-card { background: #eee8d5; border: 1.5px solid #ddd6c1; border-radius: 16px; padding: 12px; display: flex; flex-direction: column; align-items: flex-start; gap: 4px; position: relative; width: 100%; }
    .branch-card.active { border-color: #268bd2; background: white; }
    .b-name { font-size: 14px; font-weight: 800; color: #073642; }
    .active-dot { position: absolute; top: 10px; right: 12px; color: #268bd2; font-weight: 900; }

    .action-list { display: flex; flex-direction: column; gap: 10px; }
    .action-row { background: #eee8d5; border: 1px solid #ddd6c1; border-radius: 16px; padding: 14px; display: flex; align-items: center; gap: 16px; text-align: left; width: 100%; }
    .a-icon { font-size: 24px; }
    .a-text b { display: block; font-size: 15px; color: #073642; }
    .a-text p { margin: 0; font-size: 11px; color: #586e75; font-weight: 600; }

    .sheet-footer { margin-top: 10px; }
    .btn-logout-full { width: 100%; background: #dc322f; color: white; border: none; padding: 16px; border-radius: 16px; font-weight: 900; font-size: 14px; cursor: pointer; }
</style>
