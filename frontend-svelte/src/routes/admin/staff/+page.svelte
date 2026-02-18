<script>
    import { onMount } from 'svelte';
    import { staffService } from '$lib/services/staffService.js';
    import { staffSearchQuery, cachedStaff, staffMetadata } from '$lib/stores/ui.js';
    import { goto } from '$app/navigation';
    import { fade, scale } from 'svelte/transition';
    import StaffDetailModal from '$lib/components/staff/StaffDetailModal.svelte';

    let isLoading = false;
    let isFirstLoad = true;
    let debounceTimer;
    let selectedStaffId = null;
    let refreshKey = 0;

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
        isFirstLoad = ($cachedStaff.length === 0);
        await loadStaffData(0, $staffSearchQuery);
        isFirstLoad = false;
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

<div class="screen-wrapper">
    <header class="sticky-header">
        <div class="header-inner">
            <div class="title-row">
                <h1>Персонал</h1>
                <span class="count-badge">{$staffMetadata.totalElements}</span>
                {#if isLoading && !isFirstLoad}
                    <div class="mini-status-loader"></div>
                {/if}
            </div>

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
        </div>
    </header>

    <div class="screen-content">
        <div class="container-inner">
            {#if isFirstLoad}
                <div class="initial-loader-box">
                    <span class="main-spinner"></span>
                    <p>Загрузка списка...</p>
                </div>
            {:else if $cachedStaff.length === 0 && !isLoading}
                <div class="empty-state" in:fade>
                    <p>Сотрудники не найдены</p>
                </div>
            {:else}
                <div class="tiles-grid" class:is-refreshing={isLoading}>
                    {#each $cachedStaff as member (member.id)}
                        <div class="staff-tile" on:click={() => openStaffDetails(member.id)} in:fade={{duration: 150}}>
                            <div class="avatar-box">
                                {#if member.photoUrl}
                                    <img src={member.photoUrl} alt={member.name} class="avatar-img" />
                                {:else}
                                    {member.name.charAt(0)}
                                {/if}
                            </div>
                            <div class="info">
                                <span class="name">{member.name}</span>
                                <span class="spec">{member.specialty || 'Специалист'}</span>
                            </div>
                            <span class="chevron">›</span>
                        </div>
                    {/each}
                </div>

                <!-- ИСПРАВЛЕННАЯ ПАГИНАЦИЯ -->
                {#if $staffMetadata.totalPages > 1}
                    <div class="pagination">
                        <button class="pag-btn"
                                disabled={$staffMetadata.currentPage === 0}
                                on:click={() => loadStaffData($staffMetadata.currentPage - 1, $staffSearchQuery)}>
                            ‹
                        </button>
                        <div class="pag-info">
                            <span class="current">{$staffMetadata.currentPage + 1}</span>
                            <span class="divider">из</span>
                            <span class="total">{$staffMetadata.totalPages}</span>
                        </div>
                        <button class="pag-btn"
                                disabled={$staffMetadata.currentPage >= $staffMetadata.totalPages - 1}
                                on:click={() => loadStaffData($staffMetadata.currentPage + 1, $staffSearchQuery)}>
                            ›
                        </button>
                    </div>
                {/if}

                <div class="bottom-spacer"></div>
            {/if}
        </div>
    </div>

    <button class="fab-btn" on:click={() => goto('/admin/staff/new')}>+</button>

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
    .screen-wrapper { height: 100vh; width: 100%; display: flex; flex-direction: column; background: #f8fafc; overflow: hidden; }
    .sticky-header { background: white; border-bottom: 1px solid #f1f5f9; z-index: 100; box-shadow: 0 4px 12px rgba(0,0,0,0.02); }
    .header-inner { max-width: 800px; margin: 0 auto; padding: 24px 32px 12px; }
    .title-row { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; min-height: 36px; }
    h1 { font-size: 28px; font-weight: 850; margin: 0; color: #0f172a; }
    .count-badge { background: #eff6ff; color: var(--primary-color); padding: 4px 12px; border-radius: 10px; font-size: 14px; font-weight: 800; }

    .mini-status-loader { width: 14px; height: 14px; border: 2px solid #eff6ff; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 0.8s linear infinite; }

    .search-box { display: flex; align-items: center; background: #f1f5f9; padding: 12px 16px; border-radius: 18px; }
    input { border: none; background: none; width: 100%; font-size: 15px; outline: none; font-weight: 600; color: #1e293b; margin-left: 10px; }

    .screen-content { flex: 1; overflow-y: auto; width: 100%; }
    .container-inner { max-width: 800px; margin: 0 auto; padding: 32px 32px; }

    .tiles-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; transition: opacity 0.2s; }
    .is-refreshing { opacity: 0.7; }

    .staff-tile { background: white; padding: 16px; border-radius: 24px; display: flex; align-items: center; gap: 16px; border: 1px solid #f1f5f9; cursor: pointer; transition: all 0.2s; }
    .avatar-box { width: 52px; height: 52px; background: #eff6ff; color: var(--primary-color); border-radius: 16px; display: flex; justify-content: center; align-items: center; font-weight: 800; font-size: 20px; overflow: hidden; }
    .avatar-img { width: 100%; height: 100%; object-fit: cover; }
    .info { flex: 1; min-width: 0; }
    .name { display: block; font-size: 16px; font-weight: 800; color: #1e293b; }
    .spec { display: block; font-size: 12px; color: #94a3b8; font-weight: 600; text-transform: uppercase; }

    /* КРАСИВАЯ ПАГИНАЦИЯ ПО ЦЕНТРУ */
    .pagination {
        display: flex;
        justify-content: center;
        align-items: center;
        gap: 20px;
        margin-top: 40px;
        padding: 20px 0;
    }
    .pag-btn {
        background: white;
        border: 1px solid #e2e8f0;
        width: 44px;
        height: 44px;
        border-radius: 14px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 20px;
        font-weight: 700;
        color: #64748b;
        cursor: pointer;
        transition: 0.2s;
        box-shadow: 0 4px 10px rgba(0,0,0,0.02);
    }
    .pag-btn:hover:not(:disabled) {
        border-color: var(--primary-color);
        color: var(--primary-color);
        transform: translateY(-2px);
        box-shadow: 0 6px 15px rgba(56, 151, 240, 0.1);
    }
    .pag-btn:disabled { opacity: 0.3; cursor: not-allowed; }

    .pag-info { display: flex; align-items: center; gap: 8px; font-weight: 800; }
    .pag-info .current { color: var(--primary-color); font-size: 18px; }
    .pag-info .divider { color: #cbd5e1; font-size: 12px; text-transform: uppercase; }
    .pag-info .total { color: #64748b; font-size: 18px; }

    .initial-loader-box { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 100px 0; }
    .main-spinner { width: 32px; height: 32px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; }
    .initial-loader-box p { margin-top: 16px; color: #94a3b8; font-weight: 700; }

    @keyframes spin { to { transform: rotate(360deg); } }

    .fab-btn { position: fixed; bottom: 40px; right: 40px; width: 64px; height: 64px; background: var(--primary-gradient); color: white; border: none; border-radius: 20px; font-size: 32px; box-shadow: 0 10px 25px rgba(56, 151, 240, 0.4); cursor: pointer; z-index: 100; }

    .modal-backdrop { position: fixed; inset: 0; background: rgba(15, 23, 42, 0.6); backdrop-filter: blur(8px); display: flex; align-items: center; justify-content: center; z-index: 2000; padding: 20px; }
    .modal-content { background: white; width: 100%; max-width: 480px; height: 85vh; border-radius: 32px; display: flex; flex-direction: column; overflow: hidden; box-shadow: 0 30px 60px -12px rgba(0, 0, 0, 0.4); }
    .modal-header { padding: 24px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #f1f5f9; flex-shrink: 0; }
    .modal-header h2 { margin: 0; font-size: 18px; font-weight: 800; color: #0f172a; }
    .close-x-btn { background: #f1f5f9; border: none; width: 36px; height: 36px; border-radius: 50%; cursor: pointer; color: #64748b; display: flex; align-items: center; justify-content: center; }
    .modal-body-scroll { flex: 1; overflow-y: auto; background: #f8fafc; }
</style>
