<script>
    import { createEventDispatcher } from 'svelte';
    import { adminService } from '$lib/services/adminService.js';
    import { fade, scale } from 'svelte/transition';
    import { portal } from '$lib/actions/portal.js';

    export let candidate;
    const dispatch = createEventDispatcher();

    let message = `Здравствуйте, ${candidate.name}! Давно не видели вас. Приглашаем записаться${candidate.lastService ? ' на ' + candidate.lastService : ''}.`;
    let isSending = false;
    let result = null;

    async function handleSend() {
        if (!message.trim()) return;
        isSending = true;
        result = null;
        try {
            const res = await adminService.sendReturnReminder(candidate.contactId, message.trim());
            result = { success: true };
            setTimeout(() => dispatch('close'), 1500);
        } catch (e) {
            result = { success: false, error: e.response?.data?.error || 'Ошибка отправки' };
        } finally {
            isSending = false;
        }
    }

    function handleBackdropClick(e) {
        if (e.target === e.currentTarget) dispatch('close');
    }
</script>

<div class="modal-backdrop" use:portal on:click={handleBackdropClick} transition:fade={{duration: 200}}>
    <div class="modal-card" transition:scale={{start: 0.95, duration: 200}}>
        <header class="modal-head">
            <h3>✈️ Напомнить клиенту</h3>
            <button class="close-btn" on:click={() => dispatch('close')}>✕</button>
        </header>

        <div class="modal-body">
            <div class="info-block">
                <div class="info-row">
                    <span class="info-label">Клиент</span>
                    <span class="info-val">{candidate.name}</span>
                </div>
                <div class="info-row">
                    <span class="info-label">Телефон</span>
                    <span class="info-val">{candidate.phone}</span>
                </div>
                {#if candidate.lastService}
                    <div class="info-row">
                        <span class="info-label">Последняя услуга</span>
                        <span class="info-val">{candidate.lastService}</span>
                    </div>
                {/if}
                <div class="info-row">
                    <span class="info-label">Не был</span>
                    <span class="info-val">{candidate.daysSinceLastVisit} дн.</span>
                </div>
            </div>

            <div class="field">
                <label for="msg">Сообщение</label>
                <textarea id="msg" bind:value={message} rows="4" disabled={isSending}></textarea>
            </div>

            {#if result}
                <div class="result-banner" class:success={result.success} class:error={!result.success}>
                    {#if result.success}
                        ✅ Отправлено
                    {:else}
                        ❌ {result.error}
                    {/if}
                </div>
            {/if}

            <div class="actions">
                <button class="btn-cancel" on:click={() => dispatch('close')} disabled={isSending}>Отмена</button>
                <button class="btn-send" on:click={handleSend} disabled={isSending || !message.trim()}>
                    {isSending ? '⏳ Отправка...' : '✈️ Отправить'}
                </button>
            </div>
        </div>
    </div>
</div>

<style>
    :global(.modal-backdrop) {
        position: fixed; inset: 0;
        background: rgba(7, 54, 66, 0.6);
        backdrop-filter: blur(8px);
        display: flex; align-items: center; justify-content: center;
        z-index: 99999; padding: 20px;
        box-sizing: border-box;
    }
    .modal-card {
        background: #fdf6e3;
        width: 100%; max-width: 480px;
        border-radius: 28px;
        overflow: hidden;
        box-shadow: 0 30px 60px -12px rgba(0,0,0,0.4);
        border: 1px solid #ddd6c1;
    }
    .modal-head {
        display: flex; justify-content: space-between; align-items: center;
        padding: 20px 24px;
        border-bottom: 1px solid #ddd6c1;
    }
    .modal-head h3 { margin: 0; font-size: 18px; font-weight: 800; color: #073642; }
    .close-btn { background: #eee8d5; border: none; width: 32px; height: 32px; border-radius: 50%; cursor: pointer; color: #073642; font-weight: 700; }
    .modal-body { padding: 20px 24px 24px; }
    .info-block { background: #eee8d5; border-radius: 16px; padding: 14px 16px; margin-bottom: 16px; }
    .info-row { display: flex; justify-content: space-between; padding: 4px 0; font-size: 14px; }
    .info-label { color: #93a1a1; font-weight: 600; }
    .info-val { color: #073642; font-weight: 700; text-align: right; }
    .field { margin-bottom: 16px; }
    .field label { display: block; font-size: 11px; font-weight: 800; color: #586e75; margin-bottom: 8px; text-transform: uppercase; }
    textarea {
        width: 100%; padding: 14px;
        border: 2px solid #ddd6c1; border-radius: 14px;
        font-size: 15px; font-family: inherit; color: #073642;
        outline: none; resize: vertical; box-sizing: border-box;
        background: white;
    }
    textarea:focus { border-color: #0088cc; }
    .result-banner {
        padding: 12px 16px; border-radius: 12px;
        font-weight: 700; font-size: 14px; margin-bottom: 16px;
    }
    .result-banner.success { background: #d1fae5; color: #065f46; }
    .result-banner.error { background: #fee2e2; color: #991b1b; }
    .actions { display: flex; gap: 12px; }
    .btn-cancel {
        flex: 1; padding: 14px;
        background: #eee8d5; border: 1px solid #ddd6c1; border-radius: 14px;
        font-weight: 800; font-size: 14px; color: #586e75; cursor: pointer;
    }
    .btn-send {
        flex: 2; padding: 14px;
        background: #0088cc; color: white; border: none; border-radius: 14px;
        font-weight: 800; font-size: 14px; cursor: pointer;
    }
    .btn-send:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
