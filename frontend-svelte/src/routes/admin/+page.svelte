<script>
    import { user, logout } from '$lib/stores/auth.js';
    import { goto } from '$app/navigation';

    // Список разделов управления (как в AdminManagementTab)
    const menuItems = [
        { id: 'staff', title: 'Сотрудники', icon: '👥', color: '#3897f0', desc: 'Управление командой' },
        { id: 'clients', title: 'Клиенты', icon: '💎', color: '#10b981', desc: 'База ваших клиентов' },
        { id: 'services', title: 'Услуги', icon: '🛠️', color: '#f59e0b', desc: 'Настройка прайс-листа' },
        { id: 'stats', title: 'Статистика', icon: '📈', color: '#8b5cf6', desc: 'Аналитика записей' }
    ];

    function handleLogout() {
        logout();
        goto('/');
    }
</script>

<div class="page">
    <div class="header">
        <div class="user-info">
            <span class="avatar">{$user?.name?.charAt(0) || 'A'}</span>
            <div class="text">
                <h2>{$user?.name || 'Администратор'}</h2>
                <p>Панель управления 999</p>
            </div>
        </div>
        <button class="logout-mini" on:click={handleLogout}>Выйти</button>
    </div>

    <div class="grid">
        {#each menuItems as item}
            <button class="card menu-card" on:click={() => goto(`/admin/${item.id}`)}>
                <div class="icon-box" style="background-color: {item.color}15; color: {item.color}">
                    {item.icon}
                </div>
                <div class="card-content">
                    <h3>{item.title}</h3>
                    <p>{item.desc}</p>
                </div>
                <span class="arrow">→</span>
            </button>
        {/each}
    </div>
</div>

<style>
    .page {
        padding: 20px;
        background-color: var(--bg-color);
    }

    .header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 32px;
        margin-top: 10px;
    }

    .user-info {
        display: flex;
        align-items: center;
        gap: 16px;
    }

    .avatar {
        width: 50px;
        height: 50px;
        background: var(--primary-gradient);
        color: white;
        border-radius: 16px;
        display: flex;
        justify-content: center;
        align-items: center;
        font-size: 20px;
        font-weight: 800;
        box-shadow: 0 8px 16px rgba(56, 151, 240, 0.2);
    }

    h2 {
        font-size: 18px;
        font-weight: 800;
        margin: 0;
        color: #0f172a;
    }

    .text p {
        margin: 4px 0 0 0;
        font-size: 13px;
        color: var(--hint-color);
    }

    .logout-mini {
        background: white;
        border: 1.5px solid #f1f5f9;
        padding: 8px 16px;
        border-radius: 12px;
        font-size: 13px;
        font-weight: 600;
        color: #ef4444;
        cursor: pointer;
    }

    .grid {
        display: grid;
        gap: 16px;
    }

    .menu-card {
        border: none;
        width: 100%;
        display: flex;
        align-items: center;
        gap: 20px;
        padding: 20px;
        text-align: left;
        cursor: pointer;
        transition: transform 0.2s, box-shadow 0.2s;
    }

    .menu-card:active {
        transform: scale(0.98);
    }

    .icon-box {
        width: 56px;
        height: 56px;
        border-radius: 18px;
        display: flex;
        justify-content: center;
        align-items: center;
        font-size: 24px;
    }

    .card-content {
        flex: 1;
    }

    .card-content h3 {
        margin: 0;
        font-size: 16px;
        font-weight: 700;
        color: #1e293b;
    }

    .card-content p {
        margin: 4px 0 0 0;
        font-size: 13px;
        color: var(--hint-color);
    }

    .arrow {
        color: #cbd5e1;
        font-size: 20px;
        font-weight: 300;
    }
</style>
