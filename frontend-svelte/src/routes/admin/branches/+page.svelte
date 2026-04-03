<script>
    import { onMount } from 'svelte';
    import { branchService } from '$lib/services/branchService.js';
    import BranchEditModal from '$lib/components/branches/BranchEditModal.svelte';
    import { fade, scale } from 'svelte/transition';

    let branches = [];
    let isLoading = true;
    let showModal = false;
    let selectedBranch = null;

    onMount(async () => {
        await loadBranches();
    });

    async function loadBranches() {
        isLoading = true;
        try {
            branches = await branchService.getBranches();
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
    {/if}

    {#if showModal}
        <BranchEditModal branch={selectedBranch} on:close={() => showModal = false} on:success={handleSuccess} />
    {/if}
</div>

<style>
    .page-wrapper { padding: 32px; max-width: 1000px; margin: 0 auto; min-height: 100vh; background: #f8fafc; }
    .page-header { margin-bottom: 32px; }
    .title-row { display: flex; align-items: center; gap: 16px; }
    .back-link { font-size: 32px; text-decoration: none; color: #94a3b8; line-height: 1; transition: color 0.2s; }
    .back-link:hover { color: #0ea5e9; }
    h1 { font-size: 28px; font-weight: 900; margin: 0; color: #1e293b; letter-spacing: -0.5px; }

    .branches-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 24px; }

    /* СТИЛИ КАРТОЧКИ ДОБАВЛЕНИЯ */
    .add-card {
        background: none;
        border: 2px dashed #cbd5e1;
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
    .add-card:hover { border-color: #0ea5e9; background: white; transform: scale(1.02); box-shadow: 0 10px 30px rgba(14, 165, 233, 0.1); }
    .add-card .plus-icon { font-size: 48px; color: #94a3b8; line-height: 1; transition: color 0.3s; }
    .add-card:hover .plus-icon { color: #0ea5e9; }
    .add-card .add-text { font-size: 14px; font-weight: 700; color: #94a3b8; transition: color 0.3s; }
    .add-card:hover .add-text { color: #1e293b; }

    .branch-card { background: white; border-radius: 32px; box-shadow: 0 4px 20px rgba(0,0,0,0.03); cursor: pointer; transition: all 0.3s; border: 1.5px solid transparent; overflow: hidden; display: flex; flex-direction: column; }
    .branch-card:hover { transform: translateY(-5px); box-shadow: 0 20px 40px rgba(0,0,0,0.06); border-color: #0ea5e9; }

    .card-content { padding: 28px; flex: 1; }
    .card-content h3 { margin: 0 0 10px 0; font-size: 20px; color: #1e293b; font-weight: 800; }
    .card-content p { margin: 0; color: #64748b; font-size: 15px; line-height: 1.4; font-weight: 500; }

    .card-footer { display: flex; justify-content: space-between; align-items: center; padding: 16px 28px; border-top: 1px solid #f1f5f9; background: #fcfdfe; }
    .card-footer span { font-size: 13px; font-weight: 800; color: #94a3b8; text-transform: uppercase; letter-spacing: 0.5px; }

    .btn-delete-mini { background: #fee2e2; color: #ef4444; border: none; width: 36px; height: 36px; border-radius: 12px; cursor: pointer; opacity: 0.6; transition: all 0.2s; font-size: 14px; }
    .btn-delete-mini:hover { opacity: 1; transform: scale(1.1); }

    .loader-box { text-align: center; padding: 100px; }
    .spinner { width: 40px; height: 40px; border: 4px solid #f1f5f9; border-top-color: #0ea5e9; border-radius: 50%; display: inline-block; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
</style>
