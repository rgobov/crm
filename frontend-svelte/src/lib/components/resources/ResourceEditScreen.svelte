<script>
    import { createEventDispatcher, onMount } from 'svelte';
    import { resourceService } from '$lib/services/resourceService.js';
    import { branchService } from '$lib/services/branchService.js';
    import { activeBranchId } from '$lib/stores/dashboardStore.js';
    import { fade, scale } from 'svelte/transition';
    import { quintOut } from 'svelte/easing';
    import { portal } from '$lib/actions/portal.js';

    export let resource = null;
    const dispatch = createEventDispatcher();

    let branches = [];
    let formData = {
        name: resource?.name || '',
        description: resource?.description || '',
        branchId: resource?.branchId || $activeBranchId || ''
    };

    let isSaving = false;
    let isLoading = true;

    onMount(async () => {
        try {
            branches = await branchService.getBranches();
            // Если филиал не задан (создание) и в сторе есть активный - подставляем
            if (!formData.branchId && $activeBranchId) {
                formData.branchId = $activeBranchId;
            }
        } catch (e) {
            console.error('Failed to load branches');
        } finally {
            isLoading = false;
        }
    });

    async function handleSave() {
        if (!formData.name.trim()) return alert('Введите название');
        if (!formData.branchId) return alert('Выберите филиал');

        isSaving = true;
        try {
            let result;
            if (resource?.id) {
                result = await resourceService.updateResource(resource.id, formData);
            } else {
                result = await resourceService.addResource(formData);
            }
            dispatch('success', result);
        } catch (e) {
            alert('Ошибка при сохранении');
        } finally {
            isSaving = false;
        }
    }
</script>

<div class="modal-backdrop" use:portal on:click|self={() => dispatch('cancel')} transition:fade={{duration: 200}}>
    <div class="modal-content" in:scale={{duration: 400, start: 0.95, easing: quintOut}}>

        <header class="modal-header">
            <div class="header-icon">📦</div>
            <div class="header-text">
                <h3>{resource ? 'Редактировать' : 'Новый ресурс'}</h3>
                <p>Кабинеты и оборудование</p>
            </div>
            <button class="close-btn" on:click={() => dispatch('cancel')}>✕</button>
        </header>

        <div class="modal-body">
            {#if isLoading}
                <div class="loader-wrap"><span class="spinner"></span></div>
            {:else}
                <div class="tiles-grid">
                    <!-- ПЛИТКА: ФИЛИАЛ (НОВОЕ) -->
                    <div class="input-tile accent">
                        <label>Привязка к филиалу</label>
                        <select bind:value={formData.branchId}>
                            <option value="">Выберите филиал...</option>
                            {#each branches as b}
                                <option value={b.id}>{b.name}</option>
                            {/each}
                        </select>
                    </div>

                    <div class="input-tile">
                        <label>Название ресурса</label>
                        <input type="text" bind:value={formData.name} placeholder="Напр: Кабинет №5" autofocus />
                    </div>

                    <div class="input-tile full">
                        <label>Описание и заметки</label>
                        <textarea bind:value={formData.description} rows="4" placeholder="Дополнительная информация..."></textarea>
                    </div>
                </div>
            {/if}
        </div>

        <footer class="modal-footer">
            <button class="btn-cancel" on:click={() => dispatch('cancel')}>ОТМЕНА</button>
            <button class="btn-save" on:click={handleSave} disabled={isSaving || isLoading}>
                {isSaving ? '...' : 'СОХРАНИТЬ'}
            </button>
        </footer>
    </div>
</div>

<style>
    .modal-backdrop {
        position: fixed; inset: 0; background: rgba(15, 23, 42, 0.6);
        backdrop-filter: blur(8px); z-index: 99999;
        display: flex; align-items: center; justify-content: center;
        padding: 20px;
        padding-top: max(20px, calc(env(safe-area-inset-top, 20px) + 12px));
        padding-bottom: max(20px, calc(env(safe-area-inset-bottom, 20px) + 12px));
        box-sizing: border-box;
    }

    .modal-content {
        background: #f8fafc; width: 100%; max-width: 500px;
        border-radius: 32px; overflow: hidden;
        box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
        display: flex; flex-direction: column;
        max-height: calc(100dvh - max(40px, env(safe-area-inset-top, 20px) + env(safe-area-inset-bottom, 20px)) - 40px);
    }

    .modal-header {
        background: white; padding: 24px 32px; display: flex; align-items: center; gap: 20px;
        border-bottom: 1px solid #f1f5f9;
    }
    .header-icon { width: 56px; height: 56px; background: #eff6ff; border-radius: 18px; display: flex; align-items: center; justify-content: center; font-size: 24px; }
    .header-text h3 { margin: 0; font-size: 18px; font-weight: 800; color: #0f172a; }
    .header-text p { margin: 2px 0 0 0; font-size: 13px; color: #94a3b8; font-weight: 600; }
    .close-btn { margin-left: auto; background: #f1f5f9; border: none; width: 32px; height: 32px; border-radius: 50%; cursor: pointer; color: #64748b; font-weight: bold; }

    .modal-body { padding: 32px; flex: 1; overflow-y: auto; }
    .tiles-grid { display: flex; flex-direction: column; gap: 16px; }

    .input-tile {
        background: white; padding: 16px 20px; border-radius: 20px;
        border: 1px solid #f1f5f9; box-shadow: 0 4px 15px rgba(0,0,0,0.02);
    }
    .input-tile.accent { border-left: 4px solid var(--primary-color); background: #f0f9ff; }

    label { display: block; font-size: 10px; font-weight: 800; color: #94a3b8; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 8px; }

    input, textarea, select {
        width: 100%; border: none; background: none; font-size: 16px; font-weight: 700; color: #1e293b; outline: none; padding: 0;
    }
    select { cursor: pointer; color: var(--primary-color); }
    textarea { resize: none; line-height: 1.5; font-weight: 500; color: #64748b; }

    .modal-footer {
        padding: 24px 32px; background: white; border-top: 1px solid #f1f5f9;
        display: grid; grid-template-columns: 1fr 2fr; gap: 16px;
    }
    .btn-cancel { background: white; color: #64748b; border: 1.5px solid #e2e8f0; padding: 16px; border-radius: 16px; font-weight: 700; cursor: pointer; }
    .btn-save { background: var(--primary-gradient); color: white; border: none; padding: 16px; border-radius: 16px; font-weight: 800; cursor: pointer; box-shadow: 0 10px 20px rgba(56, 151, 240, 0.2); }

    .loader-wrap { display: flex; justify-content: center; padding: 40px; }
    .spinner { width: 32px; height: 32px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }

    @media (max-width: 640px) {
        .modal-backdrop { 
            padding: 20px;
            padding-top: max(20px, env(safe-area-inset-top, 20px));
            padding-bottom: max(20px, env(safe-area-inset-bottom, 20px));
            align-items: center;
            justify-content: center;
        }
        .modal-content { 
            max-height: calc(100vh - max(40px, env(safe-area-inset-top, 20px) + env(safe-area-inset-bottom, 20px)));
            width: 95%;
            max-width: 400px;
            border-radius: 32px;
        }
    }
</style>
