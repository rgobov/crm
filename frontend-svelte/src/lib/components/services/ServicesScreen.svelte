<script>
    import { onMount } from 'svelte';
    import { serviceService } from '$lib/services/serviceService.js';
    import { goto } from '$app/navigation';

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

<div class="screen">
    <div class="header">
        <h1>Услуги</h1>
        <p>Ваш прайс-лист ({services.length})</p>
    </div>

    {#if isLoading}
        <div class="center"><span class="spinner"></span></div>
    {:else if services.length === 0}
        <div class="empty-state">
            <span class="icon">✂️</span>
            <p>У вас пока нет услуг</p>
            <button class="add-btn" on:click={() => goto('/admin/services/new')}>Добавить услугу</button>
        </div>
    {:else}
        <div class="service-list">
            {#each services as service}
                <div class="service-card card" on:click={() => goto(`/admin/services/${service.id}`)}>
                    <div class="icon-box">✂️</div>
                    <div class="info">
                        <h3>{service.name}</h3>
                        <p>{service.durationInMinutes} минут</p>
                    </div>
                    <button class="delete-btn" on:click|stopPropagation={() => handleDelete(service.id, service.name)}>
                        🗑
                    </button>
                </div>
            {/each}
        </div>

        <button class="fab" on:click={() => goto('/admin/services/new')}>+</button>
    {/if}
</div>

<style>
    .screen { padding: 20px; max-width: 600px; margin: 0 auto; min-height: 100vh; }
    .header h1 { font-size: 24px; font-weight: 800; margin: 0; color: #0f172a; }
    .header p { color: var(--hint-color); margin: 4px 0 24px 0; }

    .service-list { display: grid; gap: 12px; }
    .service-card { display: flex; align-items: center; gap: 16px; padding: 16px; background: white; border-radius: 20px; cursor: pointer; transition: transform 0.1s; }
    .service-card:active { transform: scale(0.98); }

    .icon-box { width: 44px; height: 44px; background: #eff6ff; color: var(--primary-color); border-radius: 12px; display: flex; justify-content: center; align-items: center; font-size: 20px; }

    .info { flex: 1; }
    .info h3 { margin: 0; font-size: 16px; color: #1e293b; font-weight: 700; }
    .info p { margin: 2px 0 0 0; font-size: 13px; color: var(--hint-color); }

    .delete-btn { background: #fef2f2; color: #ef4444; border: none; width: 36px; height: 36px; border-radius: 10px; cursor: pointer; font-size: 16px; }

    .fab { position: fixed; bottom: 90px; right: 20px; width: 60px; height: 60px; background: var(--primary-gradient); color: white; border: none; border-radius: 20px; font-size: 32px; box-shadow: 0 12px 30px rgba(56, 151, 240, 0.4); cursor: pointer; z-index: 100; }

    .empty-state { text-align: center; padding: 60px 20px; }
    .empty-state .icon { font-size: 64px; margin-bottom: 16px; display: block; }
    .add-btn { background: var(--primary-gradient); color: white; border: none; padding: 12px 24px; border-radius: 14px; font-weight: 700; margin-top: 16px; }

    .center { display: flex; justify-content: center; padding: 40px; }
    .spinner { width: 30px; height: 30px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
</style>
