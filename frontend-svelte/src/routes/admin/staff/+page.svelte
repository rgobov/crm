<script>
    import { onMount } from 'svelte';
    import { staffService } from '$lib/services/staffService.js';
    import { staffSearchQuery, cachedStaff, staffMetadata } from '$lib/stores/ui.js';
    import { goto } from '$app/navigation';

    let isLoading = false;
    let tg = null;

    // Реактивная логика поиска (без перезагрузки страницы)
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
            // При поиске сбрасываем страницу, но не очищаем список для бесшовности
            await loadStaff(0, false);
        }
    }

    onMount(async () => {
        if (window.Telegram && window.Telegram.WebApp) {
            tg = window.Telegram.WebApp;
            tg.BackButton.show();
            tg.BackButton.onClick(() => goto('/admin'));
        }
        // Загружаем данные. Если в кэше уже есть что-то, пользователь увидит это сразу.
        await loadStaff(0, $cachedStaff.length === 0);
    });

    async function loadStaff(page = 0, showSpinner = true) {
        if (showSpinner) isLoading = true;
        try {
            const result = await staffService.getStaff($staffSearchQuery, page, 100);

            // Сохраняем в глобальное хранилище
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
        <p>Всего сотрудников: {$staffMetadata.totalElements || 0}</p>
    </div>

    <!-- ПОИСК: Всегда виден и стабилен -->
    <div class="search-container">
        <div class="search-box">
            <span class="search-icon">🔍</span>
            <input
                type="text"
                bind:value={$staffSearchQuery}
                placeholder="Поиск (от 2 букв или 6 цифр)..."
            />
            {#if $staffSearchQuery}
                <button class="clear-btn" on:click={() => $staffSearchQuery = ''}>✕</button>
            {/if}
        </div>
    </div>

    <div class="content">
        {#if $cachedStaff.length === 0 && !isLoading}
            <div class="empty-state">
                <p>{$staffSearchQuery ? 'Ничего не найдено' : 'Список сотрудников пуст'}</p>
                {#if !$staffSearchQuery}
                    <button class="add-btn" on:click={() => goto('/admin/staff/new')}>Добавить первого</button>
                {/if}
            </div>
        {:else}
            <!-- Список берется из кэша, поэтому появляется мгновенно -->
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
                    <button disabled={$staffMetadata.currentPage === 0} on:click={() => loadStaff($staffMetadata.currentPage - 1, false)}>Назад</button>
                    <span>{$staffMetadata.currentPage + 1} / {$staffMetadata.totalPages}</span>
                    <button disabled={$staffMetadata.currentPage >= $staffMetadata.totalPages - 1} on:click={() => loadStaff($staffMetadata.currentPage + 1, false)}>Вперед</button>
                </div>
            {/if}
        {/if}
    </div>

    <button class="fab" on:click={() => goto('/admin/staff/new')}>+</button>
</div>

<style>
    .page { padding: 20px; max-width: 600px; margin: 0 auto; display: flex; flex-direction: column; height: 100%; }

    .title-row { display: flex; align-items: center; gap: 12px; }
    .header h1 { font-size: 24px; font-weight: 800; margin: 0; color: #0f172a; }
    .header p { color: var(--hint-color); margin: 4px 0 20px 0; font-size: 13px; }

    .search-box {
        display: flex; align-items: center; background: white; padding: 12px 16px;
        border-radius: 16px; box-shadow: var(--shadow); margin-bottom: 24px;
    }
    .search-icon { margin-right: 12px; color: #94a3b8; }
    input { border: none; background: none; width: 100%; font-size: 15px; outline: none; }
    .clear-btn { background: none; border: none; color: #cbd5e1; cursor: pointer; padding: 4px; }

    .staff-list { display: grid; gap: 12px; transition: opacity 0.3s; }
    .updating { opacity: 0.7; }

    .staff-card { display: flex; align-items: center; gap: 16px; padding: 16px; background: white; border-radius: 20px; }
    .avatar { width: 48px; height: 48px; background: #f1f5f9; color: var(--primary-color); border-radius: 14px; display: flex; justify-content: center; align-items: center; font-weight: 800; }

    .info { flex: 1; }
    .info h3 { margin: 0; font-size: 16px; color: #1e293b; font-weight: 700; }
    .info p { margin: 2px 0 0 0; font-size: 13px; color: var(--hint-color); }
    .phone { font-size: 11px; color: var(--primary-color); font-weight: 600; margin-top: 4px; display: block; }

    .actions { display: flex; gap: 8px; }
    .actions button { width: 36px; height: 36px; border-radius: 10px; border: none; cursor: pointer; display: flex; justify-content: center; align-items: center; font-size: 16px; }
    .edit-btn { background: #eff6ff; color: var(--primary-color); }
    .delete-btn { background: #fef2f2; color: #ef4444; }

    .pagination { display: flex; justify-content: center; align-items: center; gap: 16px; margin-top: 24px; padding-bottom: 40px; }
    .pagination button { padding: 8px 16px; border-radius: 10px; border: 1px solid #e2e8f0; background: white; font-weight: 600; font-size: 13px; }

    .mini-spinner { width: 18px; height: 18px; border: 2px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }

    .fab { position: fixed; bottom: 90px; right: 20px; width: 56px; height: 56px; background: var(--primary-gradient); color: white; border: none; border-radius: 18px; font-size: 28px; box-shadow: 0 10px 25px rgba(56, 151, 240, 0.4); z-index: 100; cursor: pointer; }

    .empty-state { text-align: center; padding: 40px 20px; color: var(--hint-color); }
    .add-btn { background: var(--primary-gradient); color: white; border: none; padding: 12px 24px; border-radius: 14px; font-weight: 700; margin-top: 16px; cursor: pointer; }
</style>
