<script>
    import { onMount } from 'svelte';
    import { adminService } from '$lib/services/adminService.js';
    import { goto } from '$app/navigation';
    import { fade, scale } from 'svelte/transition';

    // Props от родителя (могут использоваться в будущем)
    export let forcedDate = null;
    export let branchId = null;

    let stats = {
        totalClients: 0,
        todayAppointments: 0,
        totalResources: 0,
        totalStaff: 0
    };
    let isLoading = true;

    onMount(async () => {
        try {
            const data = await adminService.getDashboardStats();
            stats = data;
        } finally {
            isLoading = false;
        }
    });

    const gridItems = [
        { id: 'branches', title: 'Филиалы', icon: '🏢', link: '/admin/branches' },
        { id: 'staff', title: 'Персонал', icon: '👤', link: '/admin/staff' },
        { id: 'resources', title: 'Ресурсы', icon: '⚒️', link: '/admin/resources' },
        { id: 'services', title: 'Услуги', icon: '✂️', link: '/admin/services' }
    ];

    function formatStat(num) {
        if (!num) return '0';
        if (num >= 1000) return (num/1000).toFixed(1) + 'k';
        return num;
    }
</script>

<div class="stories-dashboard">

    <!-- СЕКЦИЯ СТОРИС: Финальный дизайн -->
    <div class="stories-container">
        <div class="stories-track">

            <!-- СТОРИС: СЕГОДНЯ -->
            <div class="story-item">
                <div class="story-circle ring-blue">
                    <div class="story-inner">
                        <span class="s-icon">📅</span>
                        <span class="s-val">{formatStat(stats.todayAppointments)}</span>
                    </div>
                </div>
                <span class="story-label">Сегодня</span>
            </div>

            <!-- СТОРИС: КЛИЕНТЫ -->
            <div class="story-item">
                <div class="story-circle ring-magenta">
                    <div class="story-inner">
                        <span class="s-icon">💎</span>
                        <span class="s-val">{formatStat(stats.totalClients)}</span>
                    </div>
                </div>
                <span class="story-label">Клиенты</span>
            </div>

            <!-- СТОРИС: МАСТЕРА -->
            <div class="story-item">
                <div class="story-circle ring-green">
                    <div class="story-inner">
                        <span class="s-icon">👤</span>
                        <span class="s-val">{formatStat(stats.totalStaff)}</span>
                    </div>
                </div>
                <span class="story-label">Мастера</span>
            </div>

            <!-- СТОРИС: РЕСУРСЫ -->
            <div class="story-item">
                <div class="story-circle ring-orange">
                    <div class="story-inner">
                        <span class="s-icon">⚒️</span>
                        <span class="s-val">{formatStat(stats.totalResources)}</span>
                    </div>
                </div>
                <span class="story-label">Ресурсы</span>
            </div>

        </div>
    </div>

    <!-- СЕКЦИЯ УПРАВЛЕНИЯ -->
    <section class="main-controls">
        <label class="section-caption">ИНСТРУМЕНТЫ</label>

        <div class="phi-grid">
            {#each gridItems as item}
                <button class="phi-card" on:click={() => goto(item.link)} in:scale={{duration: 200, start: 0.95}}>
                    <div class="card-icon">{item.icon}</div>
                    <h3>{item.title}</h3>
                </button>
            {/each}
        </div>

        <button class="phi-wide-card" on:click={() => goto('/admin/clients')} in:fade={{delay: 300}}>
            <div class="wide-inner">
                <div class="card-icon accent-bg">💎</div>
                <div class="wide-text">
                    <h3>База Клиентов</h3>
                    <p>Профили, история и лояльность</p>
                </div>
            </div>
            <span class="phi-arrow">→</span>
        </button>
    </section>

    <div class="bottom-phi-spacer"></div>
</div>

<style>
    .stories-dashboard {
        height: 100%; width: 100%;
        background: #fdf6e3;
        overflow-y: auto;
        display: flex; flex-direction: column;
        box-sizing: border-box;
    }

    .stories-container {
        padding: 24px 0 20px 0;
        background: linear-gradient(to bottom, #eee8d5 0%, #fdf6e3 100%);
        margin-bottom: 16px;
    }
    .stories-track {
        display: flex; gap: 20px;
        padding: 0 20px;
        overflow-x: auto;
        scrollbar-width: none;
    }
    .stories-track::-webkit-scrollbar { display: none; }

    .story-item { display: flex; flex-direction: column; align-items: center; gap: 10px; flex-shrink: 0; }

    .story-circle {
        width: 72px; height: 72px;
        background: #eee8d5;
        border-radius: 50%;
        padding: 3px; /* Место под обводку */
        position: relative;
    }

    /* ЦВЕТНЫЕ ОБВОДКИ (Solarized Palette) */
    .ring-blue { background: linear-gradient(45deg, #268bd2, #2aa198); }
    .ring-magenta { background: linear-gradient(45deg, #d33682, #6c71c4); }
    .ring-green { background: linear-gradient(45deg, #859900, #2aa198); }
    .ring-orange { background: linear-gradient(45deg, #cb4b16, #b58900); }

    .story-inner {
        width: 100%; height: 100%;
        background: #fdf6e3;
        border-radius: 50%;
        display: flex; flex-direction: column;
        align-items: center; justify-content: center;
        gap: 2px;
    }

    .s-icon { font-size: 16px; }
    .s-val { font-size: 18px; font-weight: 900; color: #073642; line-height: 1; }
    .story-label { font-size: 10px; font-weight: 800; color: #586e75; text-transform: uppercase; letter-spacing: 0.5px; }

    /* GRID & CONTROLS */
    .main-controls { padding: 0 20px; flex: 1; }
    .section-caption { display: block; font-size: 11px; font-weight: 950; color: #93a1a1; letter-spacing: 2px; margin-bottom: 16px; text-transform: uppercase; padding-left: 4px; }

    .phi-grid {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 12px;
        margin-bottom: 12px;
    }

    .phi-card {
        background: #eee8d5;
        border: 1.5px solid #ddd6c1;
        border-radius: 24px;
        padding: 24px 20px;
        display: flex; flex-direction: column; align-items: flex-start; gap: 14px;
        aspect-ratio: 1 / 0.8;
        cursor: pointer;
        transition: transform 0.2s;
    }
    .phi-card:active { transform: scale(0.96); background: white; border-color: #268bd2; }
    .card-icon { font-size: 26px; }
    .phi-card h3 { margin: 0; font-size: 16px; font-weight: 850; color: #073642; }

    .phi-wide-card {
        width: 100%;
        background: #eee8d5;
        border: 1.5px solid #ddd6c1;
        border-radius: 30px;
        padding: 24px;
        display: flex; align-items: center; justify-content: space-between;
        cursor: pointer;
        transition: all 0.2s;
        margin-top: 4px;
    }
    .phi-wide-card:active { transform: scale(0.98); background: white; }
    .wide-inner { display: flex; align-items: center; gap: 20px; }
    .accent-bg { background: #fdf6e3; width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; border: 1px solid #ddd6c1; }
    .wide-text { text-align: left; }
    .wide-text h3 { margin: 0; font-size: 18px; font-weight: 850; color: #073642; }
    .wide-text p { margin: 2px 0 0 0; font-size: 12px; color: #586e75; font-weight: 600; }
    .phi-arrow { font-size: 22px; color: #268bd2; font-weight: 300; }

    .bottom-phi-spacer { height: 120px; }
</style>
