<script>
    import { onMount, onDestroy, createEventDispatcher } from 'svelte';
    import { telegramService } from '$lib/services/telegramService.js';
    import { telegramStatusSignal } from '$lib/services/websocketService.js';
    import { fade, scale, slide } from 'svelte/transition';

    const dispatch = createEventDispatcher();

    let status = 'DISCONNECTED';
    let qrCode = '';
    let waitSeconds = 0;
    let isLoading = true;
    let isProcessing = false;
    let connectStep = '';
    let errorMessage = '';
    let unsubscribeWS;

    let lastUpdateTs = 0;
    let userInitiated = false;

    $: isAuthorized = status === 'CONNECTED';
    $: isFloodWait = status.startsWith('FLOOD_WAIT');

    onMount(async () => {
        unsubscribeWS = telegramStatusSignal.subscribe(signal => {
            if (signal && signal.status && signal.ts > lastUpdateTs) {
                console.log('📡 Telegram WS Update:', signal.status);
                lastUpdateTs = signal.ts;

                if (signal.status === 'CONNECTED' || signal.status.startsWith('FLOOD_WAIT')) {
                    isProcessing = false;
                    connectStep = '';
                }

                applyStatusUpdate(signal.status, signal.status === 'WAITING_QR' ? (signal.qrCode || qrCode) : '');
            }
        });

        await refreshStatus(true);
    });

    onDestroy(() => {
        if (unsubscribeWS) unsubscribeWS();
    });

    async function refreshStatus(isInitial = false) {
        if (isInitial) isLoading = true;
        const requestTs = Date.now();
        try {
            const data = await telegramService.getStatus();
            if (requestTs > lastUpdateTs) {
                lastUpdateTs = requestTs;
                const qrToShow = (userInitiated || status === 'WAITING_QR') ? data.qrCode : '';
                applyStatusUpdate(data.status, qrToShow);
            }
        } catch (e) {
            errorMessage = 'Нет связи с Telegram сервисом';
        } finally {
            isLoading = false;
        }
    }

    function applyStatusUpdate(newStatus, newQr) {
        status = newStatus;
        qrCode = newQr;

        if (status === 'CONNECTED' || status.startsWith('FLOOD_WAIT')) {
            qrCode = '';
            userInitiated = false;
        }

        if (status.startsWith('FLOOD_WAIT')) {
            waitSeconds = parseInt(status.split('_')[2]) || 300;
        }

        if (status === 'DISCONNECTED') {
            qrCode = '';
            userInitiated = false;
        }
    }

    async function handleConnect() {
        if (isProcessing) return;
        isProcessing = true;
        userInitiated = true;
        errorMessage = '';
        qrCode = '';

        try {
            connectStep = 'СБРОС СЕССИИ...';
            await telegramService.disconnect();
            await new Promise(r => setTimeout(r, 1000));

            connectStep = 'ЗАПУСК...';
            await telegramService.connect();

            connectStep = 'ГЕНЕРАЦИЯ QR...';
            setTimeout(() => {
                if (!isAuthorized && !qrCode) refreshStatus();
                isProcessing = false;
                connectStep = '';
            }, 4000);

        } catch (e) {
            errorMessage = 'Ошибка при запуске подключения';
            isProcessing = false;
            connectStep = '';
        }
    }

    async function handleDisconnect() {
        if (!confirm('Отключить Telegram?')) return;
        isProcessing = true;
        try {
            await telegramService.disconnect();
            lastUpdateTs = Date.now();
            userInitiated = false;
            applyStatusUpdate('DISCONNECTED', '');
        } catch (e) {
            alert('Ошибка при отключении');
        } finally {
            isProcessing = false;
        }
    }
</script>

<div class="modal-inner">
    <header class="modal-header">
        <div class="header-main">
            <div class="header-title">Telegram Уведомления</div>
            <div class="header-status-badge" class:online={isAuthorized} class:warning={isFloodWait}>
                {#if isAuthorized}ПОДКЛЮЧЕНО{:else if isFloodWait}СИНХРОНИЗАЦИЯ{:else if status === 'WAITING_QR'}ОЖИДАНИЕ QR{:else}ОТКЛЮЧЕНО{/if}
            </div>
        </div>
        <button class="btn-close-round" on:click={() => dispatch('close')}>✕</button>
    </header>

    <div class="modal-body">
        {#if isLoading}
            <div class="loading-state" in:fade>
                <div class="spinner"></div>
                <p>Получение данных...</p>
            </div>
        {:else}
            <div class="content-wrapper" in:fade={{duration: 200}}>
                <section class="hero-card" class:active={isAuthorized} class:wait={isFloodWait}>
                    <div class="icon-box">
                        {#if isAuthorized}✓{:else if isFloodWait}⏳{:else}📱{/if}
                    </div>
                    <div class="hero-info">
                        <label>СТАТУС КАНАЛА</label>
                        <div class="status-row">
                            <span class="status-name">Telegram Bot API</span>
                            <div class="dot" class:online={isAuthorized} class:waiting={isFloodWait}></div>
                        </div>
                    </div>
                </section>

                {#if errorMessage}
                    <div class="error-banner" in:slide>⚠️ {errorMessage}</div>
                {/if}

                <div class="content-stack">
                    {#if isAuthorized}
                        <div class="card success-card" in:scale>
                            <label>АКТИВНО</label>
                            <b>Аккаунт успешно привязан</b>
                            <p>Уведомления будут уходить клиентам в штатном режиме.</p>
                        </div>
                    {:else if isFloodWait}
                        <div class="card wait-card" in:slide>
                            <label>ОЖИДАНИЕ</label>
                            <b>Telegram проверяет сессию</b>
                            <p>Защитный интервал: {Math.ceil(waitSeconds/60)} мин. Можно закрыть это окно.</p>
                            <div class="progress-bar"><div class="progress-fill"></div></div>
                        </div>
                    {:else if qrCode || (status === 'WAITING_QR' && userInitiated) || connectStep === 'ГЕНЕРАЦИЯ QR...'}
                        <div class="card qr-card" in:slide>
                            <div class="qr-header">
                                <label>{qrCode ? 'ОТСКАНИРУЙТЕ КОД' : 'ГЕНЕРАЦИЯ...'}</label>
                                <button class="btn-text" on:click={handleConnect} disabled={isProcessing}>ОБНОВИТЬ</button>
                            </div>
                            <div class="qr-container">
                                {#if qrCode}
                                    <img src="data:image/png;base64,{qrCode}" alt="QR" in:fade />
                                {:else}
                                    <div class="qr-loading-box">
                                        <div class="spinner-small"></div>
                                        <p>Связь с Telegram...</p>
                                    </div>
                                {/if}
                            </div>
                            <p class="hint">Настройки → Устройства → Подключить</p>
                        </div>
                    {:else}
                        <div class="card connect-card">
                            <label>ПОДКЛЮЧЕНИЕ</label>
                            <p>Нажмите кнопку для начала привязки аккаунта.</p>
                            <button class="btn-primary" on:click={handleConnect} disabled={isProcessing}>
                                {#if isProcessing}{connectStep}{:else}ПОДКЛЮЧИТЬ АККАУНТ{/if}
                            </button>
                        </div>
                    {/if}
                </div>
            </div>
        {/if}
    </div>

    <footer class="modal-footer">
        <div class="footer-layout">
            {#if isAuthorized || isFloodWait}
                <button class="btn-danger-link" on:click={handleDisconnect} disabled={isProcessing}>
                    ОТКЛЮЧИТЬ
                </button>
            {/if}
            <div class="spacer"></div>
            <button class="btn-secondary" on:click={() => dispatch('close')}>ЗАКРЫТЬ</button>
        </div>
    </footer>
</div>

<style>
    .modal-inner { display: flex; flex-direction: column; background: #fdf6e3; width: 100%; border-radius: 24px; overflow: hidden; }
    .modal-header { padding: 18px 24px; display: flex; align-items: center; justify-content: space-between; background: #eee8d5; border-bottom: 1.5px solid #ddd6c1; }
    .header-title { font-weight: 900; color: #073642; font-size: 13px; text-transform: uppercase; letter-spacing: 0.5px; }
    .header-status-badge { font-size: 9px; font-weight: 900; color: #93a1a1; background: #fdf6e3; padding: 2px 8px; border-radius: 6px; border: 1.5px solid #ddd6c1; margin-top: 4px; display: inline-block; }
    .header-status-badge.online { color: #859900; border-color: #859900; }
    .header-status-badge.warning { color: #b58900; border-color: #b58900; }

    .btn-close-round { background: #fdf6e3; border: 1px solid #ddd6c1; width: 32px; height: 32px; border-radius: 50%; cursor: pointer; color: #586e75; display: flex; align-items: center; justify-content: center; }
    .modal-body { padding: 24px; min-height: 240px; }
    .spinner { width: 32px; height: 32px; border: 3px solid #eee8d5; border-top-color: #268bd2; border-radius: 50%; animation: spin 1s linear infinite; margin: 0 auto; }
    .spinner-small { width: 24px; height: 24px; border: 2px solid #ddd6c1; border-top-color: #268bd2; border-radius: 50%; animation: spin 1s linear infinite; margin-bottom: 8px; }

    .hero-card { background: #eee8d5; padding: 16px; border-radius: 20px; display: flex; align-items: center; gap: 16px; border: 1.5px solid #ddd6c1; margin-bottom: 16px; }
    .hero-card.active { border-color: #859900; background: rgba(133, 153, 0, 0.05); }
    .icon-box { width: 48px; height: 48px; background: #fdf6e3; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 20px; border: 1px solid #ddd6c1; }

    label { display: block; font-size: 9px; font-weight: 900; color: #93a1a1; text-transform: uppercase; margin-bottom: 2px; }
    .status-name { font-size: 17px; font-weight: 900; color: #073642; }
    .dot { width: 8px; height: 8px; border-radius: 50%; background: #93a1a1; margin-left: 8px; display: inline-block; }
    .dot.online { background: #859900; box-shadow: 0 0 8px #859900; }
    .dot.waiting { background: #b58900; animation: pulse 1.5s infinite; }

    .card { background: #eee8d5; padding: 20px; border-radius: 20px; border: 1.5px solid #ddd6c1; }
    .card b { color: #073642; font-size: 15px; display: block; margin-bottom: 4px; }
    .card p { font-size: 13px; color: #586e75; line-height: 1.4; margin: 0; font-weight: 600; }

    .qr-card { text-align: center; background: #fdf6e3; border-color: #268bd2; }
    .qr-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
    .btn-text { background: #eee8d5; border: 1px solid #ddd6c1; padding: 6px 12px; border-radius: 8px; color: #268bd2; font-size: 10px; font-weight: 800; cursor: pointer; }
    .qr-container { background: white; padding: 12px; border-radius: 16px; display: inline-block; border: 1px solid #ddd6c1; margin: 12px 0; min-width: 180px; min-height: 180px; position: relative; }
    .qr-container img { width: 180px; height: 180px; display: block; }
    .qr-loading-box { height: 180px; display: flex; flex-direction: column; align-items: center; justify-content: center; }
    .qr-loading-box p { font-size: 11px; color: #93a1a1; font-weight: 700; margin: 0; }

    .btn-primary { width: 100%; background: #268bd2; color: #fdf6e3; border: none; padding: 16px; border-radius: 18px; font-weight: 900; cursor: pointer; margin-top: 12px; border-bottom: 3px solid #2aa198; }
    .btn-primary:active { transform: translateY(2px); border-bottom-width: 1px; }
    .btn-primary:disabled { opacity: 0.6; cursor: wait; }

    /* FOOTER FIX */
    .modal-footer { padding: 16px 24px; background: #eee8d5; border-top: 1.5px solid #ddd6c1; }
    .footer-layout { display: flex; align-items: center; min-height: 48px; }
    .spacer { flex: 1; }
    .btn-secondary { background: #fdf6e3; color: #586e75; border: 1.5px solid #ddd6c1; padding: 12px 24px; border-radius: 14px; font-weight: 850; cursor: pointer; transition: 0.2s; }
    .btn-secondary:hover { background: #eee8d5; }

    .btn-danger-link { background: transparent; color: #dc322f; border: none; font-weight: 900; cursor: pointer; font-size: 12px; padding: 10px 0; }
    .btn-danger-link:hover { text-decoration: underline; }

    .progress-bar { height: 4px; background: #ddd6c1; border-radius: 2px; margin-top: 12px; overflow: hidden; }
    .progress-fill { height: 100%; background: #b58900; width: 30%; animation: slide 2s infinite linear; }

    @keyframes spin { to { transform: rotate(360deg); } }
    @keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.4; } }
    @keyframes slide { from { transform: translateX(-100%); } to { transform: translateX(300%); } }
</style>
