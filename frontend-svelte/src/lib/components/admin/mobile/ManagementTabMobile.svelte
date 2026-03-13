<script>
    import { onMount } from 'svelte';
    import { adminService } from '$lib/services/adminService.js';
    import { goto } from '$app/navigation';
    import { fade } from 'svelte/transition';

    let stats = { totalClients: 0, todayAppointments: 0, totalResources: 0, totalStaff: 0 };
    let isLoading = true;

    onMount(async () => {
        try { stats = await adminService.getDashboardStats(); }
        finally { isLoading = false; }
    });

    const menuCards = [
        { id: 'branches', title: 'Филиалы', desc: 'Управление точками', icon: '🏢', link: '/admin/branches' },
        { id: 'staff', title: 'Персонал', desc: 'Мастера и графики', icon: '👤', link: '/admin/staff' },
        { id: 'resources', title: 'Ресурсы', desc: 'Залы и оборудование', icon: '⚒️', link: '/admin/resources' },
        { id: 'services', title: 'Услуги', desc: 'Ваш прайс-лист', icon: '✂️', link: '/admin/services' },
        { id: 'clients', title: 'Клиенты', desc: 'База клиентов', icon: '💎', link: '/admin/clients' }
    ];
</script>

<div class="management-container-mobile">
    <!-- СТАТИСТИКА: Четкие, понятные карточки -->
    <div class="stats-grid">
        <div class="stat-card">
            <span class="val">{stats.totalClients}</span>
            <span class="lbl">Клиенты</span>
        </div>
        <div class="stat-card">
            <span class="val">{stats.todayAppointments}</span>
            <span class="lbl">Сегодня</span>
        </div>
        <div class="stat-card">
            <span class="val">{stats.totalStaff}</span>
            <span class="lbl">Мастера</span>
        </div>
    </div>

    <label class="section-label">СПРАВОЧНИКИ</label>

    <nav class="menu-list">
        {#each menuCards as card}
            <button class="menu-item" on:click={() => goto(card.link)}>
                <div class="item-icon">{card.icon}</div>
                <div class="item-text">
                    <h3>{card.title}</h3>
                    <p>{card.desc}</p>
                </div>
                <span class="arrow">›</span>
            </button>
        {/each}
    </nav>

    <div class="bottom-spacer"></div>
</div>

<style>
    .management-container-mobile {
        padding: 20px 16px;
        background: #fdf6e3;
        height: 100%;
        overflow-y: auto;
        box-sizing: border-box;
    }

    .stats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin-bottom: 24px; }
    .stat-card {
        background: #eee8d5;
        padding: 16px 8px; border-radius: 18px;
        border: 1.5px solid #ddd6c1;
        text-align: center;
    }
    .stat-card .val { display: block; font-size: 22px; font-weight: 900; color: #268bd2; margin-bottom: 2px; }
    .stat-card .lbl { font-size: 8px; font-weight: 850; color: #93a1a1; text-transform: uppercase; letter-spacing: 0.5px; }

    .section-label { display: block; font-size: 10px; font-weight: 900; color: #93a1a1; margin-bottom: 12px; letter-spacing: 1.5px; text-transform: uppercase; padding-left: 4px; }

    .menu-list { display: flex; flex-direction: column; gap: 8px; }
    .menu-item {
        display: flex; align-items: center; padding: 16px;
        background: #eee8d5; border: 1.5px solid #ddd6c1; border-radius: 20px;
        cursor: pointer; text-align: left;
    }
    .menu-item:active { transform: scale(0.98); background: #fdf6e3; }

    .item-icon { width: 48px; height: 48px; background: #fdf6e3; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 22px; margin-right: 16px; border: 1px solid #ddd6c1; flex-shrink: 0; }
    .item-text h3 { margin: 0; font-size: 16px; color: #073642; font-weight: 850; }
    .item-text p { margin: 1px 0 0 0; font-size: 11px; color: #586e75; font-weight: 600; }
    .arrow { font-size: 22px; color: #93a1a1; margin-left: auto; }

    .bottom-spacer { height: 100px; }
</style>
