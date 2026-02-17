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
                setTimeout(refreshStatus, 500);
            }
        });

        await refreshStatus();
    });

    onDestroy(async () => {
        if (unsubscribeWS) unsubscribeWS();

        if (!isAuthorized && (status === 'WAITING_QR' || (isLoading && qrCode))) {
            telegramService.disconnect().catch(() => {});
        }
    });

    async function refreshStatus() {
        try {
            const data = await telegramService.getStatus();
            status = data.status;
            qrCode = data.qrCode;
            isAuthorized = (status === 'CONNECTED');
            errorMessage = '';
        } catch (e) {
            errorMessage = 'Ошибка связи';
        } finally {
            isLoading = false;
        }
    }

    async function handleConnect() {
        isLoading = true;
        qrCode = ''; // Сбрасываем старый код
        try {
            await telegramService.connect();
            // Делаем пару попыток получить новый код
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
        if (!confirm('Прервать или удалить подключение?')) return;
        try {
            isLoading = true;
            await telegramService.disconnect();
            isAuthorized = false;
            status = 'DISCONNECTED';
            qrCode = '';
            setTimeout(refreshStatus, 1000);
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

    <div class="modal-body" in:fade>

        <section class="hero-card">
            <div class="icon-box {isAuthorized ? 'success' : ''}">
                {#if isAuthorized}
                    ✓
                {:else}
                    <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="3">
                        <path d="m22 2-7 20-4-9-9-4Z"/><path d="M22 2 11 13"/>
                    </svg>
                {/if}
            </div>
            <div class="hero-info">
                <label>СТАТУС</label>
                <div class="status-row">
                    <span class="status-name">Telegram</span>
                    <div class="dot {isAuthorized ? 'online' : 'offline'}"></div>
                </div>
            </div>
        </section>

        <div class="content-stack">
            {#if isAuthorized}
                <div class="card success-card" in:scale>
                    <label>ПОДКЛЮЧЕНО</label>
                    <b>Аккаунт активен</b>
                    <p>Клиенты получают уведомления.</p>
                </div>
            {:else if qrCode}
                <div class="card qr-card" in:slide>
                    <div class="qr-header">
                        <label>АВТОРИЗАЦИЯ</label>
                        <button class="btn-refresh" on:click={handleConnect} title="Обновить QR">
                            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="3">
                                <path d="M21 12a9 9 0 1 1-9-9c2.52 0 4.93 1 6.74 2.74L21 8"/><path d="M21 3v5h-5"/>
                            </svg>
                            ОБНОВИТЬ
                        </button>
                    </div>
                    <p class="qr-hint">Отсканируйте код в приложении</p>
                    <div class="qr-container {isLoading ? 'loading' : ''}">
                        <img src="data:image/png;base64,{qrCode}" alt="QR" />
                        {#if isLoading}
                            <div class="qr-overlay"><div class="spinner"></div></div>
                        {/if}
                    </div>
                </div>
            {:else if status === 'DISCONNECTED' || (status === 'INITIALIZING' && !isLoading)}
                <div class="card setup-card">
                    <label>НАСТРОЙКА</label>
                    <p>Привяжите номер телефона для отправки уведомлений.</p>
                    <button class="btn-primary" on:click={handleConnect}>
                        ПОДКЛЮЧИТЬ АККАУНТ
                    </button>
                </div>
            {:else}
                <div class="card loading-card">
                    <div class="spinner"></div>
                    <p>Связь с Telegram...</p>
                </div>
            {/if}
        </div>
    </div>

    <footer class="modal-footer">
        {#if isAuthorized}
            <button class="btn-danger-text" on:click={handleDisconnect}>ОТКЛЮЧИТЬ</button>
        {/if}
        <button class="btn-secondary" on:click={() => dispatch('close')}>ЗАКРЫТЬ</button>
    </footer>
</div>

<style>
    .modal-inner { display: flex; flex-direction: column; background: #f8fafc; height: 100%; }
    .modal-header { padding: 16px 24px; display: flex; align-items: center; justify-content: space-between; background: white; border-bottom: 1px solid #f1f5f9; }
    .header-title { font-weight: 900; color: #1e293b; font-size: 14px; text-transform: uppercase; letter-spacing: 0.5px; }
    .btn-close-round { background: #f1f5f9; border: none; width: 30px; height: 30px; border-radius: 50%; cursor: pointer; color: #64748b; }

    .modal-body { padding: 20px; flex: 1; }
    .hero-card { background: white; padding: 16px; border-radius: 24px; display: flex; align-items: center; gap: 16px; border: 1px solid #f1f5f9; margin-bottom: 12px; }
    .icon-box { width: 48px; height: 48px; background: #f0f9ff; border-radius: 14px; display: flex; align-items: center; justify-content: center; color: #0ea5e9; }
    .icon-box.success { background: #f0fdf4; color: #22c55e; }
    label { display: block; font-size: 9px; font-weight: 900; color: #94a3b8; text-transform: uppercase; margin-bottom: 2px; }
    .status-name { font-size: 16px; font-weight: 900; }
    .dot { width: 8px; height: 8px; border-radius: 50%; background: #cbd5e1; }
    .dot.online { background: #22c55e; box-shadow: 0 0 8px #22c55e; }

    .card { background: white; padding: 20px; border-radius: 24px; border: 1px solid #f1f5f9; }
    .card p { font-size: 13px; color: #64748b; margin: 4px 0 0 0; }

    .qr-card { text-align: center; }
    .qr-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
    .btn-refresh { background: #f1f5f9; border: none; padding: 6px 10px; border-radius: 10px; color: #0ea5e9; font-size: 10px; font-weight: 800; cursor: pointer; display: flex; align-items: center; gap: 4px; }
    .qr-container { background: #f8fafc; padding: 12px; border-radius: 20px; display: inline-block; position: relative; }
    .qr-container img { width: 160px; height: 160px; mix-blend-mode: multiply; }
    .qr-container.loading img { opacity: 0.3; filter: blur(2px); }
    .qr-overlay { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; }

    .btn-primary { width: 100%; background: #0ea5e9; color: white; border: none; padding: 14px; border-radius: 16px; font-weight: 800; cursor: pointer; margin-top: 12px; }
    .btn-secondary { background: white; color: #64748b; border: 1px solid #e2e8f0; padding: 10px 20px; border-radius: 14px; font-weight: 700; cursor: pointer; }
    .btn-danger-text { background: transparent; color: #ef4444; border: none; font-weight: 800; cursor: pointer; }

    .modal-footer { padding: 12px 24px; display: flex; justify-content: space-between; align-items: center; background: white; border-top: 1px solid #f1f5f9; }
    .spinner { width: 20px; height: 20px; border: 2px solid #f1f5f9; border-top-color: #0ea5e9; border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
</style>
