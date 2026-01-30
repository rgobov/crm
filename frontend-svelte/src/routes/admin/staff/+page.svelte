<script>
    import { onMount } from 'svelte';
    import { staffService } from '$lib/services/staffService.js';
    import { staffSearchQuery, cachedStaff, staffMetadata } from '$lib/stores/ui.js';
    import { goto } from '$app/navigation';

    let isLoading = false;
    let tg = null;
    let debounceTimer;
    let showDropdown = false;
    let quickResults = [];

    // --- ГЛОБАЛЬНЫЙ ПОИСК (Sync с Flutter) ---
    $: if ($staffSearchQuery !== undefined) {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => {
            handleSearch();
        }, 600);
    }

    async function handleSearch() {
        const query = $staffSearchQuery.trim();

        // Если пусто - грузим первую страницу всех сотрудников
        if (!query) {
            showDropdown = false;
            await loadStaffData(0, '');
            return;
        }

        // Логика активации поиска: 2 символа для любого ввода
        if (query.length >= 2) {
            await loadStaffData(0, query);

            // Если в результатах что-то есть - показываем выпадающий список (топ 5)
            quickResults = $cachedStaff.slice(0, 5);
            showDropdown = quickResults.length > 0;
        } else {
            showDropdown = false;
        }
    }

    onMount(async () => {
        if (window.Telegram && window.Telegram.WebApp) {
            tg = window.Telegram.WebApp;
            tg.BackButton.show();
            tg.BackButton.onClick(() => goto('/admin'));
        }
        await loadStaffData(0, $staffSearchQuery);
    });

    async function loadStaffData(page = 0, query = '') {
        isLoading = true;
        try {
            // Запрашиваем данные напрямую с бэкенда по всему репозиторию
            const result = await staffService.getStaff(query, page, 25);

            cachedStaff.set(result.content);
            staffMetadata.set({
                totalElements: result.totalElements,
                totalPages: result.totalPages,
                currentPage: page
            });
        } catch (e) {
            console.error('API Search Error:', e);
        } finally {
            isLoading = false;
        }
    }

    function selectStaff(id) {
        showDropdown = false;
        // Очищаем поиск только если это был быстрый переход
        // $staffSearchQuery = '';
        goto(`/admin/staff/${id}`);
    }
</script>

<div class="page" on:click={() => showDropdown = false}>
    <div class="header">
        <div class="title-row">
            <h1>Персонал</h1>
            {#if isLoading}
                <span class="mini-spinner"></span>
            {/if}
        </div>
        <p class="subtitle">
            {#if $staffSearchQuery}
                Найдено результатов: {$staffMetadata.totalElements}
            {:else}
                Всего в компании: {$staffMetadata.totalElements}
            {/if}
        </p>
    </div>

    <!-- ПОИСК: Контрастное окно с автодополнением -->
    <div class="search-container" on:click|stopPropagation>
        <div class="search-box">
            <span class="search-icon">🔍</span>
            <input
                type="text"
                bind:value={$staffSearchQuery}
                placeholder="Имя или номер телефона..."
                on:focus={() => quickResults.length > 0 && (showDropdown = true)}
            />
            {#if $staffSearchQuery}
                <button class="clear-btn" on:click={() => { $staffSearchQuery = ''; showDropdown = false; }}>✕</button>
            {/if}
        </div>

        {#if showDropdown && $staffSearchQuery}
            <div class="search-dropdown shadow-card">
                {#each quickResults as res}
                    <button class="dropdown-item" on:click={() => selectStaff(res.id)}>
                        <div class="item-avatar">{res.name.charAt(0)}</div>
                        <div class="item-info">
                            <span class="item-name">{res.name}</span>
                            <span class="item-meta">{res.specialty}</span>
                        </div>
                    </button>
                {/each}
            </div>
        {/if}
    </div>

    <div class="content">
        {#if $cachedStaff.length === 0 && !isLoading}
            <div class="empty-state">
                <p>Сотрудник "{$staffSearchQuery}" не найден в базе</p>
            </div>
        {:else}
            <!-- ОСНОВНОЙ СПИСОК (ВСЕГДА СИНХРОНЕН С ПОИСКОМ) -->
            <div class="staff-list" class:dimmed={isLoading}>
                {#each $cachedStaff as member (member.id)}
                    <div class="staff-card card" on:click={() => selectStaff(member.id)}>
                        <div class="avatar">{member.name.charAt(0)}</div>
                        <div class="info">
                            <h3>{member.name}</h3>
                            <p>{member.specialty}</p>
                            {#if member.phone}
                                <span class="phone">📱 {member.phone}</span>
                            {/if}
                        </div>
                        <span class="chevron">›</span>
                    </div>
                {/each}
            </div>

            {#if $staffMetadata.totalPages > 1}
                <div class="pagination">
                    <button disabled={$staffMetadata.currentPage === 0} on:click={() => loadStaffData($staffMetadata.currentPage - 1, $staffSearchQuery)}>←</button>
                    <span>{$staffMetadata.currentPage + 1} / {$staffMetadata.totalPages}</span>
                    <button disabled={$staffMetadata.currentPage >= $staffMetadata.totalPages - 1} on:click={() => loadStaffData($staffMetadata.currentPage + 1, $staffSearchQuery)}>→</button>
                </div>
            {/if}
        {/if}
    </div>

    <button class="fab" on:click={() => goto('/admin/staff/new')}>+</button>
</div>

<style>
    .page { padding: 20px; max-width: 600px; margin: 0 auto; min-height: 100vh; background: var(--bg-color); }
    h1 { font-size: 26px; font-weight: 800; margin: 0; color: #0f172a; }
    .subtitle { color: var(--hint-color); font-size: 14px; margin-top: 4px; }

    .search-container { margin-bottom: 24px; position: sticky; top: 10px; z-index: 1000; }
    .search-box {
        display: flex; align-items: center; background: white; padding: 14px 18px;
        border-radius: 18px; border: 2px solid #3897f0;
        box-shadow: 0 10px 30px rgba(0,0,0,0.08);
    }
    input { border: none; background: none; width: 100%; font-size: 16px; outline: none; margin-left: 10px; font-weight: 500; }
    .clear-btn { background: #f1f5f9; border: none; color: #64748b; border-radius: 50%; width: 24px; height: 24px; cursor: pointer; }

    .search-dropdown { position: absolute; top: 105%; left: 0; right: 0; background: white; border-radius: 20px; padding: 8px; box-shadow: 0 15px 45px rgba(0,0,0,0.15); z-index: 1001; border: 1px solid #f1f5f9; }
    .dropdown-item { width: 100%; display: flex; align-items: center; gap: 12px; padding: 12px; border: none; background: none; cursor: pointer; text-align: left; border-radius: 14px; }
    .dropdown-item:hover { background: #f8fafc; }
    .item-avatar { width: 36px; height: 36px; background: #eff6ff; color: var(--primary-color); border-radius: 10px; display: flex; justify-content: center; align-items: center; font-weight: 800; }
    .item-name { display: block; font-size: 15px; font-weight: 700; color: #1e293b; }
    .item-meta { font-size: 12px; color: var(--hint-color); }

    .staff-list { display: grid; gap: 10px; padding-bottom: 20px; transition: opacity 0.2s; }
    .dimmed { opacity: 0.5; }
    .staff-card { display: flex; align-items: center; gap: 16px; padding: 16px; background: white; border-radius: 22px; cursor: pointer; box-shadow: var(--shadow); }
    .avatar { width: 52px; height: 52px; background: #f1f5f9; color: var(--primary-color); border-radius: 16px; display: flex; justify-content: center; align-items: center; font-weight: 800; font-size: 20px; }
    .info { flex: 1; }
    h3 { margin: 0; font-size: 17px; color: #1e293b; font-weight: 700; }
    .info p { margin: 2px 0 0 0; font-size: 13px; color: var(--hint-color); }
    .phone { font-size: 12px; color: var(--primary-color); font-weight: 600; }
    .chevron { font-size: 20px; color: #cbd5e1; }

    .pagination { display: flex; justify-content: center; align-items: center; gap: 16px; margin-top: 24px; padding-bottom: 100px; }
    .pagination button { padding: 8px 16px; border-radius: 10px; border: 1px solid #e2e8f0; background: white; font-weight: 600; }

    .mini-spinner { width: 20px; height: 20px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
    .fab { position: fixed; bottom: 90px; right: 20px; width: 60px; height: 60px; background: var(--primary-gradient); color: white; border: none; border-radius: 20px; font-size: 32px; box-shadow: 0 12px 30px rgba(56, 151, 240, 0.4); z-index: 100; }
</style>
