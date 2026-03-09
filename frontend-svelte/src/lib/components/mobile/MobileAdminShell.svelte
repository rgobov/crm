<script>
    import { activeTab, selectedDate, activeBranchId } from '$lib/stores/dashboardStore.js';
    import { openModal } from '$lib/stores/modalStore.js';
    import { goto } from '$app/navigation';
    import { fade, slide } from 'svelte/transition';
    import { createEventDispatcher } from 'svelte';
    import HorizontalDatePicker from '$lib/components/schedule/HorizontalDatePicker.svelte';

    const dispatch = createEventDispatcher();
    export let branches = [];

    function handleNav(tab) {
        activeTab.set(tab);
    }

    function handleDateSelected(event) {
        selectedDate.set(new Date(event.detail.date));
    }
</script>

<div class="mobile-shell">
    <!-- ВЕРХНЯЯ ПАНЕЛЬ -->
    <header class="mobile-header">
        <div class="branch-pill">
            <select bind:value={$activeBranchId}>
                {#each branches as b}
                    <option value={b.id}>{b.name}</option>
                {/each}
            </select>
            <span class="chevron">▼</span>
        </div>
        <div class="header-right">
            <button class="logout-mini" on:click={() => goto('/')}>🚪</button>
        </div>
    </header>

    <!-- ОСНОВНОЙ КОНТЕНТ -->
    <main class="mobile-content">
        <slot />
    </main>

    <!-- НИЖНЯЯ ПАНЕЛЬ УПРАВЛЕНИЯ -->
    <div class="mobile-bottom-ui">

        <!-- ФИКС: Лента календаря видна ТОЛЬКО на вкладке Таймлайн -->
        {#if $activeTab === 'timeline'}
            <div class="bottom-date-picker" transition:slide={{duration: 200}}>
                <HorizontalDatePicker selectedDate={$selectedDate} on:dateSelected={handleDateSelected} />
            </div>
        {/if}

        <!-- МЕНЮ -->
        <nav class="bottom-nav">
            <button class:active={$activeTab === 'management'} on:click={() => handleNav('management')}>
                <span class="icon">📊</span>
                <span class="label">Главная</span>
            </button>

            <button class:active={$activeTab === 'timeline'} on:click={() => handleNav('timeline')}>
                <span class="icon">🕒</span>
                <span class="label">Таймлайн</span>
            </button>

            <div class="fab-wrapper">
                <button class="fab-btn" on:click={() => dispatch('add')}>+</button>
            </div>

            <button on:click={() => openModal('telegram')}>
                <span class="icon">📱</span>
                <span class="label">Telegram</span>
            </button>

            <button on:click={() => openModal('templates')}>
                <span class="icon">✉️</span>
                <span class="label">Шаблоны</span>
            </button>
        </nav>
    </div>
</div>

<style>
    :global(html, body) { position: fixed; width: 100%; height: 100%; overflow: hidden; }

    .mobile-shell { display: flex; flex-direction: column; height: 100dvh; width: 100vw; background: #fdf6e3; overflow: hidden; }

    .mobile-header { flex-shrink: 0; display: flex; justify-content: space-between; align-items: center; padding: 10px 16px; background: #eee8d5; border-bottom: 1.5px solid #ddd6c1; z-index: 100; }
    .branch-pill { position: relative; background: #fdf6e3; padding: 6px 12px; border-radius: 12px; border: 1px solid #ddd6c1; }
    .branch-pill select { border: none; background: none; font-size: 13px; font-weight: 800; color: #073642; appearance: none; padding-right: 15px; outline: none; }
    .branch-pill .chevron { position: absolute; right: 8px; top: 50%; transform: translateY(-50%); font-size: 8px; color: #93a1a1; }
    .logout-mini { background: none; border: none; font-size: 18px; cursor: pointer; }

    .mobile-content { flex: 1; overflow-y: auto; padding-bottom: 160px; -webkit-overflow-scrolling: touch; }

    .mobile-bottom-ui { position: fixed; bottom: 0; left: 0; right: 0; background: #eee8d5; border-top: 1.5px solid #ddd6c1; z-index: 2000; padding-bottom: env(safe-area-inset-bottom); box-shadow: 0 -5px 25px rgba(0,0,0,0.05); }

    .bottom-date-picker { background: white; border-bottom: 1px solid #ddd6c1; }
    :global(.bottom-date-picker .date-picker-wrapper) { border-top: none !important; background: #eee8d5 !important; }
    :global(.bottom-date-picker .day-btn) { background: #fdf6e3 !important; border-color: #ddd6c1 !important; height: 60px !important; min-width: 50px !important; }
    :global(.bottom-date-picker .day-btn.is-selected) { background: #268bd2 !important; border-color: #268bd2 !important; }

    .bottom-nav { display: flex; align-items: center; height: 65px; }
    .bottom-nav button { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; background: none; border: none; color: #93a1a1; gap: 2px; height: 100%; }
    .bottom-nav button.active { color: #268bd2; }
    .bottom-nav .icon { font-size: 20px; }
    .bottom-nav .label { font-size: 9px; font-weight: 800; text-transform: uppercase; }

    .fab-wrapper { position: relative; width: 60px; height: 100%; }
    .fab-btn { position: absolute; top: -35px; left: 50%; transform: translateX(-50%); width: 56px !important; height: 56px !important; border-radius: 50% !important; background: #268bd2 !important; color: white !important; font-size: 32px !important; font-weight: 300 !important; box-shadow: 0 8px 20px rgba(38, 139, 210, 0.4) !important; border: 3px solid #eee8d5 !important; display: flex !important; align-items: center !important; justify-content: center !important; }
</style>
