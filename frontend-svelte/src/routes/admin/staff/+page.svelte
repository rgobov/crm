<script>
    import { onMount } from 'svelte';
    import { staffService } from '$lib/services/staffService.js';
    import { staffSearchQuery, cachedStaff, staffMetadata } from '$lib/stores/ui.js';
    import { goto } from '$app/navigation';

    let isLoading = false;
    let tg = null;
    let debounceTimer;

    // Реактивный поиск
    $: if ($staffSearchQuery !== undefined) {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => {
            handleSearch();
        }, 600);
    }

    async function handleSearch() {
        const query = $staffSearchQuery;
        const isPhone = /^\d+$/.test(query);
        const shouldSearch = query.length === 0 ||
                           (isPhone && query.length >= 6) ||
                           (!isPhone && query.length >= 2);
        if (shouldSearch) await loadStaff(0, false);
    }

    onMount(async () => {
        if (window.Telegram && window.Telegram.WebApp) {
            tg = window.Telegram.WebApp;
            tg.BackButton.show();
            tg.BackButton.onClick(() => goto('/admin'));
        }
        await loadStaff(0, $cachedStaff.length === 0);
    });

    async function loadStaff(page = 0, showSpinner = true) {
        if (showSpinner) isLoading = true;
        try {
            const result = await staffService.getStaff($staffSearchQuery, page, 25);
            cachedStaff.set(result.content);
            staffMetadata.set({
                totalElements: result.totalElements,
                totalPages: result.totalPages,
                currentPage: page
            });
        } catch (e) {
            console.error('Failed to load staff');
        } finally {
            isLoading = false;
        }
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
        <p class="subtitle">Всего: {$staffMetadata.totalElements || 0}</p>
    </div>

    <!-- ПОИСК: С ГАРАНТИРОВАННОЙ ВИДИМОСТЬЮ -->
    <div class="search-container" style="display: block !important;">
        <div class="search-box">
            <span class="search-icon">🔍</span>
            <input
                type="text"
                bind:value={$staffSearchQuery}
                placeholder="Поиск мастера..."
            />
            {#if $staffSearchQuery}
                <button class="clear-btn" on:click={() => $staffSearchQuery = ''}>✕</button>
            {/if}
        </div>
    </div>

    <div class="content">
        {#if $cachedStaff.length === 0 && !isLoading}
            <div class="empty-state">
                <p>Ничего не найдено</p>
            </div>
        {:else}
            <div class="staff-list">
                {#each $cachedStaff as member (member.id)}
                    <div class="staff-card card">
                        <div class="avatar">{member.name.charAt(0)}</div>
                        <div class="info">
                            <h3>{member.name}</h3>
                            <p>{member.specialty}</p>
                            {#if member.phone}
                                <span class="phone">📱 {member.phone}</span>
                            {/if}
                        </div>
                        <div class="actions">
                            <button class="edit-btn" on:click={() => goto(`/admin/staff/${member.id}`)}>✎</button>
                        </div>
                    </div>
                {/each}
            </div>

            {#if $staffMetadata.totalPages > 1}
                <div class="pagination">
                    <button disabled={$staffMetadata.currentPage === 0} on:click={() => loadStaff($staffMetadata.currentPage - 1, false)}>←</button>
                    <button disabled={$staffMetadata.currentPage >= $staffMetadata.totalPages - 1} on:click={() => loadStaff($staffMetadata.currentPage + 1, false)}>→</button>
                </div>
            {/if}
        {/if}
    </div>

    <button class="fab" on:click={() => goto('/admin/staff/new')}>+</button>
</div>

<style>
    .page { padding: 20px; max-width: 600px; margin: 0 auto; background: var(--bg-color); min-height: 100vh; }
    .header { margin-bottom: 20px; }
    h1 { font-size: 26px; font-weight: 800; margin: 0; color: #0f172a; }

    .search-container { margin-bottom: 24px; position: sticky; top: 10px; z-index: 100; }
    .search-box {
        display: flex; align-items: center; background: white; padding: 14px 18px;
        border-radius: 18px; border: 3px solid #3897f0; /* ТОЛСТАЯ РАМКА ДЛЯ ПРОВЕРКИ */
        box-shadow: 0 10px 30px rgba(0,0,0,0.1);
    }
    input { border: none; background: none; width: 100%; font-size: 16px; outline: none; }

    .staff-list { display: grid; gap: 12px; }
    .staff-card { display: flex; align-items: center; gap: 16px; padding: 16px; background: white; border-radius: 22px; box-shadow: var(--shadow); }
    .avatar { width: 52px; height: 52px; background: #eff6ff; color: var(--primary-color); border-radius: 16px; display: flex; justify-content: center; align-items: center; font-weight: 800; }
    .info { flex: 1; }
    h3 { margin: 0; font-size: 17px; color: #1e293b; font-weight: 700; }
    .actions button { width: 40px; height: 40px; border-radius: 12px; border: none; background: #eff6ff; color: var(--primary-color); cursor: pointer; }

    .mini-spinner { width: 20px; height: 20px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
    .fab { position: fixed; bottom: 90px; right: 20px; width: 60px; height: 60px; background: var(--primary-gradient); color: white; border: none; border-radius: 20px; font-size: 32px; box-shadow: 0 12px 30px rgba(56, 151, 240, 0.4); }
    .pagination { display: flex; justify-content: center; gap: 20px; margin-top: 20px; }
    .pagination button { padding: 10px 20px; border-radius: 10px; border: 1px solid #ddd; background: white; }
</style>
