<script>
    import { onMount } from 'svelte';
    import { branchService } from '$lib/services/branchService.js';
    import { branchStore } from '$lib/stores/branchStore.js';
    import BranchEditModal from '$lib/components/branches/BranchEditModal.svelte';
    import { fade, scale } from 'svelte/transition';

    let isLoading = true;
    let showModal = false;
    let selectedBranch = null;

    $: branches = $branchStore;

    onMount(async () => {
        await loadBranches();
    });

    async function loadBranches() {
        isLoading = true;
        try {
            await branchStore.refresh();
        } catch (e) {
            console.error('Failed to load branches', e);
        } finally {
            isLoading = false;
        }
    }

    function openCreate() {
        selectedBranch = null;
        showModal = true;
    }

    function openEdit(branch) {
        selectedBranch = branch;
        showModal = true;
    }

    function handleSuccess() {
        showModal = false;
        loadBranches();
    }

    async function handleDelete(id, name) {
        if (confirm(`Удалить филиал "${name}"?`)) {
            try {
                await branchService.deleteBranch(id);
                await loadBranches();
            } catch (e) {
                alert('Ошибка при удалении');
            }
        }
    }
</script>

<div class="page-scroll-container">
    <div class="page-wrapper">
        <header class="page-header">
            <div class="title-row">
                <a href="/admin" class="back-link">‹</a>
                <h1>Филиалы</h1>
            </div>
        </header>

        {#if isLoading}
            <div class="loader-box"><span class="spinner"></span></div>
        {:else}
            <div class="branches-grid">
                <!-- КНОПКА ДОБАВЛЕНИЯ В ВИДЕ КАРТОЧКИ -->
                <button class="add-card" on:click={openCreate} in:scale>
                    <span class="plus-icon">+</span>
                    <span class="add-text">Добавить новый филиал</span>
                </button>

                {#each branches as branch}
                    <div class="branch-card" on:click={() => openEdit(branch)} in:fade>
                        <div class="card-content">
                            <h3>{branch.name}</h3>
                            <p>{branch.address || 'Адрес не указан'}</p>
                        </div>
                        <div class="card-footer">
                            <span>{branch.timezone}</span>
                            <button class="btn-delete-mini" on:click|stopPropagation={() => handleDelete(branch.id, branch.name)}>🗑️</button>
                        </div>
                    </div>
                {/each}
            </div>
            <div class="bottom-spacer" aria-hidden="true"></div>
        {/if}

        {#if showModal}
            <BranchEditModal branch={selectedBranch} on:close={() => showModal = false} on:success={handleSuccess} />
        {/if}
    </div>
</div>

<style>
    .page-scroll-container {
        flex: 1;
        overflow-y: auto;
        height: 100%;
        background: #fdf6e3;
        -webkit-overflow-scrolling: touch;
    }
    .page-wrapper { padding: 32px; max-width: 1000px; margin: 0 auto; min-height: 100%; }
    .page-header { margin-bottom: 32px; }
    .title-row { display: flex; align-items: center; gap: 16px; }
    .back-link { font-size: 32px; text-decoration: none; color: #93a1a1; line-height: 1; transition: color 0.2s; }
    .back-link:hover { color: #268bd2; }
    h1 { font-size: 28px; font-weight: 900; margin: 0; color: #073642; letter-spacing: -0.5px; }

    .branches-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 24px; }

    /* СТИЛИ КАРТОЧКИ ДОБАВЛЕНИЯ */
    .add-card {
        background: none;
        border: 2px dashed #ddd6c1;
        border-radius: 32px;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: 12px;
        cursor: pointer;
        min-height: 180px;
        transition: all 0.3s ease;
    }
    .add-card:hover { border-color: #268bd2; background: #eee8d5; transform: scale(1.02); }
    .add-card .plus-icon { font-size: 48px; color: #93a1a1; line-height: 1; transition: color 0.3s; }
    .add-card:hover .plus-icon { color: #268bd2; }
    .add-card .add-text { font-size: 14px; font-weight: 700; color: #93a1a1; transition: color 0.3s; }
    .add-card:hover .add-text { color: #073642; }

    .branch-card { background: #eee8d5; border-radius: 32px; box-shadow: 0 4px 20px rgba(0,0,0,0.03); cursor: pointer; transition: all 0.3s; border: 1.5px solid #ddd6c1; overflow: hidden; display: flex; flex-direction: column; }
    .branch-card:hover { transform: translateY(-5px); box-shadow: 0 20px 40px rgba(0,0,0,0.06); border-color: #268bd2; background: white; }

    .card-content { padding: 28px; flex: 1; }
    .card-content h3 { margin: 0 0 10px 0; font-size: 20px; color: #073642; font-weight: 800; }
    .card-content p { margin: 0; color: #586e75; font-size: 15px; line-height: 1.4; font-weight: 500; }

    .card-footer { display: flex; justify-content: space-between; align-items: center; padding: 16px 28px; border-top: 1px solid #ddd6c1; background: rgba(238, 232, 213, 0.5); }
    .card-footer span { font-size: 13px; font-weight: 800; color: #93a1a1; text-transform: uppercase; letter-spacing: 0.5px; }

    .btn-delete-mini { background: #eee8d5; color: #dc322f; border: 1px solid #ddd6c1; width: 36px; height: 36px; border-radius: 12px; cursor: pointer; opacity: 0.6; transition: all 0.2s; font-size: 14px; }
    .btn-delete-mini:hover { opacity: 1; transform: scale(1.1); background: #dc322f; color: white; border-color: #dc322f; }

    .loader-box { text-align: center; padding: 100px; }
    .spinner { width: 40px; height: 40px; border: 4px solid #eee8d5; border-top-color: #268bd2; border-radius: 50%; display: inline-block; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }

    .bottom-spacer { display: none; }

    @media (max-width: 1279px) {
        .page-scroll-container { min-height: 0; box-sizing: border-box; }
        .page-wrapper { padding: 20px; }
        .branches-grid { gap: 16px; }
        .bottom-spacer { display: block; height: calc(120px + env(safe-area-inset-bottom, 0px)); }
    }
</style>
