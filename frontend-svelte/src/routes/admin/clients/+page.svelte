<script>
    import { onMount } from 'svelte';
    import { contactService } from '$lib/services/contactService.js';
    import { goto } from '$app/navigation';
    import { fade, scale } from 'svelte/transition';
    import AddContactModal from '$lib/components/admin/AddContactModal.svelte';
    import ContactDetailScreen from '$lib/components/contacts/ContactDetailScreen.svelte';

    let clients = [];
    let searchQuery = '';
    let lastQuery = '';
    let showAll = true;
    let currentPage = 0;
    let totalPages = 0;
    let totalElements = 0;
    let isLoading = true;
    let debounceTimer;

    let showAddModal = false;
    let selectedClientId = null;

    $: {
        const query = searchQuery.trim();
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => {
            if (query === '' || query.length >= 3 || query !== lastQuery) {
                lastQuery = query;
                loadPage(0);
            }
        }, 600);
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
            const result = await contactService.getContacts(searchQuery, showAll, page, 24);
            clients = result.content || [];
            totalPages = result.totalPages || 0;
            totalElements = result.totalElements || 0;
            currentPage = page;
        } catch (e) {
            console.error('Failed to load clients', e);
        } finally {
            isLoading = false;
        }
    }

    function handleAddSuccess() {
        showAddModal = false;
        loadPage(0);
    }

    function handleUpdateSuccess(event) {
        loadPage(currentPage);
    }

    function openDetails(id) { selectedClientId = id; }
    function closeDetails() { selectedClientId = null; }
</script>

<div class="screen-wrapper">
    <header class="sticky-header">
        <div class="header-inner">
            <div class="title-row">
                <button class="back-btn" on:click={() => goto('/admin')}>‹</button>
                <h1>База клиентов</h1>
                {#if isLoading && clients.length > 0}
                    <div class="mini-top-loader"></div>
                {/if}
            </div>

            <div class="filter-tabs">
                <button class="tab" class:active={!showAll} on:click={() => { showAll = false; currentPage = 0; loadPage(0); }}>
                    На сегодня
                </button>
                <button class="tab" class:active={showAll} on:click={() => { showAll = true; currentPage = 0; loadPage(0); }}>
                    Вся база
                </button>
            </div>

            <div class="search-bar-wrap">
                <div class="search-inner">
                    <span class="search-icon">🔍</span>
                    <input type="text" bind:value={searchQuery} placeholder="Имя или телефон..." />
                    {#if searchQuery}
                        <button class="btn-clear" on:click={() => searchQuery = ''}>✕</button>
                    {/if}
                </div>
            </div>
        </div>
    </header>

    <div class="screen-content">
        <div class="container-inner">
            <!-- СТАТИСТИКА (с фиксированной высотой, чтобы не дергалась) -->
            <div class="stats-area">
                {#if totalElements > 0 || !isLoading}
                    <span>Найдено: <b>{totalElements}</b></span>
                {/if}
            </div>

            {#if isLoading && clients.length === 0}
                <div class="clients-initial-loader"><span class="spinner"></span></div>
            {:else if clients.length === 0}
                <div class="empty-view" in:fade>
                    <p>{searchQuery ? 'Ничего не найдено' : 'В базе пока нет клиентов'}</p>
                </div>
            {:else}
                <div class="client-tiles-grid" class:is-loading={isLoading}>
                    {#each clients as client (client.id)}
                        <button class="client-tile" on:click={() => openDetails(client.id)} in:fade={{duration: 150}}>
                            <div class="avatar-box">{client.name.charAt(0).toUpperCase()}</div>
                            <div class="info-meta">
                                <div class="name-line">{client.name}</div>
                                <div class="phone-line">{client.phones?.[0] || 'нет номера'}</div>
                            </div>
                            <span class="chevron-icon">›</span>
                        </button>
                    {/each}
                </div>

                {#if totalPages > 1}
                    <div class="pager">
                        <button class="btn-p" disabled={currentPage === 0} on:click={() => loadPage(currentPage - 1)}>←</button>
                        <span class="p-text">{currentPage + 1} / {totalPages}</span>
                        <button class="btn-p" disabled={currentPage >= totalPages - 1} on:click={() => loadPage(currentPage + 1)}>→</button>
                    </div>
                {/if}

                <div class="bottom-spacer"></div>
            {/if}
        </div>
    </div>

    <button class="fab-circle" on:click={() => showAddModal = true}>+</button>

    {#if showAddModal}
        <AddContactModal on:close={() => showAddModal = false} on:success={handleAddSuccess} />
    {/if}

    <!-- МОДАЛЬНОЕ ОКНО (ВОССТАНОВЛЕННЫЕ ОРИГИНАЛЬНЫЕ КЛАССЫ) -->
    {#if selectedClientId}
        <div class="modal-backdrop" on:click|self={closeDetails} transition:fade={{duration: 200}}>
            <div class="modal-content" transition:scale={{start: 0.95, duration: 200}}>
                <header class="modal-header">
                    <h2>Карточка клиента</h2>
                    <button class="close-x" on:click={closeDetails}>✕</button>
                </header>
                <div class="modal-scroll-body">
                    <ContactDetailScreen contactId={selectedClientId} on:updated={handleUpdateSuccess} />
                </div>
            </div>
        </div>
    {/if}
</div>

<style>
    .screen-wrapper { height: 100vh; width: 100%; display: flex; flex-direction: column; background: #f8fafc; overflow: hidden; }
    .sticky-header { background: white; border-bottom: 1px solid #f1f5f9; z-index: 100; }
    .header-inner { max-width: 800px; margin: 0 auto; padding: 20px 24px 12px; }
    .title-row { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; }
    .back-btn { background: #f1f5f9; border: none; width: 36px; height: 36px; border-radius: 12px; font-size: 24px; cursor: pointer; color: var(--primary-color); }
    h1 { font-size: 22px; font-weight: 900; margin: 0; color: #0f172a; }

    .mini-top-loader { width: 14px; height: 14px; border: 2px solid #eff6ff; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 0.8s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }

    .filter-tabs { background: #f1f5f9; padding: 4px; border-radius: 14px; display: flex; gap: 4px; margin-bottom: 16px; }
    .tab { flex: 1; border: none; background: none; padding: 8px; border-radius: 10px; font-size: 13px; font-weight: 700; color: #64748b; cursor: pointer; }
    .tab.active { background: white; color: var(--primary-color); box-shadow: 0 4px 12px rgba(0,0,0,0.05); }

    .search-inner { display: flex; align-items: center; background: #f1f5f9; padding: 12px 16px; border-radius: 18px; }
    input { border: none; background: none; width: 100%; font-size: 15px; outline: none; font-weight: 600; margin-left: 8px; }

    .screen-content { flex: 1; overflow-y: auto; width: 100%; }
    .container-inner { max-width: 800px; margin: 0 auto; padding: 20px; }

    /* ФИКСИРОВАННАЯ ВЫСОТА ДЛЯ СТАТИСТИКИ */
    .stats-area { height: 24px; margin-bottom: 8px; font-size: 12px; color: #94a3b8; font-weight: 600; padding-left: 8px; display: flex; align-items: center; }

    .client-tiles-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 12px; transition: opacity 0.2s; }
    .is-loading { opacity: 0.7; }

    .client-tile { display: flex; align-items: center; gap: 16px; padding: 16px; background: white; border-radius: 24px; border: 1px solid #f1f5f9; cursor: pointer; text-align: left; box-shadow: 0 4px 12px rgba(0,0,0,0.02); }
    .avatar-box { width: 48px; height: 48px; background: #eff6ff; color: var(--primary-color); border-radius: 16px; display: flex; justify-content: center; align-items: center; font-weight: 800; font-size: 18px; }
    .name-line { font-size: 15px; font-weight: 800; color: #1e293b; }
    .phone-line { font-size: 13px; color: #94a3b8; font-weight: 600; }

    .fab-circle { position: fixed; bottom: 40px; right: 40px; width: 64px; height: 64px; background: var(--primary-gradient); color: white; border: none; border-radius: 20px; font-size: 32px; box-shadow: 0 10px 25px rgba(56, 151, 240, 0.4); cursor: pointer; z-index: 1000; }

    /* МОДАЛЬНОЕ ОКНО (ВОССТАНОВЛЕННЫЕ ОРИГИНАЛЬНЫЕ СТИЛИ) */
    .modal-backdrop { position: fixed; inset: 0; background: rgba(15, 23, 42, 0.6); backdrop-filter: blur(8px); display: flex; align-items: center; justify-content: center; z-index: 2000; padding: 20px; }
    .modal-content { background: white; width: 100%; max-width: 500px; height: 85vh; border-radius: 32px; display: flex; flex-direction: column; overflow: hidden; box-shadow: 0 30px 60px -12px rgba(0, 0, 0, 0.4); }
    .modal-header { padding: 24px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #f1f5f9; }
    .modal-header h2 { margin: 0; font-size: 18px; font-weight: 800; }
    .close-x { background: #f1f5f9; border: none; width: 32px; height: 32px; border-radius: 50%; cursor: pointer; }
    .modal-scroll-body { flex: 1; overflow-y: auto; background: #f8fafc; }

    .clients-initial-loader { display: flex; justify-content: center; padding: 100px 0; }
    .spinner { width: 32px; height: 32px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; }

    .pager { display: flex; justify-content: center; align-items: center; gap: 20px; margin-top: 32px; }
    .btn-p { background: white; border: 1.5px solid #f1f5f9; padding: 10px 20px; border-radius: 14px; font-weight: 700; color: #64748b; cursor: pointer; }
    .bottom-spacer { height: 120px; }
</style>
