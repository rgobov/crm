<script>
    import { onMount } from 'svelte';
    import { serviceService } from '$lib/services/serviceService.js';
    import { goto } from '$app/navigation';
    import { fade } from 'svelte/transition';

    let services = [];
    let isLoading = true;
    let tg = null;

    onMount(async () => {
        try {
            if (window.Telegram && window.Telegram.WebApp) {
                tg = window.Telegram.WebApp;
                if (tg.BackButton) {
                    tg.BackButton.show();
                    tg.BackButton.onClick(() => goto('/admin'));
                }
            }
            await loadServices();
        } catch (e) {
            console.error('onMount error:', e);
            isLoading = false;
        }
    });

    async function loadServices() {
        isLoading = true;
        try {
            services = await serviceService.getServices();
        } catch (e) {
            console.error('Failed to load services:', e);
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
                console.error('Ошибка при удалении услуги:', e);
                const errMsg = e.response?.data?.message || e.message || 'Неизвестная ошибка';
                alert(`Ошибка при удалении: ${errMsg}`);
            }
        }
    }
</script>

<div class="screen-wrapper">
    <header class="sticky-header">
        <div class="header-inner">
            <div class="title-row">
                <h1>Услуги</h1>
                <span class="count-badge">{services.length}</span>
                <button class="add-header-btn" on:click={() => goto('/admin/services/new')}>+</button>
            </div>
            <p class="subtitle">Ваш прейскурант и длительность</p>
        </div>
    </header>

    <div class="screen-content">
        <div class="container-inner">

            {#if isLoading && services.length === 0}
                <div class="center-loader"><span class="spinner"></span></div>
            {:else if services.length === 0}
                <div class="empty-state" in:fade>
                    <div class="empty-icon">⭐</div>
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
                                <p>
                                    {service.durationInMinutes} минут
                                    {#if service.priceMin !== null && service.priceMin !== undefined}
                                        • {service.priceMax !== null && service.priceMax !== undefined ? `от ${service.priceMin} до ${service.priceMax}` : service.priceMin} руб.
                                    {/if}
                                </p>
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
</div>

<style>
    .screen-wrapper {
        height: 100vh;
        width: 100%;
        display: flex;
        flex-direction: column;
        background: #fdf6e3;
        overflow: hidden;
    }

    .sticky-header { background: #fdf6e3; border-bottom: 1px solid #ddd6c1; z-index: 100; }
    .header-inner { max-width: 800px; margin: 0 auto; padding: 24px 32px 12px; }
    .title-row { display: flex; align-items: center; gap: 12px; min-height: 36px; }
    h1 { font-size: 28px; font-weight: 850; margin: 0; color: #073642; }
    .count-badge { background: #eee8d5; color: #268bd2; padding: 4px 12px; border-radius: 10px; font-size: 14px; font-weight: 800; }
    .subtitle { color: #586e75; margin: 8px 0 0 0; font-weight: 600; }

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

    .tiles-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
        gap: 16px;
    }

    .service-tile {
        background: #eee8d5; padding: 20px; border-radius: 24px; display: flex; align-items: center; gap: 16px;
        border: 1px solid #ddd6c1; cursor: pointer; transition: all 0.2s;
    }
    .service-tile:hover { transform: translateY(-2px); border-color: #268bd2; }

    .icon-circle { width: 48px; height: 48px; background: #fdf6e3; color: #268bd2; border-radius: 14px; display: flex; align-items: center; justify-content: center; font-size: 22px; }

    .info { flex: 1; }
    .info h3 { margin: 0; font-size: 16px; font-weight: 800; color: #073642; }
    .info p { margin: 4px 0 0 0; font-size: 13px; color: #586e75; font-weight: 500; }

    .btn-del { background: #fdf6e3; color: #dc322f; border: none; width: 36px; height: 36px; border-radius: 10px; cursor: pointer; opacity: 0; transition: 0.2s; }
    .service-tile:hover .btn-del { opacity: 1; }

    .add-header-btn { margin-left: auto; width: 44px; height: 44px; background: #268bd2; color: white; border: none; border-radius: 14px; font-size: 28px; font-weight: 300; line-height: 1; display: flex; align-items: center; justify-content: center; cursor: pointer; transition: transform 0.15s, box-shadow 0.15s; flex-shrink: 0; }
    .add-header-btn:active { transform: scale(0.92); }

    .btn-prime {
        background: #268bd2;
        color: white;
        border: none;
        padding: 12px 24px;
        border-radius: 12px;
        font-weight: 700;
        cursor: pointer;
        margin-top: 16px;
    }

    .empty-state {
        text-align: center;
        padding: 60px 20px;
        color: #586e75;
    }
    .empty-state h3 { color: #073642; margin: 16px 0 8px; }

    .bottom-spacer { height: 120px; }

    .center-loader { display: flex; justify-content: center; padding: 100px; }
    .spinner { width: 32px; height: 32px; border: 3px solid #eee8d5; border-top-color: #268bd2; border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }

    @media (max-width: 640px) {
        .container-inner { padding: 20px; }
        .tiles-grid { grid-template-columns: 1fr; }
    }
</style>
