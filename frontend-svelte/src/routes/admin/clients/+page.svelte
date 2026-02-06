<script>
    import { onMount } from 'svelte';
    import { contactService } from '$lib/services/contactService.js';
    import { goto } from '$app/navigation';
    import { fade, scale } from 'svelte/transition';
    import AddContactModal from '$lib/components/admin/AddContactModal.svelte';
    import ContactDetailScreen from '$lib/components/contacts/ContactDetailScreen.svelte';

    let clients = [];
    let searchQuery = '';
    let lastQuery = ''; // Для предотвращения повторных запросов
    let showAll = true;
    let currentPage = 0;
    let totalPages = 0;
    let totalElements = 0;
    let isLoading = true;
    let debounceTimer;

    let showAddModal = false;
    let selectedClientId = null;

    // УМНЫЙ ПОИСК (Аналогично окну записи и Flutter)
    $: {
        const query = searchQuery.trim();
        const digits = query.replace(/\D/g, '');

        // Условие: 6 цифр для телефона ИЛИ 3 буквы для имени
        const isReadyToSearch = (digits.length >= 6) || (query.length >= 3 && digits.length < 3);

        if (query !== lastQuery) {
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(() => {
                if (query === '' || isReadyToSearch) {
                    lastQuery = query;
                    loadPage(0);
                }
            }, 800); // 800ms - эталонный дебаунс
        }
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
            const result = await contactService.getContacts(searchQuery, showAll, page, 20);
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

    function openDetails(id) {
        selectedClientId = id;
    }

    function closeDetails() {
        selectedClientId = null;
    }
</script>

<div class="clients-page">
    <div class="page-header">
        <div class="title-row">
            <button class="back-btn" on:click={() => goto('/admin')}>‹</button>
            <h1>База клиентов</h1>
        </div>

        <div class="filter-tabs">
            <button class="tab" class:active={!showAll} on:click={() => showAll = false}>
                На сегодня
            </button>
            <button class="tab" class:active={showAll} on:click={() => showAll = true}>
                Вся база
            </button>
        </div>
    </div>

    <div class="search-bar-fixed">
        <div class="search-inner">
            <span class="search-icon">🔍</span>
            <input
                type="text"
                bind:value={searchQuery}
                placeholder="Имя или телефон..."
            />
            {#if searchQuery}
                <button class="btn-clear" on:click={() => searchQuery = ''}>✕</button>
            {/if}
        </div>
    </div>

    <div class="results-stats">
        {#if !isLoading}
            <span>Найдено клиентов: <b>{totalElements}</b></span>
        {/if}
    </div>

    <div class="scroll-container">
        {#if isLoading && clients.length === 0}
            <div class="center-loader"><span class="spinner"></span></div>
        {:else if clients.length === 0}
            <div class="empty-view" in:fade>
                <p>{searchQuery ? 'Ничего не найдено' : (showAll ? 'В базе пока нет клиентов' : 'На сегодня записей нет')}</p>
            </div>
        {:else}
            <div class="client-grid" class:is-loading={isLoading}>
                {#each clients as client (client.id)}
                    <button class="client-card-item" on:click={() => openDetails(client.id)}>
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
        {/if}
    </div>

    <button class="fab-circle" on:click={() => showAddModal = true} title="Добавить клиента">+</button>

    {#if showAddModal}
        <AddContactModal
            on:close={() => showAddModal = false}
            on:success={handleAddSuccess}
        />
    {/if}

    {#if selectedClientId}
        <div class="modal-backdrop" on:click|self={closeDetails} transition:fade={{duration: 200}}>
            <div class="modal-content" transition:scale={{start: 0.95, duration: 200}}>
                <header class="modal-header">
                    <h2>Карточка клиента</h2>
                    <button class="close-x" on:click={closeDetails}>✕</button>
                </header>
                <div class="modal-scroll-body">
                    <ContactDetailScreen contactId={selectedClientId} />
                </div>
            </div>
        </div>
    {/if}
</div>

<style>
    .clients-page { background: #f8fafc; min-height: 100vh; padding-bottom: 100px; }

    .page-header { background: white; padding: 20px 24px; border-bottom: 1px solid #f1f5f9; position: sticky; top: 0; z-index: 100; }
    .title-row { display: flex; align-items: center; gap: 16px; margin-bottom: 20px; }
    .back-btn { background: #f1f5f9; border: none; width: 36px; height: 36px; border-radius: 12px; font-size: 24px; cursor: pointer; color: var(--primary-color); display: flex; align-items: center; justify-content: center; padding-bottom: 4px; }
    h1 { font-size: 22px; font-weight: 900; margin: 0; color: #0f172a; }

    .filter-tabs { background: #f1f5f9; padding: 4px; border-radius: 14px; display: flex; gap: 4px; }
    .tab { flex: 1; border: none; background: none; padding: 8px; border-radius: 10px; font-size: 13px; font-weight: 700; color: #64748b; cursor: pointer; }
    .tab.active { background: white; color: var(--primary-color); box-shadow: 0 4px 12px rgba(0,0,0,0.05); }

    .search-bar-fixed { padding: 16px 20px; }
    .search-inner { display: flex; align-items: center; background: white; padding: 12px 16px; border-radius: 18px; border: 1.5px solid #f1f5f9; box-shadow: 0 4px 15px rgba(0,0,0,0.02); }
    input { border: none; background: none; width: 100%; font-size: 15px; outline: none; font-weight: 500; }
    .btn-clear { background: #f1f5f9; border: none; border-radius: 50%; width: 20px; height: 20px; font-size: 10px; color: #94a3b8; cursor: pointer; }

    .results-stats { padding: 0 24px; font-size: 12px; color: #94a3b8; font-weight: 600; margin-bottom: 12px; }

    .scroll-container { padding: 0 20px; }
    .client-grid { display: grid; gap: 10px; }
    .is-loading { opacity: 0.6; }

    .client-card-item { display: flex; align-items: center; gap: 16px; padding: 14px; background: white; border-radius: 20px; border: 1px solid #f1f5f9; cursor: pointer; text-align: left; transition: transform 0.1s; }
    .client-card-item:active { transform: scale(0.98); }

    .avatar-box { width: 44px; height: 44px; background: #eff6ff; color: var(--primary-color); border-radius: 14px; display: flex; justify-content: center; align-items: center; font-weight: 800; font-size: 18px; }
    .info-meta { flex: 1; min-width: 0; }
    .name-line { font-size: 15px; font-weight: 700; color: #1e293b; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .phone-line { font-size: 13px; color: #94a3b8; font-weight: 500; margin-top: 2px; }
    .chevron-icon { color: #cbd5e1; font-size: 24px; font-weight: 300; }

    .fab-circle { position: fixed; bottom: 100px; right: 24px; width: 56px; height: 56px; background: var(--primary-gradient); color: white; border: none; border-radius: 18px; font-size: 32px; font-weight: 300; box-shadow: 0 10px 25px rgba(56, 151, 240, 0.4); cursor: pointer; z-index: 1000; }

    .modal-backdrop { position: fixed; inset: 0; background: rgba(15, 23, 42, 0.6); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; z-index: 2000; padding: 20px; }
    .modal-content { background: white; width: 100%; max-width: 500px; height: 85vh; border-radius: 32px; display: flex; flex-direction: column; overflow: hidden; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.3); }
    .modal-header { padding: 24px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #f1f5f9; flex-shrink: 0; }
    .modal-header h2 { margin: 0; font-size: 18px; font-weight: 800; color: #0f172a; }
    .close-x { background: #f1f5f9; border: none; width: 32px; height: 32px; border-radius: 50%; font-weight: 800; cursor: pointer; color: #64748b; }
    .modal-scroll-body { flex: 1; overflow-y: auto; background: #f8fafc; }

    .pager { display: flex; justify-content: center; align-items: center; gap: 20px; margin-top: 32px; padding-bottom: 100px; }
    .btn-p { background: white; border: 1.5px solid #f1f5f9; padding: 8px 16px; border-radius: 12px; font-weight: 700; color: #64748b; cursor: pointer; }
    .btn-p:disabled { opacity: 0.4; }

    .spinner { width: 24px; height: 24px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; display: inline-block; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
    .center-loader { display: flex; justify-content: center; padding: 40px; }
</style>
