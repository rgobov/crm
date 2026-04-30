<script>
    import { onMount } from 'svelte';
    import { staffService } from '$lib/services/staffService.js';
    import { staffSearchQuery, cachedStaff, staffMetadata } from '$lib/stores/ui.js';
    import { goto } from '$app/navigation';
    import { fade, scale } from 'svelte/transition';
    import StaffDetailModal from '$lib/components/staff/StaffDetailModal.svelte';
    import AddStaffModal from '$lib/components/staff/AddStaffModal.svelte'; // ✅ ДОБАВИЛИ

    let isLoading = false;
    let isFirstLoad = true;
    let debounceTimer;
    let selectedStaffId = null;
    let showAddModal = false; // ✅ СОСТОЯНИЕ ДЛЯ НОВОГО ОКНА
    let refreshKey = 0;

    $: if ($staffSearchQuery !== undefined) {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => {
            loadStaffData(0, $staffSearchQuery);
        }, 600);
    }

    onMount(async () => {
        if (window.Telegram && window.Telegram.WebApp && window.Telegram.WebApp.BackButton) {
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

    function handleAddSuccess() {
        loadStaffData(0, ''); // Сбрасываем фильтры и грузим первую страницу
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
                <button class="add-header-btn" on:click={() => showAddModal = true}>+</button>
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

    <!-- ✅ МОДАЛКА СОЗДАНИЯ -->
    {#if showAddModal}
        <AddStaffModal
            on:close={() => showAddModal = false}
            on:added={handleAddSuccess}
        />
    {/if}

    {#if selectedStaffId}
        {#key refreshKey}
            <StaffDetailModal
                staffId={selectedStaffId}
                on:updated={handleUpdateSuccess}
                on:close={closeStaffModal}
            />
        {/key}
    {/if}
</div>

<style>
    .screen-wrapper { height: 100vh; width: 100%; display: flex; flex-direction: column; background: #fdf6e3; overflow: hidden; }
    .sticky-header { background: #eee8d5; border-bottom: 1.5px solid #ddd6c1; z-index: 100; box-shadow: 0 4px 20px rgba(0,0,0,0.05); }
    .header-inner { max-width: 800px; margin: 0 auto; padding: 24px 32px 12px; }
    .title-row { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; min-height: 36px; }
    h1 { font-size: 28px; font-weight: 850; margin: 0; color: #073642; }
    .count-badge { background: #fdf6e3; color: #268bd2; padding: 4px 12px; border-radius: 10px; font-size: 14px; font-weight: 800; border: 1px solid #ddd6c1; }

    .mini-status-loader { width: 14px; height: 14px; border: 2px solid #fdf6e3; border-top-color: #268bd2; border-radius: 50%; animation: spin 0.8s linear infinite; }

    .search-box { display: flex; align-items: center; background: #fdf6e3; padding: 12px 16px; border-radius: 18px; border: 1.5px solid #ddd6c1; }
    input { border: none; background: none; width: 100%; font-size: 15px; outline: none; font-weight: 600; color: #073642; margin-left: 10px; }
    input::placeholder { color: #93a1a1; }

    .screen-content { flex: 1; overflow-y: auto; width: 100%; }
    .container-inner { max-width: 800px; margin: 0 auto; padding: 32px 32px; }

    .tiles-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; transition: opacity 0.2s; }
    .is-refreshing { opacity: 0.7; }

    .staff-tile { background: #eee8d5; padding: 16px; border-radius: 24px; display: flex; align-items: center; gap: 16px; border: 1.5px solid #ddd6c1; cursor: pointer; transition: all 0.2s; }
    .staff-tile:hover { transform: translateY(-2px); border-color: #268bd2; background: white; }
    .avatar-box { width: 52px; height: 52px; background: #fdf6e3; color: #268bd2; border-radius: 16px; display: flex; justify-content: center; align-items: center; font-weight: 800; font-size: 20px; overflow: hidden; border: 1px solid #ddd6c1; }
    .avatar-img { width: 100%; height: 100%; object-fit: cover; }
    .info { flex: 1; min-width: 0; }
    .name { display: block; font-size: 16px; font-weight: 800; color: #073642; }
    .spec { display: block; font-size: 12px; color: #586e75; font-weight: 600; text-transform: uppercase; }

    .pagination { display: flex; justify-content: center; align-items: center; gap: 20px; margin-top: 40px; padding: 20px 0; }
    .pag-btn { background: #eee8d5; border: 1.5px solid #ddd6c1; width: 44px; height: 44px; border-radius: 14px; display: flex; align-items: center; justify-content: center; font-size: 20px; font-weight: 700; color: #586e75; cursor: pointer; transition: 0.2s; }
    .pag-btn:hover:not(:disabled) { border-color: #268bd2; color: #268bd2; transform: translateY(-2px); background: white; }
    .pag-btn:disabled { opacity: 0.3; cursor: not-allowed; }

    .pag-info { display: flex; align-items: center; gap: 8px; font-weight: 800; }
    .pag-info .current { color: #268bd2; font-size: 18px; }
    .pag-info .divider { color: #93a1a1; font-size: 12px; text-transform: uppercase; }
    .pag-info .total { color: #586e75; font-size: 18px; }

    .initial-loader-box { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 100px 0; }
    .main-spinner { width: 32px; height: 32px; border: 3px solid #eee8d5; border-top-color: #268bd2; border-radius: 50%; animation: spin 1s linear infinite; }
    .initial-loader-box p { margin-top: 16px; color: #586e75; font-weight: 700; }

    @keyframes spin { to { transform: rotate(360deg); } }

    .add-header-btn { margin-left: auto; width: 44px; height: 44px; background: linear-gradient(135deg, #268bd2 0%, #2aa198 100%); color: white; border: none; border-radius: 14px; font-size: 28px; font-weight: 300; line-height: 1; display: flex; align-items: center; justify-content: center; cursor: pointer; box-shadow: 0 4px 12px rgba(38, 139, 210, 0.3); transition: transform 0.15s, box-shadow 0.15s; flex-shrink: 0; }
    .add-header-btn:active { transform: scale(0.92); }

    .modal-backdrop { position: fixed; inset: 0; background: rgba(7, 54, 66, 0.6); backdrop-filter: blur(8px); display: flex; align-items: center; justify-content: center; z-index: 2000; padding: 20px; }
    .modal-content { background: #fdf6e3; width: 100%; max-width: 480px; height: 85vh; border-radius: 32px; display: flex; flex-direction: column; overflow: hidden; box-shadow: 0 30px 60px -12px rgba(0, 0, 0, 0.4); border: 1.5px solid #ddd6c1; }
    .modal-header { padding: 24px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1.5px solid #ddd6c1; background: #eee8d5; flex-shrink: 0; }
    .modal-header h2 { margin: 0; font-size: 18px; font-weight: 800; color: #073642; }
    .close-x-btn { background: #fdf6e3; border: 1px solid #ddd6c1; width: 36px; height: 36px; border-radius: 50%; cursor: pointer; color: #586e75; display: flex; align-items: center; justify-content: center; }
    .modal-body-scroll { flex: 1; overflow-y: auto; background: #fdf6e3; }
</style>
