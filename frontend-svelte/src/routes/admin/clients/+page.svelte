<script>
    import { onMount } from 'svelte';
    import { contactService } from '$lib/services/contactService.js';
    import { goto } from '$app/navigation';

    let clients = [];
    let searchQuery = '';
    let currentPage = 0;
    let totalPages = 0;
    let totalElements = 0;
    let isLoading = true;
    let isSearching = false;
    let tg = null;
    let debounceTimer;

    // "Умный" поиск (аналог Flutter): 3 буквы для имени, 6 цифр для телефона
    $: if (searchQuery !== undefined) {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => {
            const cleanDigits = searchQuery.replace(/\D/g, '');
            const shouldSearch = searchQuery.length === 0 ||
                               (cleanDigits.length >= 6) ||
                               (searchQuery.trim().length >= 3);
            if (shouldSearch) loadPage(0);
        }, 600);
    }

    onMount(async () => {
        if (window.Telegram && window.Telegram.WebApp) {
            tg = window.Telegram.WebApp;
            tg.BackButton.show();
            tg.BackButton.onClick(() => goto('/admin'));
        }
        await loadPage(0);
    });

    async function loadPage(page) {
        isLoading = true;
        try {
            const result = await contactService.getContacts(searchQuery, page, 25);
            clients = result.contacts;
            totalPages = Math.ceil(result.totalElements / 25);
            totalElements = result.totalElements;
            currentPage = page;
        } catch (e) {
            console.error('Load page failed', e);
        } finally {
            isLoading = false;
        }
    }
</script>

<div class="page">
    <div class="header">
        <div class="title-row">
            <h1>Клиенты</h1>
            {#if isLoading}
                <span class="mini-spinner"></span>
            {/if}
        </div>
        <p class="subtitle">Всего в базе: {totalElements} контактов</p>
    </div>

    <!-- КОНТРАСТНОЕ ОКНО ПОИСКА -->
    <div class="search-container">
        <div class="search-box">
            <span class="search-icon">🔍</span>
            <input
                type="text"
                bind:value={searchQuery}
                placeholder="Поиск (от 3 букв или 6 цифр телефона)..."
            />
            {#if searchQuery}
                <button class="clear-btn" on:click={() => searchQuery = ''}>✕</button>
            {/if}
        </div>
    </div>

    <div class="content">
        {#if clients.length === 0 && !isLoading}
            <div class="empty-state">
                <p>{searchQuery ? 'Клиенты не найдены' : 'Список пуст'}</p>
            </div>
        {:else}
            <div class="client-list" class:dimmed={isLoading}>
                {#each clients as client (client.id)}
                    <div class="client-card card" on:click={() => goto(`/admin/clients/${client.id}`)}>
                        <div class="avatar">{client.name.charAt(0).toUpperCase()}</div>
                        <div class="info">
                            <h3>{client.name}</h3>
                            <p>{client.phones?.[0] || 'Нет телефона'}</p>
                        </div>
                        <span class="chevron">›</span>
                    </div>
                {/each}
            </div>

            <!-- ПАГИНАЦИЯ: Кнопки переключения по 25 человек -->
            {#if totalPages > 1}
                <div class="pagination">
                    <button class="pag-btn" disabled={currentPage === 0} on:click={() => loadPage(currentPage - 1)}>
                        ← Назад
                    </button>
                    <span class="page-info">
                        Страница <strong>{currentPage + 1}</strong> из {totalPages}
                    </span>
                    <button class="pag-btn" disabled={currentPage >= totalPages - 1} on:click={() => loadPage(currentPage + 1)}>
                        Далее →
                    </button>
                </div>
            {/if}
        {/if}
    </div>

    <button class="fab" on:click={() => goto('/admin/clients/new')}>+</button>
</div>

<style>
    .page { padding: 20px; max-width: 600px; margin: 0 auto; background: var(--bg-color); min-height: 100vh; }

    .header { margin-bottom: 20px; }
    .title-row { display: flex; align-items: center; gap: 12px; }
    h1 { font-size: 26px; font-weight: 800; margin: 0; color: #0f172a; }
    .subtitle { color: var(--hint-color); font-size: 14px; margin-top: 4px; }

    /* Поиск: Сделан максимально заметным */
    .search-container { margin-bottom: 24px; position: sticky; top: 10px; z-index: 10; }
    .search-box {
        display: flex; align-items: center; background: white; padding: 14px 18px;
        border-radius: 18px; border: 2px solid #3897f033;
        box-shadow: 0 8px 20px rgba(0,0,0,0.04);
    }
    input { border: none; background: none; width: 100%; font-size: 16px; outline: none; margin-left: 10px; font-weight: 500; }
    .clear-btn { background: #f1f5f9; border: none; color: #64748b; border-radius: 50%; width: 24px; height: 24px; cursor: pointer; }

    .client-list { display: grid; gap: 10px; transition: opacity 0.2s; }
    .dimmed { opacity: 0.6; pointer-events: none; }

    .client-card {
        display: flex; align-items: center; gap: 16px; padding: 16px;
        background: white; border-radius: 22px; cursor: pointer;
        box-shadow: var(--shadow); border: 1px solid rgba(0,0,0,0.01);
    }

    .avatar {
        width: 50px; height: 50px;
        background: #f1f5f9; color: var(--primary-color);
        border-radius: 16px; display: flex; justify-content: center; align-items: center;
        font-weight: 800; font-size: 18px;
    }

    .info { flex: 1; }
    h3 { margin: 0; font-size: 16px; color: #1e293b; font-weight: 700; }
    .info p { margin: 2px 0 0 0; font-size: 13px; color: var(--hint-color); }
    .chevron { font-size: 20px; color: #cbd5e1; }

    /* Пагинация: Понятные кнопки */
    .pagination {
        display: flex; justify-content: center; align-items: center;
        gap: 12px; margin-top: 32px; padding-bottom: 100px;
    }
    .pag-btn {
        padding: 10px 16px; border-radius: 12px; border: 1px solid #e2e8f0;
        background: white; color: #1e293b; font-weight: 600; font-size: 13px;
        cursor: pointer; transition: all 0.2s;
    }
    .pag-btn:disabled { opacity: 0.4; cursor: not-allowed; }
    .page-info { font-size: 13px; color: #64748b; }

    .mini-spinner { width: 18px; height: 18px; border: 2px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }

    .fab {
        position: fixed; bottom: 90px; right: 20px;
        width: 60px; height: 60px;
        background: var(--primary-gradient); color: white;
        border: none; border-radius: 20px; font-size: 32px;
        box-shadow: 0 12px 30px rgba(56, 151, 240, 0.4);
    }
</style>
