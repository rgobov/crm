<script>
    import { onMount, onDestroy, createEventDispatcher } from 'svelte';
    import { telegramService } from '$lib/services/telegramService.js';
    import { telegramStatusSignal } from '$lib/services/websocketService.js';
    import { fade, scale, slide } from 'svelte/transition';

    const dispatch = createEventDispatcher();

    let status = 'INITIALIZING';
    let qrCode = '';
    let isLoading = true;
    let errorMessage = '';
    let unsubscribeWS;
    let isAuthorized = false;

    onMount(async () => {
        unsubscribeWS = telegramStatusSignal.subscribe(signal => {
            if (signal && signal.ts > 0) {
                refreshStatus(false);
            }
        });

        // Быстрая проверка без искусственных задержек, вызывающих прыжки
        await refreshStatus(true);
    });

    onDestroy(async () => {
        if (unsubscribeWS) unsubscribeWS();
        if (!isAuthorized && (status === 'WAITING_QR' || (isLoading && qrCode))) {
            telegramService.disconnect().catch(() => {});
        }
    });

    async function refreshStatus(setLoading = false) {
        if (setLoading) isLoading = true;
        try {
            const data = await telegramService.getStatus();
            status = data.status;
            qrCode = data.qrCode;
            isAuthorized = (status === 'CONNECTED');
        } catch (e) {
            errorMessage = 'Ошибка связи';
        } finally {
            isLoading = false;
        }
    }

    async function handleConnect() {
        isLoading = true;
        qrCode = '';
        try {
            await telegramService.connect();
            let attempts = 0;
            const interval = setInterval(async () => {
                await refreshStatus();
                attempts++;
                if (qrCode || isAuthorized || attempts > 5) clearInterval(interval);
            }, 2000);
        } catch (e) {
            errorMessage = 'Ошибка запуска';
            isLoading = false;
        }
    }

    async function handleDisconnect() {
        if (!confirm('Отключить Telegram?')) return;
        try {
            isLoading = true;
            await telegramService.disconnect();
            isAuthorized = false;
            status = 'DISCONNECTED';
            qrCode = '';
            refreshStatus(true);
        } catch (e) {
            alert('Ошибка');
            isLoading = false;
        }
    }
</script>

<div class="modal-inner">
    <header class="modal-header">
        <div class="header-title">Telegram Уведомления</div>
        <button class="btn-close-round" on:click={() => dispatch('close')}>✕</button>
    </header>

    <div class="modal-body">
        {#if isLoading}
            <div class="loading-state" in:fade={{duration: 100}}>
                <div class="spinner"></div>
                <p>Проверка...</p>
            </div>
        {:else}
            <div class="content-wrapper" in:fade={{duration: 200}}>
                <section class="hero-card">
                    <div class="icon-box {isAuthorized ? 'success' : ''}">
                        {#if isAuthorized}
                            ✓
                        {:else}
                            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="3">
                                <path d="m22 2-7 20-4-9-9-4Z"/><path d="M22 2 11 13"/>
                            </svg>
                        {/if}
                    </div>
                    <div class="hero-info">
                        <label>СТАТУС КАНАЛА</label>
                        <div class="status-row">
                            <span class="status-name">Telegram</span>
                            <div class="dot {isAuthorized ? 'online' : 'offline'}"></div>
                        </div>
                    </div>
                </section>

                <div class="content-stack">
                    {#if isAuthorized}
                        <div class="card success-card" in:scale={{start: 0.98}}>
                            <label>АКТИВНО</label>
                            <b>Аккаунт подключен</b>
                            <p>Уведомления отправляются автоматически.</p>
                        </div>
                    {:else if qrCode}
                        <div class="card qr-card" in:slide>
                            <div class="qr-header">
                                <label>АВТОРИЗАЦИЯ</label>
                                <button class="btn-refresh" on:click={handleConnect}>ОБНОВИТЬ</button>
                            </div>
                            <div class="qr-container">
                                <img src="data:image/png;base64,{qrCode}" alt="QR" />
                            </div>
                        </div>
                    {:else}
                        <div class="card setup-card">
                            <label>ПОДКЛЮЧЕНИЕ</label>
                            <p>Привяжите аккаунт для рассылки уведомлений.</p>
                            <button class="btn-primary" on:click={handleConnect}>
                                ПОДКЛЮЧИТЬ АККАУНТ
                            </button>
                        </div>
                    {/if}
                </div>
            </div>
        {/if}
    </div>

    <footer class="modal-footer">
        <div class="footer-btns">
            {#if isAuthorized && !isLoading}
                <button class="btn-danger-text" on:click={handleDisconnect}>ОТКЛЮЧИТЬ</button>
            {/if}
            <div class="spacer"></div>
            <button class="btn-secondary" on:click={() => dispatch('close')}>ЗАКРЫТЬ</button>
        </div>
    </footer>
</div>

<style>
    /* УБРАЛИ height: 100% И min-height ДЛЯ ПЛОТНОЙ ВЕРСТКИ */
    .modal-inner { display: flex; flex-direction: column; background: #f8fafc; width: 100%; }

    .modal-header { padding: 16px 24px; display: flex; align-items: center; justify-content: space-between; background: white; border-bottom: 1px solid #f1f5f9; }
    .header-title { font-weight: 900; color: #1e293b; font-size: 14px; text-transform: uppercase; }
    .btn-close-round { background: #f1f5f9; border: none; width: 30px; height: 30px; border-radius: 50%; cursor: pointer; color: #64748b; }

    .modal-body { padding: 20px; position: relative; }

    /* ФИКСИРОВАННЫЙ СПИННЕР ЧТОБЫ НЕ ПРЫГАЛ */
    .loading-state { padding: 40px 0; text-align: center; }
    .spinner { width: 32px; height: 32px; border: 3px solid #f1f5f9; border-top-color: #0ea5e9; border-radius: 50%; animation: spin 1s linear infinite; margin: 0 auto; }
    .loading-state p { font-weight: 700; color: #94a3b8; margin-top: 12px; font-size: 13px; }

    .hero-card { background: white; padding: 16px; border-radius: 24px; display: flex; align-items: center; gap: 16px; border: 1px solid #f1f5f9; margin-bottom: 12px; }
    .icon-box { width: 48px; height: 48px; background: #f1f5f9; border-radius: 14px; display: flex; align-items: center; justify-content: center; color: #94a3b8; }
    .icon-box.success { background: #f0fdf4; color: #22c55e; }

    label { display: block; font-size: 9px; font-weight: 900; color: #94a3b8; text-transform: uppercase; margin-bottom: 2px; }
    .status-name { font-size: 16px; font-weight: 900; color: #1e293b; }
    .dot { width: 8px; height: 8px; border-radius: 50%; background: #cbd5e1; }
    .dot.online { background: #22c55e; box-shadow: 0 0 8px #22c55e; }

    .card { background: white; padding: 20px; border-radius: 24px; border: 1px solid #f1f5f9; }
    .card p { font-size: 13px; color: #64748b; margin-top: 4px; line-height: 1.4; }

    .qr-card { text-align: center; }
    .qr-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
    .btn-refresh { background: #f1f5f9; border: none; padding: 6px 10px; border-radius: 10px; color: #0ea5e9; font-size: 10px; font-weight: 800; cursor: pointer; }
    .qr-container { background: #f8fafc; padding: 12px; border-radius: 20px; display: inline-block; }
    .qr-container img { width: 160px; height: 160px; }

    .btn-primary { width: 100%; background: #0ea5e9; color: white; border: none; padding: 14px; border-radius: 16px; font-weight: 800; cursor: pointer; margin-top: 12px; }

    /* СТАБИЛЬНЫЙ ФУТЕР */
    .modal-footer { padding: 12px 24px; background: white; border-top: 1px solid #f1f5f9; }
    .footer-btns { display: flex; align-items: center; min-height: 44px; }
    .spacer { flex: 1; }

    .btn-secondary { background: white; color: #64748b; border: 1px solid #e2e8f0; padding: 10px 20px; border-radius: 14px; font-weight: 700; cursor: pointer; }
    .btn-danger-text { background: transparent; color: #ef4444; border: none; font-weight: 800; cursor: pointer; padding: 10px 0; }

    @keyframes spin { to { transform: rotate(360deg); } }
</style>
