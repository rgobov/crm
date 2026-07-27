<script>
    import { onMount } from 'svelte';
    import { adminService } from '$lib/services/adminService.js';
    import { goto } from '$app/navigation';
    import { fade } from 'svelte/transition';
    import ReturnReminderModal from '$lib/components/admin/ReturnReminderModal.svelte';

    let candidates = [];
    let isLoading = true;
    let daysThreshold = 30;
    let selectedCandidate = null;

    onMount(() => {
        if (window.Telegram && window.Telegram.WebApp && window.Telegram.WebApp.BackButton) {
            window.Telegram.WebApp.BackButton.show();
            window.Telegram.WebApp.BackButton.onClick(() => goto('/admin'));
        }
        loadCandidates();
    });

    async function loadCandidates() {
        isLoading = true;
        try {
            candidates = await adminService.getReturnReminderCandidates(daysThreshold);
        } catch (e) {
            console.error('Failed to load return reminders', e);
        } finally {
            isLoading = false;
        }
    }

    function formatDate(dateStr) {
        if (!dateStr) return '—';
        const d = new Date(dateStr);
        return d.toLocaleDateString('ru-RU', { day: 'numeric', month: 'long', year: 'numeric' });
    }

    function formatPhone(phone) {
        if (!phone) return '—';
        const c = phone.replace(/\D/g, '');
        if (c.length === 11) return `+${c[0]} (${c.slice(1,4)}) ${c.slice(4,7)}-${c.slice(7,9)}-${c.slice(9,11)}`;
        return phone;
    }

    function handleRemindSent() {
        selectedCandidate = null;
        loadCandidates();
    }
</script>

<div class="screen-wrapper">
    <header class="page-header">
        <button class="back-btn" on:click={() => goto('/admin')}>←</button>
        <h1>Возврат клиентов</h1>
        <div style="width: 40px"></div>
    </header>

    <div class="filter-bar">
        <label>Не были более</label>
        <input type="number" bind:value={daysThreshold} min="1" max="365" />
        <label>дн.</label>
        <button class="btn-filter" on:click={loadCandidates}>🔍</button>
    </div>

    <div class="content">
        {#if isLoading}
            <div class="center-loader"><span class="spinner"></span></div>
        {:else if candidates.length === 0}
            <div class="empty-state" in:fade>
                <p>Все клиенты были в течение {daysThreshold} дней</p>
            </div>
        {:else}
            <div class="candidates-list">
                {#each candidates as c (c.contactId)}
                    <div class="candidate-card" in:fade>
                        <div class="card-main">
                            <div class="card-info">
                                <div class="name-row">
                                    <span class="name">{c.name}</span>
                                    <span class="days-badge">{c.daysSinceLastVisit} дн.</span>
                                </div>
                                <div class="phone-row">{formatPhone(c.phone)}</div>
                                {#if c.lastService}
                                    <div class="service-row">Последнее: {c.lastService}</div>
                                {/if}
                                <div class="date-row">Был: {formatDate(c.lastVisit)}</div>
                            </div>
                            <button class="btn-remind" on:click={() => selectedCandidate = c}>
                                ✈️
                            </button>
                        </div>
                    </div>
                {/each}
            </div>
        {/if}
    </div>
</div>

{#if selectedCandidate}
    <ReturnReminderModal candidate={selectedCandidate} on:close={() => selectedCandidate = null} on:close={handleRemindSent} />
{/if}

<style>
    .screen-wrapper { min-height: 100vh; background: #fdf6e3; }
    .page-header {
        display: flex; justify-content: space-between; align-items: center;
        padding: 16px 20px; background: #eee8d5; border-bottom: 1px solid #ddd6c1;
        position: sticky; top: 0; z-index: 10;
    }
    .back-btn { background: none; border: none; color: #268bd2; font-size: 20px; cursor: pointer; font-weight: 700; }
    h1 { font-size: 18px; font-weight: 800; margin: 0; color: #073642; }

    .filter-bar {
        display: flex; align-items: center; gap: 8px;
        padding: 16px 20px; background: #fdf6e3;
        border-bottom: 1px solid #ddd6c1;
    }
    .filter-bar label { font-size: 13px; font-weight: 700; color: #586e75; }
    .filter-bar input {
        width: 60px; padding: 8px 12px;
        border: 2px solid #ddd6c1; border-radius: 10px;
        font-size: 15px; font-weight: 800; text-align: center;
        background: white; color: #073642; outline: none;
    }
    .filter-bar input:focus { border-color: #268bd2; }
    .btn-filter {
        background: #268bd2; color: white; border: none;
        width: 36px; height: 36px; border-radius: 10px;
        font-size: 16px; cursor: pointer;
    }

    .content { padding: 16px 20px; max-width: 700px; margin: 0 auto; }
    .center-loader { display: flex; justify-content: center; padding: 60px; }
    .spinner { width: 32px; height: 32px; border: 3px solid #eee8d5; border-top-color: #268bd2; border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
    .empty-state { text-align: center; padding: 60px 20px; color: #93a1a1; font-weight: 600; }

    .candidates-list { display: flex; flex-direction: column; gap: 10px; }
    .candidate-card {
        background: #eee8d5; border: 1px solid #ddd6c1;
        border-radius: 20px; padding: 16px 20px;
    }
    .card-main { display: flex; justify-content: space-between; align-items: center; gap: 16px; }
    .card-info { flex: 1; min-width: 0; }
    .name-row { display: flex; align-items: center; gap: 10px; margin-bottom: 4px; }
    .name { font-size: 16px; font-weight: 800; color: #073642; }
    .days-badge {
        background: #fdf6e3; padding: 2px 10px; border-radius: 8px;
        font-size: 11px; font-weight: 800; color: #dc322f;
    }
    .phone-row { font-size: 13px; font-weight: 600; color: #586e75; margin-bottom: 2px; }
    .service-row { font-size: 12px; font-weight: 600; color: #268bd2; }
    .date-row { font-size: 11px; font-weight: 500; color: #93a1a1; margin-top: 2px; }
    .btn-remind {
        background: #0088cc; color: white; border: none;
        width: 44px; height: 44px; border-radius: 14px;
        font-size: 20px; cursor: pointer; flex-shrink: 0;
        display: flex; align-items: center; justify-content: center;
        transition: transform 0.15s;
    }
    .btn-remind:active { transform: scale(0.9); }
</style>
