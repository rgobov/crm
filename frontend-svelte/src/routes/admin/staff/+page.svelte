<script>
    import { onMount } from 'svelte';
    import { staffService } from '$lib/services/staffService.js';
    import { staffSearchQuery, cachedStaff, staffMetadata } from '$lib/stores/ui.js';
    import { goto } from '$app/navigation';
    import { fade, scale } from 'svelte/transition';
    import StaffDetailModal from '$lib/components/staff/StaffDetailModal.svelte';

    let isLoading = false;
    let debounceTimer;
    let selectedStaffId = null;
    let refreshKey = 0;

    // --- ГЛОБАЛЬНЫЙ ПОИСК ---
    $: if ($staffSearchQuery !== undefined) {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => {
            loadStaffData(0, $staffSearchQuery);
        }, 600);
    }

    onMount(async () => {
        if (window.Telegram && window.Telegram.WebApp) {
            window.Telegram.WebApp.BackButton.show();
            window.Telegram.WebApp.BackButton.onClick(() => goto('/admin'));
        }
        await loadStaffData(0, $staffSearchQuery);
    });

    async function loadStaffData(page = 0, query = '') {
        isLoading = true;
        try {
            const result = await staffService.getStaff(query, page, 25);
            cachedStaff.set(result.content);
            staffMetadata.set({
                totalElements: result.totalElements,
                totalPages: result.totalPages,
                currentPage: page
            });
        } catch (e) {
            console.error('API Load Error:', e);
        } finally {
            isLoading = false;
        }
    }

    function openStaffDetails(id) {
        selectedStaffId = id;
    }

    function handleUpdateSuccess() {
        refreshKey += 1;
        loadStaffData($staffMetadata.currentPage, $staffSearchQuery);
    }

    function closeStaffModal() {
        selectedStaffId = null;
    }
</script>

<div class="page">
    <div class="header">
        <div class="title-row">
            <h1>Персонал</h1>
            {#if isLoading}
                <span class="mini-spinner"></span>
            {/if}
        </div>
        <p class="subtitle">Всего в компании: {$staffMetadata.totalElements}</p>
    </div>

    <!-- ПОИСК -->
    <div class="search-container">
        <div class="search-box">
            <span class="search-icon">🔍</span>
            <input
                type="text"
                bind:value={$staffSearchQuery}
                placeholder="Имя или специальность..."
            />
            {#if $staffSearchQuery}
                <button class="clear-btn" on:click={() => $staffSearchQuery = ''}>✕</button>
            {/if}
        </div>
    </div>

    <div class="content">
        {#if $cachedStaff.length === 0 && !isLoading}
            <div class="empty-state">
                <p>Никого не нашли</p>
            </div>
        {:else}
            <div class="staff-grid-layout" class:dimmed={isLoading}>
                {#each $cachedStaff as member (member.id)}
                    <button class="staff-card-row" on:click={() => openStaffDetails(member.id)}>
                        <div class="avatar">{member.name.charAt(0)}</div>
                        <div class="info">
                            <span class="name">{member.name}</span>
                            <span class="spec">{member.specialty}</span>
                        </div>
                        <span class="arrow">›</span>
                    </button>
                {/each}
            </div>

            {#if $staffMetadata.totalPages > 1}
                <div class="pagination">
                    <button class="pag-btn" disabled={$staffMetadata.currentPage === 0} on:click={() => loadStaffData($staffMetadata.currentPage - 1, $staffSearchQuery)}>←</button>
                    <span>{$staffMetadata.currentPage + 1} / {$staffMetadata.totalPages}</span>
                    <button class="pag-btn" disabled={$staffMetadata.currentPage >= $staffMetadata.totalPages - 1} on:click={() => loadStaffData($staffMetadata.currentPage + 1, $staffSearchQuery)}>→</button>
                </div>
            {/if}
        {/if}
    </div>

    <button class="fab" on:click={() => goto('/admin/staff/new')}>+</button>

    <!-- МОДАЛЬНОЕ ОКНО ДЕТАЛЕЙ -->
    {#if selectedStaffId}
        <div class="modal-backdrop" on:click|self={closeStaffModal} transition:fade={{duration: 200}}>
            <div class="modal-content" transition:scale={{start: 0.95, duration: 200}}>
                <header class="modal-header">
                    <h2>Профиль мастера</h2>
                    <button class="close-x-btn" on:click={closeStaffModal}>✕</button>
                </header>
                <div class="modal-body-scroll">
                    {#key refreshKey}
                        <StaffDetailModal
                            staffId={selectedStaffId}
                            on:updated={handleUpdateSuccess}
                            on:close={closeStaffModal}
                        />
                    {/key}
                </div>
            </div>
        </div>
    {/if}
</div>

<style>
    .page { padding: 20px; max-width: 600px; margin: 0 auto; min-height: 100vh; background: #f8fafc; }
    h1 { font-size: 24px; font-weight: 900; margin: 0; color: #0f172a; }
    .subtitle { color: #94a3b8; font-size: 13px; font-weight: 600; margin-top: 4px; text-transform: uppercase; letter-spacing: 0.5px; }

    .search-container { margin: 20px 0; }
    .search-box { display: flex; align-items: center; background: white; padding: 12px 16px; border-radius: 16px; border: 1.5px solid #f1f5f9; box-shadow: 0 4px 15px rgba(0,0,0,0.02); }
    input { border: none; background: none; width: 100%; font-size: 15px; outline: none; margin-left: 10px; font-weight: 500; }
    .clear-btn { background: #f1f5f9; border: none; border-radius: 50%; width: 20px; height: 20px; font-size: 10px; }

    .staff-grid-layout { display: grid; gap: 10px; }
    .dimmed { opacity: 0.5; }

    .staff-card-row { display: grid; grid-template-columns: 48px 1fr 24px; gap: 16px; align-items: center; padding: 14px; background: white; border-radius: 20px; border: 1px solid #f1f5f9; cursor: pointer; text-align: left; transition: transform 0.1s; }
    .staff-card-row:active { transform: scale(0.98); }

    .avatar { width: 48px; height: 48px; background: #eff6ff; color: var(--primary-color); border-radius: 14px; display: flex; justify-content: center; align-items: center; font-weight: 800; font-size: 18px; }
    .info { display: flex; flex-direction: column; min-width: 0; }
    .name { font-size: 15px; font-weight: 700; color: #1e293b; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .spec { font-size: 12px; color: #94a3b8; font-weight: 600; }
    .arrow { color: #cbd5e1; font-size: 24px; }

    /* ФИКС МОДАЛКИ И КНОПКИ ЗАКРЫТИЯ */
    .modal-backdrop { position: fixed; inset: 0; background: rgba(15, 23, 42, 0.6); backdrop-filter: blur(8px); display: flex; align-items: center; justify-content: center; z-index: 2000; padding: 20px; }
    .modal-content { background: white; width: 100%; max-width: 480px; height: 85vh; border-radius: 32px; display: flex; flex-direction: column; overflow: hidden; box-shadow: 0 30px 60px -12px rgba(0, 0, 0, 0.4); }

    .modal-header { padding: 20px 24px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #f1f5f9; flex-shrink: 0; }
    .modal-header h2 { margin: 0; font-size: 17px; font-weight: 800; color: #0f172a; }

    .close-x-btn { background: #f1f5f9; border: none; width: 36px; height: 36px; border-radius: 50%; font-weight: 800; cursor: pointer; color: #64748b; font-size: 16px; display: flex; align-items: center; justify-content: center; transition: background 0.2s; }
    .close-x-btn:hover { background: #e2e8f0; }

    .modal-body-scroll { flex: 1; overflow-y: auto; background: #f8fafc; }

    .pagination { display: flex; justify-content: center; align-items: center; gap: 16px; margin-top: 32px; padding-bottom: 100px; }
    .pag-btn { background: white; border: 1.5px solid #f1f5f9; padding: 8px 16px; border-radius: 12px; font-weight: 700; color: #64748b; cursor: pointer; }

    .fab { position: fixed; bottom: 100px; right: 24px; width: 56px; height: 56px; background: var(--primary-gradient); color: white; border: none; border-radius: 18px; font-size: 32px; box-shadow: 0 10px 25px rgba(56, 151, 240, 0.4); cursor: pointer; z-index: 100; }
</style>
