<script>
    import { page } from '$app/stores';
    import { user } from '$lib/stores/auth.js';

    // Автоматически определяем, какой пункт меню активен по URL
    $: activePath = $page.url.pathname;
</script>

<nav class="bottom-nav">
    <a href="/{$user?.role?.toLowerCase() || 'employee'}" class:active={activePath.endsWith('admin') || activePath.endsWith('manager') || activePath.endsWith('employee')}>
        <span class="icon">📊</span>
        <span class="label">Главная</span>
    </a>

    <a href="/calendar" class:active={activePath.includes('/calendar')}>
        <span class="icon">📅</span>
        <span class="label">Календарь</span>
    </a>

    <a href="/profile" class:active={activePath.includes('/profile')}>
        <span class="icon">👤</span>
        <span class="label">Профиль</span>
    </a>
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

    :global(body.tg) .bottom-nav {
        background: var(--tg-theme-secondary-bg-color);
        border-top: 0.5px solid var(--tg-theme-hint-color);
    }

    a {
        flex: 1;
        text-decoration: none;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 4px;
        color: #94a3b8;
        transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
    }

    a.active {
        color: var(--primary-color);
        transform: translateY(-2px);
    }

    .icon {
        font-size: 22px;
    }

    .label {
        font-size: 11px;
        font-weight: 700;
        text-transform: uppercase;
        letter-spacing: 0.5px;
    }
</style>
