<script>
    import { onMount } from 'svelte';
    import { contactService } from '$lib/services/contactService.js';
    import { goto } from '$app/navigation';

    let clients = [];
    let searchQuery = '';
    let showAll = false; // Переключатель: false = Сегодня, true = Все
    let currentPage = 0;
    let totalPages = 0;
    let totalElements = 0;
    let isLoading = true;
    let debounceTimer;

    // Реактивная логика: при смене поиска или переключателя - грузим заново
    $: if (searchQuery !== undefined || showAll !== undefined) {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => {
            loadPage(0);
        }, 500);
    }

    onMount(async () => {
        if (window.Telegram && window.Telegram.WebApp) {
            window.Telegram.WebApp.BackButton.show();
            window.Telegram.WebApp.BackButton.onClick(() => goto('/admin'));
        }
        await loadPage(0);
    });

    async function loadPage(page) {
        isLoading = true;
        try {
            // Отправляем новый параметр showAll на бэкенд
            const result = await contactService.getContacts(searchQuery, showAll, page, 25);
            clients = result.contacts;
            totalPages = Math.ceil(result.totalElements / 25);
            totalElements = result.totalElements;
            currentPage = page;
        } catch (e) {
            console.error('Failed to load clients', e);
        } finally {
            isLoading = false;
        }
    }
</script>

<div class="page">
    <div class="header">
        <h1>Клиенты</h1>
        <div class="mode-toggle">
            <button class:active={!showAll} on:click={() => showAll = false}>Сегодня</button>
            <button class:active={showAll} on:click={() => showAll = true}>Все база</button>
        </div>
    </div>

    <!-- ПОИСК -->
    <div class="search-container">
        <div class="search-box">
            <span class="search-icon">🔍</span>
            <input
                type="text"
                bind:value={searchQuery}
                placeholder="Поиск по всей базе..."
            />
            {#if searchQuery}
                <button class="clear-btn" on:click={() => searchQuery = ''}>✕</button>
            {/if}
        </div>
    </div>

    <div class="content">
        <p class="stats">Найдено: {totalElements}</p>

        {#if clients.length === 0 && !isLoading}
            <div class="empty-state">
                <p>{searchQuery ? 'Ничего не найдено' : 'На сегодня записей нет'}</p>
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

            {#if totalPages > 1}
                <div class="pagination">
                    <button class="pag-btn" disabled={currentPage === 0} on:click={() => loadPage(currentPage - 1)}>←</button>
                    <span>{currentPage + 1} / {totalPages}</span>
                    <button class="pag-btn" disabled={currentPage >= totalPages - 1} on:click={() => loadPage(currentPage + 1)}>→</button>
                </div>
            {/if}
        {/if}
    </div>

    <button class="fab" on:click={() => goto('/admin/clients/new')}>+</button>
</div>

<style>
    .page { padding: 20px; max-width: 600px; margin: 0 auto; background: var(--bg-color); min-height: 100vh; }

    .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
    h1 { font-size: 24px; font-weight: 800; margin: 0; color: #0f172a; }

    /* Переключатель режимов */
    .mode-toggle { background: #f1f5f9; padding: 4px; border-radius: 12px; display: flex; gap: 4px; }
    .mode-toggle button { border: none; background: none; padding: 6px 12px; border-radius: 10px; font-size: 13px; font-weight: 700; color: #64748b; cursor: pointer; }
    .mode-toggle button.active { background: white; color: var(--primary-color); box-shadow: 0 2px 8px rgba(0,0,0,0.05); }

    .search-container { margin-bottom: 24px; position: sticky; top: 10px; z-index: 10; }
    .search-box { display: flex; align-items: center; background: white; padding: 14px 18px; border-radius: 18px; border: 2px solid #3897f033; box-shadow: var(--shadow); }
    input { border: none; background: none; width: 100%; font-size: 16px; outline: none; margin-left: 10px; }

    .stats { font-size: 12px; color: #94a3b8; font-weight: 600; margin-bottom: 12px; }

    .client-list { display: grid; gap: 10px; padding-bottom: 20px; }
    .dimmed { opacity: 0.5; }

    .client-card { display: flex; align-items: center; gap: 16px; padding: 16px; background: white; border-radius: 22px; cursor: pointer; }
    .avatar { width: 48px; height: 48px; background: #f1f5f9; color: var(--primary-color); border-radius: 14px; display: flex; justify-content: center; align-items: center; font-weight: 800; }
    .info { flex: 1; }
    h3 { margin: 0; font-size: 16px; color: #1e293b; font-weight: 700; }
    .info p { margin: 2px 0 0 0; font-size: 13px; color: var(--hint-color); }

    .pagination { display: flex; justify-content: center; align-items: center; gap: 16px; margin-top: 24px; padding-bottom: 100px; }
    .pag-btn { padding: 8px 16px; border-radius: 10px; border: 1px solid #e2e8f0; background: white; font-weight: 600; }

    .fab { position: fixed; bottom: 90px; right: 20px; width: 60px; height: 60px; background: var(--primary-gradient); color: white; border: none; border-radius: 20px; font-size: 32px; box-shadow: 0 12px 30px rgba(56, 151, 240, 0.4); }
</style>
