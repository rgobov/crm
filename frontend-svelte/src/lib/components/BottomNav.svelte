<script>
    import { activeTab } from '$lib/stores/dashboardStore.js';
    import { page } from '$app/stores';
    import { goto } from '$app/navigation';

    // Проверяем, находимся ли мы на главном экране роли
    $: isOnMainDashboard = $page.url.pathname.match(/^\/(admin|manager|employee)$/);

    async function handleNav(tab) {
        if (isOnMainDashboard) {
            activeTab.set(tab);
        } else {
            // Бесшовный переход на главную страницу админа
            await goto('/admin');
            activeTab.set(tab);
        }
    }
</script>

<nav class="bottom-nav">
    <button
        class:active={$activeTab === 'management'}
        on:click={() => handleNav('management')}
    >
        <span class="icon">📊</span>
        <span class="label">Управление</span>
    </button>

    <button
        class:active={$activeTab === 'calendar'}
        on:click={() => handleNav('calendar')}
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
        /*
         * backdrop-filter убран намеренно: создаёт Stacking Context на iOS WebKit,
         * из-за которого position:fixed модалки с любым z-index не может
         * перекрыть этот элемент. Заменён на непрозрачный белый фон.
         */
        background: #ffffff;
        display: flex;
        justify-content: space-around;
        align-items: center;
        border-top: 1px solid #f1f5f9;
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
        transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
    }

    button.active {
        color: var(--primary-color);
        transform: translateY(-2px);
    }

    .icon { font-size: 22px; }
    .label { font-size: 11px; font-weight: 700; letter-spacing: 0.2px; }
</style>
