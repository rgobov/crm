<script>
    import { activeTab } from '$lib/stores/dashboardStore.js';
    import { page } from '$app/stores';

    // Меню работает как переключатель вкладок, если мы на главной странице роли
    // Или как обычные ссылки, если мы ушли глубоко в настройки
    $: isOnMainDashboard = $page.url.pathname.match(/^\/(admin|manager|employee)$/);
</script>

<nav class="bottom-nav">
    <button
        class:active={$activeTab === 'management'}
        on:click={() => isOnMainDashboard ? activeTab.set('management') : window.location.href='/admin'}
    >
        <span class="icon">📊</span>
        <span class="label">Управление</span>
    </button>

    <button
        class:active={$activeTab === 'calendar'}
        on:click={() => isOnMainDashboard ? activeTab.set('calendar') : window.location.href='/admin'}
    >
        <span class="icon">📅</span>
        <span class="label">Календарь</span>
    </button>
</nav>

<style>
    .bottom-nav {
        position: fixed;
        bottom: 0;
        left: 0;
        right: 0;
        height: 65px;
        background: rgba(255, 255, 255, 0.95);
        backdrop-filter: blur(10px);
        display: flex;
        justify-content: space-around;
        align-items: center;
        border-top: 1px solid rgba(0, 0, 0, 0.05);
        padding-bottom: env(safe-area-inset-bottom);
        z-index: 1000;
    }

    button {
        flex: 1;
        background: none;
        border: none;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 4px;
        color: #94a3b8;
        cursor: pointer;
        transition: all 0.2s ease;
    }

    button.active {
        color: var(--primary-color);
    }

    .icon { font-size: 22px; }
    .label { font-size: 11px; font-weight: 700; letter-spacing: 0.2px; }
</style>
