<script>
    import { onMount } from 'svelte';
    import { resourceService } from '$lib/services/resourceService.js';
    import { branchService } from '$lib/services/branchService.js';
    import { activeBranchId } from '$lib/stores/dashboardStore.js';
    import ResourceEditScreen from './ResourceEditScreen.svelte';
    import { fade, scale } from 'svelte/transition';

    let resources = [];
    let branches = [];
    let isLoading = true;
    let showModal = false;
    let selectedResource = null;

    onMount(async () => {
        await Promise.all([
            loadResources(),
            loadBranches()
        ]);
    });

    async function loadResources() {
        isLoading = true;
        try {
            if (!$activeBranchId) {
                console.error('Branch ID not available for resource loading');
                resources = [];
                return;
            }
            resources = await resourceService.getResources($activeBranchId);
        } catch (e) {
            console.error('Failed to load resources', e);
            resources = [];
        } finally {
            isLoading = false;
        }
    }

    async function loadBranches() {
        try {
            branches = await branchService.getBranches();
        } catch (e) {
            console.error('Failed to load branches');
        }
    }

    function getBranchName(branchId) {
        if (!branchId) return 'Филиал не указан ⚠️';
        const branch = branches.find(b => b.id === branchId);
        return branch ? branch.name : 'Филиал не найден';
    }

    // Реактивная загрузка ресурсов при смене филиала
    $: if ($activeBranchId) {
        loadResources();
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
    <header class="sticky-header">
        <div class="header-inner">
            <div class="title-row">
                <h1>Ресурсы</h1>
                <span class="count-badge">{resources.length}</span>
                <button class="add-header-btn" on:click={openCreate}>+</button>
            </div>
            <p class="subtitle">Управление кабинетами и оборудованием филиалов</p>
        </div>
    </header>

    <div class="screen-content">
        <div class="container-inner">

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
                                <div class="name-row">
                                    <h3>{resource.name}</h3>
                                    <!-- ТЕПЕРЬ СИНИЙ БЕЙДЖИК -->
                                    <span class="branch-tag" class:warning={!resource.branchId}>
                                        {getBranchName(resource.branchId)}
                                    </span>
                                </div>
                                <p>{resource.description || 'Без описания'}</p>
                            </div>
                            <button class="btn-del" on:click|stopPropagation={() => handleDelete(resource.id, resource.name)} title="Удалить">
                                🗑
                            </button>
                        </div>
                    {/each}
                </div>

                <div class="bottom-spacer"></div>
            {/if}
        </div>
    </div>


    {#if showModal}
        <ResourceEditScreen
            resource={selectedResource}
            on:cancel={() => showModal = false}
            on:success={handleSuccess}
        />
    {/if}
</div>

<style>
    .screen-wrapper { height: 100vh; width: 100%; display: flex; flex-direction: column; background: #f8fafc; overflow: hidden; }
    .sticky-header { background: white; border-bottom: 1px solid #f1f5f9; z-index: 100; box-shadow: 0 4px 12px rgba(0,0,0,0.02); }
    .header-inner { max-width: 800px; margin: 0 auto; padding: 24px 32px 12px; }
    .title-row { display: flex; align-items: center; gap: 12px; min-height: 36px; }
    h1 { font-size: 28px; font-weight: 850; margin: 0; color: #0f172a; }
    .count-badge { background: #eff6ff; color: var(--primary-color); padding: 4px 12px; border-radius: 10px; font-size: 14px; font-weight: 800; }
    .subtitle { color: #94a3b8; margin: 8px 0 0 0; font-weight: 600; }
    .screen-content { flex: 1; overflow-y: auto; width: 100%; box-sizing: border-box; }
    .container-inner { max-width: 800px; margin: 0 auto; padding: 32px 20px; }

    .tiles-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; }

    .resource-tile {
        background: white; padding: 20px; border-radius: 24px; display: flex; align-items: center; gap: 16px;
        border: 1px solid #f1f5f9; cursor: pointer; transition: all 0.2s; box-shadow: 0 4px 12px rgba(0,0,0,0.02);
    }
    .resource-tile:hover { transform: translateY(-2px); box-shadow: 0 10px 25px rgba(0,0,0,0.05); border-color: var(--primary-color); }

    .icon-circle { width: 48px; height: 48px; background: #f8fafc; border-radius: 14px; display: flex; align-items: center; justify-content: center; font-size: 22px; }

    .info { flex: 1; }
    .name-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 4px; gap: 10px; }
    .info h3 { margin: 0; font-size: 16px; font-weight: 800; color: #1e293b; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

    /* СИНИЙ БЕЙДЖИК ФИЛИАЛА */
    .branch-tag {
        background: #eff6ff; color: var(--primary-color);
        font-size: 9px; font-weight: 900; padding: 4px 8px; border-radius: 8px;
        text-transform: uppercase; letter-spacing: 0.5px; white-space: nowrap;
        border: 1px solid #dbeafe;
    }
    .branch-tag.warning { background: #fff7ed; color: #ea580c; border-color: #ffedd5; }

    .info p { margin: 0; font-size: 13px; color: #94a3b8; font-weight: 500; }

    .btn-del { background: #fef2f2; color: #ef4444; border: none; width: 36px; height: 36px; border-radius: 10px; cursor: pointer; opacity: 0; transition: 0.2s; }
    .resource-tile:hover .btn-del { opacity: 1; }

    .add-header-btn { margin-left: auto; width: 44px; height: 44px; background: var(--primary-gradient); color: white; border: none; border-radius: 14px; font-size: 28px; font-weight: 300; line-height: 1; display: flex; align-items: center; justify-content: center; cursor: pointer; box-shadow: 0 4px 12px rgba(56, 151, 240, 0.35); transition: transform 0.15s, box-shadow 0.15s; flex-shrink: 0; }
    .add-header-btn:active { transform: scale(0.92); box-shadow: 0 2px 6px rgba(56, 151, 240, 0.2); }

    .center-loader { display: flex; justify-content: center; padding: 100px; }
    .spinner { width: 32px; height: 32px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }

    @media (max-width: 640px) {
        .container-inner { padding: 20px; }
        .tiles-grid { grid-template-columns: 1fr; }
    }
</style>
