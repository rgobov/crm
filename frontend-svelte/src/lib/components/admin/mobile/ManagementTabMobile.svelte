<script>
    import { onMount } from 'svelte';
    import { adminService } from '$lib/services/adminService.js';
    import { goto } from '$app/navigation';
    import { fade, scale } from 'svelte/transition';

    let stats = { totalClients: 0, todayAppointments: 0, totalResources: 0, totalStaff: 0 };
    let isLoading = true;

    onMount(async () => {
        try { stats = await adminService.getDashboardStats(); }
        finally { isLoading = false; }
    });

    const gridItems = [
        { id: 'branches', title: 'Филиалы', icon: '🏢', link: '/admin/branches' },
        { id: 'staff', title: 'Персонал', icon: '👤', link: '/admin/staff' },
        { id: 'resources', title: 'Ресурсы', icon: '⚒️', link: '/admin/resources' },
        { id: 'services', title: 'Услуги', icon: '✂️', link: '/admin/services' }
    ];

    // Форматирование больших чисел для сторис
    function formatStat(num) {
        if (num >= 1000) return (num/1000).toFixed(1) + 'k';
        return num;
    }
</script>

<div class="stories-dashboard">

    <!-- СЕКЦИЯ СТОРИС (СТАТИСТИКА) -->
    <div class="stories-container">
        <div class="stories-track">
            <div class="story-item">
                <div class="story-circle highlight">
                    <span class="story-val">{formatStat(stats.todayAppointments)}</span>
                </div>
                <span class="story-label">Визиты</span>
            </div>

            <div class="story-item">
                <div class="story-circle">
                    <span class="story-val">{formatStat(stats.totalClients)}</span>
                </div>
                <span class="story-label">Клиенты</span>
            </div>

            <div class="story-item">
                <div class="story-circle">
                    <span class="story-val">{formatStat(stats.totalStaff)}</span>
                </div>
                <span class="story-label">Мастера</span>
            </div>

            <div class="story-item">
                <div class="story-circle secondary">
                    <span class="story-val">{formatStat(stats.totalResources)}</span>
                </div>
                <span class="story-label">Ресурсы</span>
            </div>

            <!-- Задел под будущие истории -->
            <div class="story-item placeholder">
                <div class="story-circle dotted">
                    <span class="story-val">+</span>
                </div>
                <span class="story-label">Добавить</span>
            </div>
        </div>
    </div>

    <!-- СЕКЦИЯ УПРАВЛЕНИЯ (ЗОЛОТАЯ СЕТКА) -->
    <section class="main-controls">
        <label class="phi-caption">ИНСТРУМЕНТЫ</label>

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

    /* STORIES: Горизонтальная лента */
    .stories-container {
        padding: 24px 0 16px 0;
        border-bottom: 1px solid rgba(147, 161, 161, 0.1);
        margin-bottom: 24px;
    }
    .stories-track {
        display: flex; gap: 18px;
        padding: 0 20px;
        overflow-x: auto;
        scrollbar-width: none;
    }
    .stories-track::-webkit-scrollbar { display: none; }

    .story-item { display: flex; flex-direction: column; align-items: center; gap: 8px; flex-shrink: 0; }

    .story-circle {
        width: 68px; height: 68px;
        background: #eee8d5;
        border-radius: 50%;
        display: flex; align-items: center; justify-content: center;
        border: 2px solid #fdf6e3;
        box-shadow: 0 0 0 2px #ddd6c1; /* "Обойка" сторис */
        position: relative;
    }
    .story-circle.highlight { box-shadow: 0 0 0 2px #268bd2; }
    .story-circle.secondary { box-shadow: 0 0 0 2px #2aa198; }

    .story-val { font-size: 18px; font-weight: 900; color: #073642; }
    .story-label { font-size: 10px; font-weight: 800; color: #93a1a1; text-transform: uppercase; letter-spacing: 0.5px; }

    .story-circle.dotted { border: 2px dashed #93a1a1; background: transparent; box-shadow: none; }
    .placeholder .story-val { color: #93a1a1; font-weight: 300; font-size: 24px; }

    /* GRID: Золотое сечение 1.618 */
    .main-controls { padding: 0 20px; flex: 1; }
    .phi-caption { display: block; font-size: 10px; font-weight: 950; color: #93a1a1; letter-spacing: 2px; margin-bottom: 16px; opacity: 0.8; }

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
        padding: 20px;
        display: flex; flex-direction: column; align-items: flex-start; gap: 12px;
        aspect-ratio: 1 / 0.75; /* Приближение к золотому сечению для плитки */
        cursor: pointer;
        transition: transform 0.2s;
    }
    .phi-card:active { transform: scale(0.96); background: white; border-color: #268bd2; }
    .card-icon { font-size: 24px; }
    .phi-card h3 { margin: 0; font-size: 15px; font-weight: 850; color: #073642; }

    /* WIDE CARD: Акцент */
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
    .wide-text h3 { margin: 0; font-size: 17px; font-weight: 850; color: #073642; }
    .wide-text p { margin: 2px 0 0 0; font-size: 11px; color: #586e75; font-weight: 600; }
    .phi-arrow { font-size: 22px; color: #268bd2; font-weight: 300; }

    .bottom-phi-spacer { height: 120px; }
</style>
