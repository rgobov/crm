<script>
    import { onMount } from 'svelte';
    import { resourceService } from '$lib/services/resourceService.js';
    import ResourceEditScreen from './ResourceEditScreen.svelte';
    import { fade } from 'svelte/transition';

    let resources = [];
    let isLoading = true;
    let showModal = false;
    let selectedResource = null;

    onMount(async () => {
        await loadResources();
    });

    async function loadResources() {
        isLoading = true;
        try {
            resources = await resourceService.getResources();
        } catch (e) {
            console.error('Failed to load resources');
        } finally {
            isLoading = false;
        }
    }

    function openCreate() {
        selectedResource = null;
        showModal = true;
    }

    function openEdit(resource) {
        selectedResource = resource;
        showModal = true;
    }

    function handleSuccess() {
        showModal = false;
        loadResources();
    }

    async function handleDelete(id, name) {
        if (confirm(`Удалить ресурс "${name}"?`)) {
            try {
                await resourceService.deleteResource(id);
                await loadResources();
            } catch (e) {
                alert('Ошибка при удалении');
            }
        }
    }
</script>

<div class="screen-wrapper">
    <div class="screen-content">
        <header class="header">
            <div class="title-wrap">
                <h1>Ресурсы</h1>
                <span class="count-badge">{resources.length}</span>
            </div>
            <p class="subtitle">Кабинеты, залы и оборудование</p>
        </header>

        {#if isLoading && resources.length === 0}
            <div class="center-loader"><span class="spinner"></span></div>
        {:else if resources.length === 0}
            <div class="empty-state" in:fade>
                <div class="empty-icon">🛠️</div>
                <h3>Нет активных ресурсов</h3>
                <p>Добавьте первый кабинет для работы</p>
                <button class="btn-prime" on:click={openCreate}>Создать ресурс</button>
            </div>
        {:else}
            <div class="tiles-grid">
                {#each resources as resource}
                    <div class="resource-tile" on:click={() => openEdit(resource)}>
                        <div class="icon-circle">📦</div>
                        <div class="info">
                            <h3>{resource.name}</h3>
                            <p>{resource.description || 'Без описания'}</p>
                        </div>
                        <button class="btn-del" on:click|stopPropagation={() => handleDelete(resource.id, resource.name)}>
                            🗑
                        </button>
                    </div>
                {/each}
            </div>

            <div class="bottom-spacer"></div>
        {/if}
    </div>

    <button class="fab-btn" on:click={openCreate}>+</button>

    <!-- МОДАЛЬНОЕ ОКНО РЕДАКТИРОВАНИЯ/СОЗДАНИЯ -->
    {#if showModal}
        <ResourceEditScreen
            resource={selectedResource}
            on:cancel={() => showModal = false}
            on:success={handleSuccess}
        />
    {/if}
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
        padding: 32px;
        max-width: 800px;
        margin: 0 auto;
        width: 100%;
        box-sizing: border-box;
        -webkit-overflow-scrolling: touch;
    }

    .header { margin-bottom: 32px; }
    .title-wrap { display: flex; align-items: center; gap: 12px; }
    h1 { font-size: 28px; font-weight: 850; margin: 0; color: #0f172a; }
    .count-badge { background: #eff6ff; color: var(--primary-color); padding: 4px 12px; border-radius: 10px; font-size: 14px; font-weight: 800; }
    .subtitle { color: #94a3b8; margin: 8px 0 0 0; font-weight: 600; }

    .tiles-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; }

    .resource-tile {
        background: white; padding: 20px; border-radius: 24px; display: flex; align-items: center; gap: 16px;
        border: 1px solid #f1f5f9; cursor: pointer; transition: all 0.2s; box-shadow: 0 4px 12px rgba(0,0,0,0.02);
    }
    .resource-tile:hover { transform: translateY(-2px); box-shadow: 0 10px 25px rgba(0,0,0,0.05); border-color: var(--primary-color); }

    .icon-circle { width: 48px; height: 48px; background: #f8fafc; border-radius: 14px; display: flex; align-items: center; justify-content: center; font-size: 22px; }

    .info { flex: 1; }
    .info h3 { margin: 0; font-size: 16px; font-weight: 800; color: #1e293b; }
    .info p { margin: 4px 0 0 0; font-size: 13px; color: #94a3b8; font-weight: 500; }

    .btn-del { background: #fef2f2; color: #ef4444; border: none; width: 36px; height: 36px; border-radius: 10px; cursor: pointer; opacity: 0; transition: 0.2s; }
    .resource-tile:hover .btn-del { opacity: 1; }

    .fab-btn { position: fixed; bottom: 40px; right: 40px; width: 64px; height: 64px; background: var(--primary-gradient); color: white; border: none; border-radius: 20px; font-size: 32px; font-weight: 300; cursor: pointer; box-shadow: 0 10px 25px rgba(56, 151, 240, 0.3); transition: 0.2s; z-index: 100; }
    .fab-btn:active { transform: scale(0.9); }

    .bottom-spacer { height: 120px; }

    .empty-state { text-align: center; padding: 80px 20px; }
    .empty-icon { font-size: 64px; margin-bottom: 24px; opacity: 0.5; }
    .btn-prime { background: var(--primary-gradient); color: white; border: none; padding: 14px 32px; border-radius: 16px; font-weight: 800; cursor: pointer; }

    .center-loader { display: flex; justify-content: center; padding: 100px; }
    .spinner { width: 32px; height: 32px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }

    @media (max-width: 640px) {
        .screen-content { padding: 20px; }
        .tiles-grid { grid-template-columns: 1fr; }
        .fab-btn { bottom: 100px; right: 20px; }
    }
</style>
