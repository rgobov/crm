<script>
    import { activeTab, selectedDate, activeBranchId } from '$lib/stores/dashboardStore.js';
    import { branchStore } from '$lib/stores/branchStore.js';
    import { getNicheIcon } from '$lib/config/nicheConfig.js';
    import { openModal } from '$lib/stores/modalStore.js';
    import { logout } from '$lib/stores/auth.js';
    import { goto } from '$app/navigation';
    import { page } from '$app/stores';
    import { fade, slide, scale } from 'svelte/transition';
    import { createEventDispatcher } from 'svelte';
    import HorizontalDatePicker from '$lib/components/schedule/HorizontalDatePicker.svelte';
    import CalendarScreen from '$lib/components/calendar/CalendarScreen.svelte';
    import { swipeDown } from '$lib/actions/swipeDown.js';

    const dispatch = createEventDispatcher();

    $: branches = $branchStore;
    $: hasValidActiveBranch = Boolean($activeBranchId) && branches.some(b => String(b.id) === String($activeBranchId));
    $: branchSelectionRequired = branches.length > 0 && !hasValidActiveBranch;

    let showMoreMenu = false;
    let showCalendarModal = false;

    async function handleNav(tab) {
        showMoreMenu = false;
        showCalendarModal = false;
        activeTab.set(tab);
        try {
            if ($page?.url?.pathname !== '/admin') {
                await goto('/admin');
            }
        } catch (e) {
            // Fallback для WebView/Telegram где SvelteKit router может быть не готов
            window.location.href = '/admin';
        }
    }

    function handleDateSelected(event) {
        selectedDate.set(new Date(event.detail.date));
    }

    async function handleFullCalendarSelect(event) {
        selectedDate.set(new Date(event.detail.date));
        activeTab.set('timeline');
        showCalendarModal = false;

        // Если мы не на /admin, нужно туда вернуться, чтобы увидеть таймлайн
        if ($page?.url?.pathname !== '/admin') {
            try {
                await goto('/admin');
            } catch (e) {
                window.location.href = '/admin';
            }
        }
    }

    function toggleMoreMenu() {
        showMoreMenu = !showMoreMenu;
        showCalendarModal = false;
    }

    function closeMoreMenu() {
        showMoreMenu = false;
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

    {#if showMoreMenu}
        <div class="more-menu-backdrop" on:click|self={closeMoreMenu} transition:fade={{duration: 200}}>
            <div class="more-menu-sheet" transition:slide={{axis: 'y', duration: 250}}>
                <div class="sheet-drag-zone" use:swipeDown on:swipe={closeMoreMenu} aria-hidden="true">
                    <div class="sheet-handle"></div>
                </div>

                <section class="sheet-section">
                    <label>ВЫБОР ФИЛИАЛА</label>
                    <div class="branch-grid">
                        {#each branches as b}
                            <button
                                class="branch-card"
                                class:active={$activeBranchId === b.id}
                                on:click={() => selectBranch(b.id)}>
                                <span class="b-icon">{getNicheIcon(b.niche)}</span>
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
                        <button class="action-row" on:click={() => { goto('/admin/return-reminders'); showMoreMenu = false; }}>
                            <span class="a-icon">📩</span>
                            <div class="a-text">
                                <b>Возврат клиентов</b>
                                <p>Напомнить о визите</p>
                            </div>
                        </button>
                        <button class="action-row" on:click={() => { goto('/admin/settings/ai'); showMoreMenu = false; }}>
                            <span class="a-icon">🤖</span>
                            <div class="a-text">
                                <b>AI Настройки</b>
                                <p>Провайдер, ключи, база знаний</p>
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

    {#if branchSelectionRequired}
        <div class="branch-required-backdrop" role="presentation">
            <section class="branch-required-dialog" role="dialog" aria-modal="true" aria-labelledby="branch-required-title">
                <div class="branch-required-icon">🏢</div>
                <h2 id="branch-required-title">Выберите филиал</h2>
                <p>Для расписания и создания записи нужно выбрать филиал.</p>
                <div class="branch-required-list">
                    {#each branches as b}
                        <button class="branch-required-btn" on:click={() => selectBranch(b.id)} type="button">
                            <span class="required-branch-icon">{getNicheIcon(b.niche)}</span>
                            <span>{b.name}</span>
                        </button>
                    {/each}
                </div>
            </section>
        </div>
    {/if}
</div>

<style>
    :global(html, body) { position: fixed; width: 100%; height: 100%; overflow: hidden; }

    .mobile-shell { display: flex; flex-direction: column; height: 100dvh; width: 100vw; background: #fdf6e3; overflow: hidden; position: relative; }

    .mobile-content {
        flex: 1;
        overflow-y: auto;
        padding-top: env(safe-area-inset-top);
        padding-bottom: 160px;
        -webkit-overflow-scrolling: touch;
    }

    /*
     * ВАЖНО: backdrop-filter убран намеренно!
     *
     * backdrop-filter (и -webkit-backdrop-filter) создаёт новый CSS Stacking Context
     * на iOS WebKit. Это означает, что z-index дочерних элементов сравнивается только
     * внутри этого контекста. Модалка с z-index: 99999 внутри .mobile-content
     * (сестринского к .mobile-bottom-ui) никогда не перекрывала бы BottomNav на iOS,
     * даже при любых значениях z-index.
     *
     * Замена: непрозрачный фон #eee8d5 — визуально идентично, без побочных эффектов.
     */
    .mobile-bottom-ui { position: fixed; bottom: 0; left: 0; right: 0; background: #eee8d5; border-top: 1.5px solid #ddd6c1; z-index: 1000; padding-bottom: env(safe-area-inset-bottom); box-shadow: 0 -5px 25px rgba(0,0,0,0.05); }

    .bottom-date-picker { background: white; border-bottom: 1px solid #ddd6c1; }

    :global(.bottom-date-picker .date-picker-wrapper) { border-top: none !important; background: #eee8d5 !important; }
    :global(.bottom-date-picker .day-btn) { background: #fdf6e3 !important; border-color: #ddd6c1 !important; }
    :global(.bottom-date-picker .day-btn.is-selected) { background: #268bd2 !important; border-color: #268bd2 !important; }

    .bottom-nav { display: flex; align-items: center; height: 65px; padding: 0 10px; }
    .bottom-nav button { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; background: none; border: none; color: #93a1a1; gap: 2px; height: 100%; transition: color 0.2s; }
    .bottom-nav button.active { color: #268bd2; }
    .bottom-nav .icon { font-size: 22px; }
    .bottom-nav .label { font-size: 10px; font-weight: 800; text-transform: uppercase; }

    .mobile-full-modal { position: fixed; inset: 0; background: #fdf6e3; z-index: 4000; display: flex; flex-direction: column; }
    .modal-head { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; background: #eee8d5; border-bottom: 1.5px solid #ddd6c1; }
    .modal-head h3 { margin: 0; font-size: 18px; font-weight: 800; color: #073642; }
    .back-btn { background: none; border: none; font-size: 24px; color: #586e75; cursor: pointer; }
    .modal-scroll-body { flex: 1; overflow-y: auto; padding: 20px; }

    .more-menu-backdrop { position: fixed; inset: 0; background: rgba(7, 54, 66, 0.7); backdrop-filter: blur(8px); z-index: 3000; display: flex; align-items: flex-end; }
    .more-menu-sheet { width: 100%; max-height: calc(100dvh - 8px); box-sizing: border-box; overflow-y: auto; overscroll-behavior: contain; -webkit-overflow-scrolling: touch; background: #fdf6e3; border-radius: 32px 32px 0 0; padding: 24px; padding-bottom: calc(30px + env(safe-area-inset-bottom)); border-top: 2px solid #ddd6c1; }
    .sheet-drag-zone { height: 28px; margin: -12px 0 8px; display: flex; justify-content: center; align-items: flex-start; padding-top: 2px; box-sizing: border-box; user-select: none; }
    .sheet-handle { width: 40px; height: 4px; background: #ddd6c1; border-radius: 2px; }

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

    .branch-required-backdrop {
        position: fixed;
        inset: 0;
        z-index: 5000;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 20px;
        padding-bottom: calc(20px + env(safe-area-inset-bottom, 0px));
        box-sizing: border-box;
        background: rgba(7, 54, 66, 0.78);
    }

    .branch-required-dialog {
        width: 100%;
        max-width: 480px;
        max-height: calc(100dvh - 40px - env(safe-area-inset-bottom, 0px));
        overflow-y: auto;
        box-sizing: border-box;
        padding: 28px 20px 20px;
        border: 1.5px solid #ddd6c1;
        border-radius: 28px;
        background: #fdf6e3;
        text-align: center;
        box-shadow: 0 24px 60px rgba(0, 43, 54, 0.35);
    }

    .branch-required-icon { font-size: 32px; }
    .branch-required-dialog h2 { margin: 10px 0 8px; color: #073642; font-size: 20px; }
    .branch-required-dialog p { margin: 0 0 20px; color: #586e75; font-size: 13px; line-height: 1.4; }
    .branch-required-list { display: grid; gap: 10px; text-align: left; }
    .branch-required-btn {
        display: flex;
        align-items: center;
        gap: 12px;
        width: 100%;
        padding: 14px 16px;
        border: 1.5px solid #ddd6c1;
        border-radius: 16px;
        background: #eee8d5;
        color: #073642;
        font: inherit;
        font-weight: 800;
        text-align: left;
        cursor: pointer;
    }

    .branch-required-btn:active { border-color: #268bd2; background: white; }
    .required-branch-icon { font-size: 22px; }
</style>
