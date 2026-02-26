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
    let qrJustUpdated = false;
    let qrVersion = 0;

    // Логируем каждое изменение QR-кода для отладки
    $: if (qrCode) console.log('🖼 QR Code updated, length:', qrCode.length);

    $: isAuthorized = status === 'CONNECTED';
    $: isFloodWait = status.startsWith('FLOOD_WAIT');

    onMount(async () => {
        // Подписка на WebSocket - строго по логике Грок
        unsubscribeWS = telegramStatusSignal.subscribe(signal => {
            if (signal && signal.status && signal.ts > lastUpdateTs) {
                console.log('📡 Telegram WS Sync:', signal.status, 'ts:', signal.ts);
                lastUpdateTs = signal.ts;

                if (isAuthorized || isFloodWait || signal.status === 'WAITING_QR') {
                    isProcessing = false;
                    connectStep = '';
                }

                // Всегда запрашиваем QR при WAITING_QR
                if (signal.status === 'WAITING_QR' && userInitiated) {
                    refreshStatus();
                }

                applyStatusUpdate(signal.status, '');
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
        const currentStatus = status;

        try {
            const data = await telegramService.getStatus();
            console.log('🔍 refreshStatus called → status:', data.status, 'qrCode length:', data.qrCode?.length || 0);

            const isWaiting = data.status === 'WAITING_QR' || currentStatus === 'WAITING_QR';

            if (isWaiting && userInitiated) {
                applyStatusUpdate(data.status, data.qrCode || '');
            } else if (requestTs > lastUpdateTs) {
                applyStatusUpdate(data.status, data.qrCode || '');
                lastUpdateTs = requestTs;
            }
        } catch (e) {
            errorMessage = 'Ошибка связи';
            console.error('❌ refreshStatus error:', e);
        } finally {
            if (isInitial) isLoading = false;
        }
    }

    function applyStatusUpdate(newStatus, newQr) {
        status = newStatus;

        if (newStatus === 'WAITING_QR' && newQr) {
            if (qrCode !== newQr) {
                qrCode = newQr;
                qrVersion = Date.now();
                qrJustUpdated = true;
                setTimeout(() => { qrJustUpdated = false; }, 1500);
            }
        } else if (newStatus !== 'WAITING_QR') {
            qrCode = '';
            qrVersion = 0;
        }

        if (status === 'CONNECTED' || status.startsWith('FLOOD_WAIT')) {
            if (!isProcessing) userInitiated = false;
        }

        if (status === 'DISCONNECTED' && !isProcessing) {
            userInitiated = false;
            qrCode = '';
        }
    }

    async function handleConnect() {
        if (isProcessing) return;
        isProcessing = true;
        userInitiated = true;
        errorMessage = '';
        qrCode = '';
        connectStep = 'ГЕНЕРАЦИЯ...';

        try {
            await telegramService.connect();
        } catch (e) {
            errorMessage = 'Ошибка запуска';
            isProcessing = false;
            userInitiated = false;
        }
    }

    async function handleDisconnect() {
        if (!confirm('Отключить Telegram?')) return;
        isProcessing = true;
        try {
            await telegramService.disconnect();
            userInitiated = false;
            status = 'DISCONNECTED';
            qrCode = '';
            lastUpdateTs = Date.now();
        } catch (e) {
            alert('Ошибка при отключении');
        } finally {
            isProcessing = false;
        }
    }

    function handleClose() {
        dispatch('close');
    }
</script>

<div class="modal-inner">
    <header class="modal-header">
        <div class="header-main">
            <h2 class="header-title">Telegram Уведомления</h2>
            <div class="header-status-badge" class:online={isAuthorized}>
                {#if isAuthorized}ПОДКЛЮЧЕНО{:else if isFloodWait}СИНХРОНИЗАЦИЯ{:else if status === 'WAITING_QR'}ОЖИДАНИЕ QR{:else}ОТКЛЮЧЕНО{/if}
            </div>
        </div>
        <button class="btn-close-round" on:click={handleClose} aria-label="Закрыть">✕</button>
    </header>

    <div class="modal-body">
        {#if isLoading && !userInitiated}
            <div class="loading-state" in:fade>
                <div class="spinner"></div>
                <p>Проверка статуса...</p>
            </div>
        {:else}
            <div class="content-wrapper" in:fade={{duration: 200}}>
                <section class="hero-card" class:active={isAuthorized}>
                    <div class="icon-box">
                        {#if isAuthorized}✓{:else}📱{/if}
                    </div>
                    <div class="hero-info">
                        <label for="status-info-id">КАНАЛ СВЯЗИ</label>
                        <div id="status-info-id" class="status-row">
                            <span class="status-name">Telegram Bot API</span>
                            <div class="dot" class:online={isAuthorized}></div>
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
                            <p>Уведомления клиентам будут уходить автоматически.</p>
                        </div>
                    {:else if userInitiated || status === 'WAITING_QR'}
                        <div class="card qr-card" in:slide>
                            <div class="qr-header">
                                <label>{qrCode ? 'ОТСКАНИРУЙТЕ КОД' : 'ГЕНЕРАЦИЯ...'}</label>
                                {#if qrJustUpdated}
                                    <span class="qr-flash" in:fade>ОБНОВЛЕНО ✅</span>
                                {/if}
                            </div>
                            <div class="qr-container">
                                {#if qrCode}
                                    {#key qrVersion}
                                        <img src="data:image/png;base64,{qrCode}" alt="QR" in:fade />
                                    {/key}
                                {:else}
                                    <div class="qr-loading-box">
                                        <div class="spinner-small"></div>
                                        <p>{connectStep || 'Связь с сервером...'}</p>
                                    </div>
                                {/if}
                            </div>
                            <div class="qr-hint-box">
                                <p class="hint">Настройки → Устройства → Подключить</p>
                                {#if qrCode}
                                    <p class="qr-urgent-hint">Код живет 30 секунд — сканируйте быстрее!</p>
                                {/if}
                            </div>
                        </div>
                    {:else}
                        <div class="card connect-card">
                            <label>ПОДКЛЮЧЕНИЕ</label>
                            <p>Привяжите аккаунт для рассылки уведомлений.</p>
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
            {#if isAuthorized}
                <button class="btn-danger-link" on:click={handleDisconnect}>ОТКЛЮЧИТЬ</button>
            {/if}
            <div class="spacer"></div>
            <button class="btn-secondary" on:click={handleClose}>ЗАКРЫТЬ</button>
        </div>
    </footer>
</div>

<style>
    .modal-inner { display: flex; flex-direction: column; background: #fdf6e3; width: 100%; border-radius: 24px; overflow: hidden; }
    .modal-header { padding: 18px 24px; display: flex; align-items: center; justify-content: space-between; background: #eee8d5; border-bottom: 1.5px solid #ddd6c1; }
    .header-title { font-weight: 900; color: #073642; font-size: 13px; text-transform: uppercase; margin: 0; }
    .header-status-badge { font-size: 9px; font-weight: 900; color: #93a1a1; background: #fdf6e3; padding: 2px 8px; border-radius: 6px; border: 1px solid #ddd6c1; margin-top: 4px; display: inline-block; }
    .header-status-badge.online { color: #859900; border-color: #859900; }

    .btn-close-round { background: #fdf6e3; border: 1px solid #ddd6c1; width: 32px; height: 32px; border-radius: 50%; cursor: pointer; color: #586e75; display: flex; align-items: center; justify-content: center; }
    .modal-body { padding: 24px; min-height: 200px; }
    .spinner { width: 32px; height: 32px; border: 3px solid #eee8d5; border-top-color: #268bd2; border-radius: 50%; animation: spin 1s linear infinite; margin: 0 auto; }
    .spinner-small { width: 24px; height: 24px; border: 2px solid #ddd6c1; border-top-color: #268bd2; border-radius: 50%; animation: spin 1s linear infinite; margin: 20px auto; }

    .hero-card { background: #eee8d5; padding: 16px; border-radius: 20px; display: flex; align-items: center; gap: 16px; border: 1.5px solid #ddd6c1; margin-bottom: 16px; }
    .hero-card.active { border-color: #859900; background: rgba(133, 153, 0, 0.05); }
    .icon-box { width: 48px; height: 48px; background: #fdf6e3; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 20px; border: 1px solid #ddd6c1; }
    .icon-box.success { color: #859900; border-color: #859900; background: rgba(133, 153, 0, 0.1); }

    label { display: block; font-size: 9px; font-weight: 900; color: #93a1a1; text-transform: uppercase; margin-bottom: 2px; }
    .status-name { font-size: 17px; font-weight: 900; color: #073642; }
    .dot { width: 8px; height: 8px; border-radius: 50%; background: #93a1a1; margin-left: 8px; display: inline-block; }
    .dot.online { background: #859900; box-shadow: 0 0 8px #859900; }

    .card { background: #eee8d5; padding: 20px; border-radius: 20px; border: 1.5px solid #ddd6c1; }
    .card b { color: #073642; font-size: 15px; display: block; margin-bottom: 4px; }
    .card p { font-size: 13px; color: #586e75; line-height: 1.4; margin: 0; font-weight: 600; }

    .qr-card { text-align: center; background: #fdf6e3; border-color: #268bd2; }
    .qr-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
    .qr-container { background: white; padding: 12px; border-radius: 16px; display: inline-block; border: 1px solid #ddd6c1; margin: 12px 0; min-width: 180px; min-height: 180px; text-align: center; }
    .qr-container img { width: 180px; height: 180px; display: block; }

    .qr-flash { font-size: 10px; font-weight: 900; color: #859900; background: rgba(133, 153, 0, 0.1); padding: 2px 8px; border-radius: 6px; }
    .qr-urgent-hint { color: #dc322f !important; font-weight: 900 !important; font-size: 11px !important; margin-top: 4px !important; }

    .btn-primary { width: 100%; background: #268bd2; color: white; border: none; padding: 16px; border-radius: 16px; font-weight: 900; cursor: pointer; margin-top: 12px; transition: 0.2s; }

    .modal-footer { padding: 16px 24px; background: #eee8d5; border-top: 1.5px solid #ddd6c1; display: flex; align-items: center; }
    .spacer { flex: 1; }
    .btn-secondary { background: #fdf6e3; color: #586e75; border: 1.5px solid #ddd6c1; padding: 10px 24px; border-radius: 12px; font-weight: 800; cursor: pointer; }
    .btn-danger-link { background: transparent; color: #dc322f; border: none; font-weight: 900; cursor: pointer; font-size: 12px; }

    @keyframes spin { to { transform: rotate(360deg); } }
</style>
