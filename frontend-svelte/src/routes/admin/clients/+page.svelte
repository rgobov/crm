<script>
    import { onMount, onDestroy } from 'svelte';
    import { contactService } from '$lib/services/contactService.js';
    import { goto } from '$app/navigation';

    let clients = [];
    let searchQuery = '';
    let currentPage = 0;
    let isLastPage = false;
    let isLoading = false;
    let isInitialLoading = true;
    let debounceTimer;
    let scrollContainer;

    // Синхронизация поиска с логикой Flutter (3 буквы или 6 цифр)
    $: if (searchQuery !== undefined) {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => {
            handleSearch();
        }, 600);
    }

    async function handleSearch() {
        const cleanDigits = searchQuery.replace(/\D/g, '');
        const shouldSearch = searchQuery.length === 0 ||
                           (cleanDigits.length >= 6) ||
                           (searchQuery.trim().length >= 3);

        if (shouldSearch) {
            await loadInitial();
        }
    }

    onMount(async () => {
        if (window.Telegram && window.Telegram.WebApp) {
            window.Telegram.WebApp.BackButton.show();
            window.Telegram.WebApp.BackButton.onClick(() => goto('/admin'));
        }
        await loadInitial();
    });

    async function loadInitial() {
        isInitialLoading = true;
        currentPage = 0;
        try {
            const result = await contactService.getContacts(searchQuery, 0, 25);
            clients = result.contacts;
            isLastPage = result.isLast;
        } catch (e) {
            console.error('Search failed');
        } finally {
            isInitialLoading = false;
        }
    }

    async function loadMore() {
        if (isLoading || isLastPage) return;
        isLoading = true;
        try {
            const nextPage = currentPage + 1;
            const result = await contactService.getContacts(searchQuery, nextPage, 25);

            // Добавляем только уникальных (как во Flutter)
            const newOnes = result.contacts.filter(c => !clients.some(ex => ex.id === c.id));
            clients = [...clients, ...newOnes];
            isLastPage = result.isLast;
            currentPage = nextPage;
        } catch (e) {
            console.error('Load more failed');
        } finally {
            isLoading = false;
        }
    }

    // Обработка бесконечной прокрутки
    function handleScroll(e) {
        const { scrollTop, scrollHeight, clientHeight } = e.target;
        if (scrollHeight - scrollTop - clientHeight < 200) {
            loadMore();
        }
    }
</script>

<div class="page" on:scroll={handleScroll} bind:this={scrollContainer}>
    <div class="header">
        <h1>Клиенты</h1>
        <p>База контактов</p>
    </div>

    <div class="search-container">
        <div class="search-box">
            <span class="search-icon">💎</span>
            <input
                type="text"
                bind:value={searchQuery}
                placeholder="Имя или +7 (___) ..."
            />
            {#if searchQuery}
                <button class="clear-btn" on:click={() => searchQuery = ''}>✕</button>
            {/if}
        </div>
    </div>

    <div class="client-list">
        {#if isInitialLoading}
            <div class="center"><span class="spinner"></span></div>
        {:else if clients.length === 0}
            <div class="empty-state">
                <div class="icon">🔍</div>
                <p>Клиенты не найдены</p>
            </div>
        {:else}
            {#each clients as client (client.id)}
                <div class="client-card card" on:click={() => goto(`/admin/clients/${client.id}`)}>
                    <div class="avatar">{client.name.charAt(0).toUpperCase()}</div>
                    <div class="info">
                        <h3>{client.name}</h3>
                        <p>{client.phones?.[0] || 'Нет телефона'}</p>
                    </div>
                    <span class="arrow">›</span>
                </div>
            {/each}

            {#if isLoading}
                <div class="load-more-spinner">
                    <span class="spinner mini"></span>
                </div>
            {/if}

            {#if isLastPage && clients.length > 0}
                <p class="end-message">Всего клиентов: {clients.length}</p>
            {/if}
        {/if}
    </div>

    <button class="fab" on:click={() => goto('/admin/clients/new')}>+</button>
</div>

<style>
    .page { padding: 20px; max-width: 600px; margin: 0 auto; height: 100vh; overflow-y: auto; background: var(--bg-color); }

    .header h1 { font-size: 24px; font-weight: 800; margin: 0; color: #0f172a; }
    .header p { color: var(--hint-color); margin: 4px 0 24px 0; font-size: 14px; }

    .search-box {
        display: flex; align-items: center; background: white; padding: 14px 18px;
        border-radius: 18px; box-shadow: var(--shadow); margin-bottom: 24px;
    }
    input { border: none; background: none; width: 100%; font-size: 16px; outline: none; margin-left: 10px; }

    .client-list { display: grid; gap: 10px; padding-bottom: 100px; }

    .client-card {
        display: flex; align-items: center; gap: 16px; padding: 16px;
        background: white; border-radius: 20px; cursor: pointer;
    }

    .avatar {
        width: 48px; height: 48px;
        background: #f1f5f9; color: var(--primary-color);
        border-radius: 14px; display: flex; justify-content: center; align-items: center;
        font-weight: 800; font-size: 18px;
    }

    .info flex: 1;
    h3 { margin: 0; font-size: 16px; color: #1e293b; font-weight: 700; }
    .info p { margin: 2px 0 0 0; font-size: 13px; color: var(--hint-color); }

    .end-message { text-align: center; font-size: 12px; color: #94a3b8; margin-top: 20px; font-weight: 600; }

    .fab {
        position: fixed; bottom: 90px; right: 20px;
        width: 56px; height: 56px;
        background: var(--primary-gradient); color: white;
        border: none; border-radius: 18px; font-size: 28px;
        box-shadow: 0 10px 25px rgba(56, 151, 240, 0.4);
    }

    .spinner { width: 30px; height: 30px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; display: inline-block; }
    .spinner.mini { width: 20px; height: 20px; }
    @keyframes spin { to { transform: rotate(360deg); } }
    .center { text-align: center; padding: 40px; }
</style>
