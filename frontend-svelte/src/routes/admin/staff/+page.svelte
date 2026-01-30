<script>
    import { onMount } from 'svelte';
    import { staffService } from '$lib/services/staffService.js';
    import { goto } from '$app/navigation';

    let staff = [];
    let searchQuery = '';
    let currentPage = 0;
    let totalPages = 0;
    let totalElements = 0;
    let isLoading = true;
    let tg = null;

    // Реактивная логика поиска: отправляем запрос на бэкенд только при выполнении условий
    $: if (searchQuery !== undefined) {
        handleSearch();
    }

    async function handleSearch() {
        // Условия поиска: 2 буквы для текста или 6 цифр для телефона
        const isPhone = /^\d+$/.test(searchQuery);
        const shouldSearch = searchQuery.length === 0 ||
                           (isPhone && searchQuery.length >= 6) ||
                           (!isPhone && searchQuery.length >= 2);

        if (shouldSearch) {
            currentPage = 0; // Сбрасываем на первую страницу при новом поиске
            await loadStaff();
        }
    }

    onMount(async () => {
        if (window.Telegram && window.Telegram.WebApp) {
            tg = window.Telegram.WebApp;
            tg.BackButton.show();
            tg.BackButton.onClick(() => goto('/admin'));
        }
        await loadStaff();
    });

    async function loadStaff(page = 0) {
        isLoading = true;
        try {
            const result = await staffService.getStaff(searchQuery, page, 100);
            staff = result.content;
            totalPages = result.totalPages;
            totalElements = result.totalElements;
            currentPage = page;
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
                await loadStaff(currentPage);
            } catch (e) {
                alert('Ошибка при удалении');
            }
        }
    }
</script>

<div class="page">
    <div class="header">
        <h1>Персонал</h1>
        <p>Всего сотрудников: {totalElements}</p>
    </div>

    <div class="search-container">
        <div class="search-box">
            <span class="search-icon">🔍</span>
            <input
                type="text"
                bind:value={searchQuery}
                placeholder="Имя (от 2 букв) или телефон (от 6 цифр)..."
            />
            {#if searchQuery}
                <button class="clear-btn" on:click={() => searchQuery = ''}>✕</button>
            {/if}
        </div>
    </div>

    {#if isLoading && staff.length === 0}
        <div class="center"><span class="spinner"></span></div>
    {:else}
        <div class="staff-list">
            {#each staff as member}
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

        <!-- Пагинация -->
        {#if totalPages > 1}
            <div class="pagination">
                <button disabled={currentPage === 0} on:click={() => loadStaff(currentPage - 1)}>Назад</button>
                <span>Страница {currentPage + 1} из {totalPages}</span>
                <button disabled={currentPage >= totalPages - 1} on:click={() => loadStaff(currentPage + 1)}>Вперед</button>
            </div>
        {/if}

        <button class="fab" on:click={() => goto('/admin/staff/new')}>+</button>
    {/if}
</div>

<style>
    .page { padding: 20px; max-width: 600px; margin: 0 auto; }
    .header h1 { font-size: 24px; font-weight: 800; margin: 0; color: #0f172a; }
    .header p { color: var(--hint-color); margin: 4px 0 20px 0; font-size: 13px; }

    .search-box {
        display: flex; align-items: center; background: white; padding: 12px 16px;
        border-radius: 16px; box-shadow: var(--shadow); margin-bottom: 24px;
    }
    .search-icon { margin-right: 12px; color: #94a3b8; }
    input { border: none; background: none; width: 100%; font-size: 15px; outline: none; }
    .clear-btn { background: none; border: none; color: #cbd5e1; cursor: pointer; }

    .staff-list { display: grid; gap: 12px; }
    .staff-card { display: flex; align-items: center; gap: 16px; padding: 16px; background: white; border-radius: 20px; }
    .avatar { width: 48px; height: 48px; background: #f1f5f9; color: var(--primary-color); border-radius: 14px; display: flex; justify-content: center; align-items: center; font-weight: 800; }
    .info { flex: 1; }
    .info h3 { margin: 0; font-size: 16px; color: #1e293b; }
    .info p { margin: 2px 0 0 0; font-size: 13px; color: var(--hint-color); }
    .phone { font-size: 11px; color: var(--primary-color); font-weight: 600; }

    .actions { display: flex; gap: 8px; }
    .actions button { width: 36px; height: 36px; border-radius: 10px; border: none; cursor: pointer; }
    .edit-btn { background: #eff6ff; color: var(--primary-color); }
    .delete-btn { background: #fef2f2; color: #ef4444; }

    .pagination { display: flex; justify-content: center; align-items: center; gap: 16px; margin-top: 32px; padding-bottom: 40px; }
    .pagination button { padding: 8px 16px; border-radius: 10px; border: 1px solid #e2e8f0; background: white; cursor: pointer; }
    .pagination button:disabled { opacity: 0.5; cursor: not-allowed; }

    .fab { position: fixed; bottom: 90px; right: 20px; width: 56px; height: 56px; background: var(--primary-gradient); color: white; border: none; border-radius: 18px; font-size: 28px; box-shadow: 0 10px 25px rgba(56, 151, 240, 0.4); z-index: 100; cursor: pointer; }
    .center { display: flex; justify-content: center; padding: 40px; }
    .spinner { width: 30px; height: 30px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
</style>
