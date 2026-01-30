<script>
    import { onMount } from 'svelte';
    import { staffService } from '$lib/services/staffService.js';
    import { staffSearchQuery, cachedStaff, staffMetadata } from '$lib/stores/ui.js';
    import { goto } from '$app/navigation';

    let isLoading = false;
    let tg = null;

    // Реактивный поиск
    $: if ($staffSearchQuery !== undefined) {
        handleSearch();
    }

    async function handleSearch() {
        const query = $staffSearchQuery;
        const isPhone = /^\d+$/.test(query);
        const shouldSearch = query.length === 0 ||
                           (isPhone && query.length >= 6) ||
                           (!isPhone && query.length >= 2);

        if (shouldSearch) {
            await loadStaff(0, false);
        }
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
            const result = await staffService.getStaff($staffSearchQuery, page, 100);
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

    async function handleDelete(id, name) {
        if (confirm(`Удалить сотрудника ${name}?`)) {
            try {
                await staffService.deleteStaffMember(id);
                await loadStaff($staffMetadata.currentPage, false);
            } catch (e) {
                alert('Ошибка при удалении');
            }
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
        <p class="subtitle">Всего: {$staffMetadata.totalElements || 0} мастеров</p>
    </div>

    <!-- ПОИСК: Сделан более контрастным -->
    <div class="search-container">
        <div class="search-box">
            <span class="search-icon">🔍</span>
            <input
                type="text"
                bind:value={$staffSearchQuery}
                placeholder="Поиск по имени или телефону..."
            />
            {#if $staffSearchQuery}
                <button class="clear-btn" on:click={() => $staffSearchQuery = ''}>✕</button>
            {/if}
        </div>
    </div>

    <div class="content">
        {#if $cachedStaff.length === 0 && !isLoading}
            <div class="empty-state">
                <p>{$staffSearchQuery ? 'Ничего не найдено' : 'Список пуст'}</p>
            </div>
        {:else}
            <div class="staff-list" class:updating={isLoading}>
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
                            <button class="delete-btn" on:click={() => handleDelete(member.id, member.name)}>🗑</button>
                        </div>
                    </div>
                {/each}
            </div>

            {#if $staffMetadata.totalPages > 1}
                <div class="pagination">
                    <button disabled={$staffMetadata.currentPage === 0} on:click={() => loadStaff($staffMetadata.currentPage - 1, false)}>←</button>
                    <span>{$staffMetadata.currentPage + 1} / {$staffMetadata.totalPages}</span>
                    <button disabled={$staffMetadata.currentPage >= $staffMetadata.totalPages - 1} on:click={() => loadStaff($staffMetadata.currentPage + 1, false)}>→</button>
                </div>
            {/if}
        {/if}
    </div>

    <button class="fab" on:click={() => goto('/admin/staff/new')}>+</button>
</div>

<style>
    .page { padding: 20px; max-width: 600px; margin: 0 auto; min-height: 100vh; background: var(--bg-color); }

    .header { margin-bottom: 20px; }
    .title-row { display: flex; align-items: center; gap: 12px; }
    h1 { font-size: 26px; font-weight: 800; margin: 0; color: #0f172a; }
    .subtitle { color: var(--hint-color); margin: 4px 0 0 0; font-size: 14px; font-weight: 500; }

    /* УЛУЧШЕННЫЙ ПОИСК */
    .search-container { margin-bottom: 24px; position: sticky; top: 10px; z-index: 10; }
    .search-box {
        display: flex; align-items: center; background: white; padding: 14px 18px;
        border-radius: 18px;
        box-shadow: 0 10px 25px rgba(0,0,0,0.05);
        border: 2px solid #3897f033; /* Тонкая синяя рамка */
    }
    .search-icon { margin-right: 12px; font-size: 18px; }
    input { border: none; background: none; width: 100%; font-size: 16px; outline: none; color: #1e293b; font-weight: 500; }
    .clear-btn { background: #f1f5f9; border: none; color: #64748b; border-radius: 50%; width: 24px; height: 24px; cursor: pointer; font-size: 12px; }

    .staff-list { display: grid; gap: 12px; padding-bottom: 100px; }
    .staff-card { display: flex; align-items: center; gap: 16px; padding: 16px; background: white; border-radius: 22px; box-shadow: var(--shadow); }
    .avatar { width: 52px; height: 52px; background: #eff6ff; color: var(--primary-color); border-radius: 16px; display: flex; justify-content: center; align-items: center; font-weight: 800; font-size: 20px; }

    .info { flex: 1; }
    h3 { margin: 0; font-size: 17px; color: #1e293b; font-weight: 700; }
    .info p { margin: 2px 0 0 0; font-size: 13px; color: var(--hint-color); }
    .phone { font-size: 12px; color: var(--primary-color); font-weight: 600; margin-top: 4px; display: block; }

    .actions { display: flex; gap: 10px; }
    .actions button { width: 40px; height: 40px; border-radius: 12px; border: none; cursor: pointer; font-size: 18px; transition: transform 0.1s; }
    .actions button:active { transform: scale(0.9); }
    .edit-btn { background: #eff6ff; color: var(--primary-color); }
    .delete-btn { background: #fef2f2; color: #ef4444; }

    .mini-spinner { width: 20px; height: 20px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }

    .fab { position: fixed; bottom: 90px; right: 20px; width: 60px; height: 60px; background: var(--primary-gradient); color: white; border: none; border-radius: 20px; font-size: 32px; box-shadow: 0 12px 30px rgba(56, 151, 240, 0.4); z-index: 100; cursor: pointer; }
</style>
