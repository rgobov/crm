<script>
    import { onMount } from 'svelte';
    import { serviceService } from '$lib/services/serviceService.js';
    import { goto } from '$app/navigation';
    import { fade } from 'svelte/transition';

    let services = [];
    let isLoading = true;
    let tg = null;

    onMount(async () => {
        if (window.Telegram && window.Telegram.WebApp) {
            tg = window.Telegram.WebApp;
            tg.BackButton.show();
            tg.BackButton.onClick(() => goto('/admin'));
        }
        await loadServices();
    });

    async function loadServices() {
        isLoading = true;
        try {
            services = await serviceService.getServices();
        } catch (e) {
            console.error('Failed to load services');
        } finally {
            isLoading = false;
        }
    }

    async function handleDelete(id, name) {
        if (confirm(`Удалить услугу "${name}"?`)) {
            try {
                await serviceService.deleteService(id);
                await loadServices();
            } catch (e) {
                alert('Ошибка при удалении');
            }
        }
    }
</script>

<div class="screen-wrapper">
    <div class="screen-content">
        <!-- ВНУТРЕННИЙ КОНТЕЙНЕР ДЛЯ ЦЕНТРИРОВАНИЯ И ПРИВЫЧНОГО СКРОЛЛА -->
        <div class="container-inner">
            <header class="header">
                <div class="title-wrap">
                    <h1>Услуги</h1>
                    <span class="count-badge">{services.length}</span>
                </div>
                <p class="subtitle">Ваш прейскурант и длительность</p>
            </header>

            {#if isLoading && services.length === 0}
                <div class="center-loader"><span class="spinner"></span></div>
            {:else if services.length === 0}
                <div class="empty-state" in:fade>
                    <div class="empty-icon">✂️</div>
                    <h3>Нет активных услуг</h3>
                    <p>Добавьте первую услугу для записи клиентов</p>
                    <button class="btn-prime" on:click={() => goto('/admin/services/new')}>Создать услугу</button>
                </div>
            {:else}
                <div class="tiles-grid">
                    {#each services as service}
                        <div class="service-tile" on:click={() => goto(`/admin/services/${service.id}`)}>
                            <div class="icon-circle">✨</div>
                            <div class="info">
                                <h3>{service.name}</h3>
                                <p>{service.durationInMinutes} минут</p>
                            </div>
                            <button class="btn-del" on:click|stopPropagation={() => handleDelete(service.id, service.name)}>
                                🗑
                            </button>
                        </div>
                    {/each}
                </div>

                <div class="bottom-spacer"></div>
            {/if}
        </div>
    </div>

    <button class="fab-btn" on:click={() => goto('/admin/services/new')}>+</button>
</div>

<style>
    .screen-wrapper {
        height: 100vh;
        width: 100%;
        display: flex;
        flex-direction: column;
        background: #f8fafc;
        overflow: hidden;
        position: relative;
    }

    .screen-content {
        flex: 1;
        overflow-y: auto;
        width: 100%;
        box-sizing: border-box;
        -webkit-overflow-scrolling: touch;
    }

    .container-inner {
        max-width: 800px;
        margin: 0 auto;
        padding: 32px 20px;
    }

    .header { margin-bottom: 32px; }
    .title-wrap { display: flex; align-items: center; gap: 12px; }
    h1 { font-size: 28px; font-weight: 850; margin: 0; color: #0f172a; }
    .count-badge { background: #eff6ff; color: var(--primary-color); padding: 4px 12px; border-radius: 10px; font-size: 14px; font-weight: 800; }
    .subtitle { color: #94a3b8; margin: 8px 0 0 0; font-weight: 600; }

    .tiles-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
        gap: 16px;
    }

    .service-tile {
        background: white; padding: 20px; border-radius: 24px; display: flex; align-items: center; gap: 16px;
        border: 1px solid #f1f5f9; cursor: pointer; transition: all 0.2s; box-shadow: 0 4px 12px rgba(0,0,0,0.02);
    }
    .service-tile:hover { transform: translateY(-2px); box-shadow: 0 10px 25px rgba(0,0,0,0.05); border-color: var(--primary-color); }

    .icon-circle { width: 48px; height: 48px; background: #fffbeb; color: #ca8a04; border-radius: 14px; display: flex; align-items: center; justify-content: center; font-size: 22px; }

    .info { flex: 1; }
    .info h3 { margin: 0; font-size: 16px; font-weight: 800; color: #1e293b; }
    .info p { margin: 4px 0 0 0; font-size: 13px; color: #94a3b8; font-weight: 500; }

    .btn-del { background: #fef2f2; color: #ef4444; border: none; width: 36px; height: 36px; border-radius: 10px; cursor: pointer; opacity: 0; transition: 0.2s; }
    .service-tile:hover .btn-del { opacity: 1; }

    .fab-btn { position: fixed; bottom: 40px; right: 40px; width: 64px; height: 64px; background: var(--primary-gradient); color: white; border: none; border-radius: 20px; font-size: 32px; font-weight: 300; cursor: pointer; box-shadow: 0 10px 25px rgba(56, 151, 240, 0.3); transition: 0.2s; z-index: 100; }
    .fab-btn:active { transform: scale(0.9); }

    .bottom-spacer { height: 120px; }

    .center-loader { display: flex; justify-content: center; padding: 100px; }
    .spinner { width: 32px; height: 32px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }

    @media (max-width: 640px) {
        .container-inner { padding: 20px; }
        .tiles-grid { grid-template-columns: 1fr; }
        .fab-btn { bottom: 100px; right: 20px; }
    }
</style>
