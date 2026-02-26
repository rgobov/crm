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

    // Для 2FA
    let cloudPassword = '';

    $: isAuthorized = status === 'CONNECTED';
    $: isFloodWait = status.startsWith('FLOOD_WAIT');
    $: isWaitPassword = status === 'WAITING_PASSWORD' || status === 'PASSWORD_ERROR';

    onMount(async () => {
        unsubscribeWS = telegramStatusSignal.subscribe(signal => {
            if (signal && signal.status && signal.ts > lastUpdateTs) {
                console.log('📡 Telegram WS Sync:', signal.status, 'ts:', signal.ts);
                lastUpdateTs = signal.ts;

                if (isAuthorized || isFloodWait || signal.status === 'WAITING_QR' || signal.status.includes('PASSWORD')) {
                    isProcessing = false;
                    connectStep = '';
                }

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
        try {
            const data = await telegramService.getStatus();
            const isWaiting = data.status === 'WAITING_QR' || status === 'WAITING_QR';

            if (isWaiting && userInitiated) {
                applyStatusUpdate(data.status, data.qrCode || '');
            } else if (requestTs > lastUpdateTs) {
                applyStatusUpdate(data.status, data.qrCode || '');
                lastUpdateTs = requestTs;
            }
        } catch (e) {
            errorMessage = 'Ошибка связи';
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
        }

        if (isAuthorized || isFloodWait) {
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
        connectStep = 'ЗАПУСК...';

        try {
            await telegramService.connect();
        } catch (e) {
            errorMessage = 'Ошибка запуска';
            isProcessing = false;
            userInitiated = false;
        }
    }

    async function submitPassword() {
        if (!cloudPassword || isProcessing) return;
        isProcessing = true;
        errorMessage = '';
        try {
            // В telegramService.js должен быть метод sendPassword
            await telegramService.sendPassword(cloudPassword);
            cloudPassword = '';
        } catch (e) {
            errorMessage = 'Неверный пароль';
            isProcessing = false;
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
            alert('Ошибка');
        } finally {
            isProcessing = false;
        }
    }
</script>

<div class="modal-inner">
    <header class="modal-header">
        <div class="header-main">
            <h2 class="header-title">Telegram Уведомления</h2>
            <div class="header-status-badge" class:online={isAuthorized} class:warning={isWaitPassword}>
                {#if isAuthorized}ПОДКЛЮЧЕНО{:else if isWaitPassword}НУЖЕН ПАРОЛЬ{:else if status === 'WAITING_QR'}ОЖИДАНИЕ QR{:else}ОТКЛЮЧЕНО{/if}
            </div>
        </div>
        <button class="btn-close-round" on:click={() => dispatch('close')}>✕</button>
    </header>

    <div class="modal-body">
        {#if isLoading && !userInitiated}
            <div class="loading-state" in:fade>
                <div class="spinner"></div>
                <p>Проверка статуса...</p>
            </div>
        {:else}
            <div class="content-wrapper" in:fade={{duration: 200}}>
                <section class="hero-card" class:active={isAuthorized} class:warning={isWaitPassword}>
                    <div class="icon-box">
                        {#if isAuthorized}✓{:else if isWaitPassword}🔐{:else}📱{/if}
                    </div>
                    <div class="hero-info">
                        <label for="st-info">КАНАЛ СВЯЗИ</label>
                        <div id="st-info" class="status-row">
                            <span class="status-name">Telegram Bot API</span>
                            <div class="dot" class:online={isAuthorized} class:waiting={isWaitPassword}></div>
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
                    {:else if isWaitPassword}
                        <div class="card setup-card" in:slide>
                            <label>ДВУХЭТАПНАЯ ПРОВЕРКА</label>
                            <b>Введите облачный пароль</b>
                            <p>На вашем аккаунте включена дополнительная защита.</p>
                            <div class="password-input-group">
                                <input
                                    type="password"
                                    bind:value={cloudPassword}
                                    placeholder="Ваш пароль"
                                    class="input-primary"
                                    on:keydown={(e) => e.key === 'Enter' && submitPassword()}
                                />
                                <button class="btn-primary" on:click={submitPassword} disabled={isProcessing}>
                                    {#if isProcessing}...{:else}ПОДТВЕРДИТЬ{/if}
                                </button>
                            </div>
                            {#if status === 'PASSWORD_ERROR'}
                                <p class="error-text">❌ Неверный пароль. Попробуйте еще раз.</p>
                            {/if}
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
                            <p class="hint">Настройки → Устройства → Подключить</p>
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
            <button class="btn-secondary" on:click={() => dispatch('close')}>ЗАКРЫТЬ</button>
        </div>
    </footer>
</div>

<style>
    .modal-inner { display: flex; flex-direction: column; background: #fdf6e3; width: 100%; border-radius: 24px; overflow: hidden; }
    .modal-header { padding: 18px 24px; display: flex; align-items: center; justify-content: space-between; background: #eee8d5; border-bottom: 1.5px solid #ddd6c1; }
    .header-title { font-weight: 900; color: #073642; font-size: 13px; text-transform: uppercase; margin: 0; }
    .header-status-badge { font-size: 9px; font-weight: 900; color: #93a1a1; background: #fdf6e3; padding: 2px 8px; border-radius: 6px; border: 1px solid #ddd6c1; margin-top: 4px; display: inline-block; }
    .header-status-badge.online { color: #859900; border-color: #859900; }
    .header-status-badge.warning { color: #b58900; border-color: #b58900; }

    .btn-close-round { background: #fdf6e3; border: 1px solid #ddd6c1; width: 32px; height: 32px; border-radius: 50%; cursor: pointer; color: #586e75; display: flex; align-items: center; justify-content: center; }
    .modal-body { padding: 24px; min-height: 200px; }
    .spinner { width: 32px; height: 32px; border: 3px solid #eee8d5; border-top-color: #268bd2; border-radius: 50%; animation: spin 1s linear infinite; margin: 0 auto; }
    .spinner-small { width: 24px; height: 24px; border: 2px solid #ddd6c1; border-top-color: #268bd2; border-radius: 50%; animation: spin 1s linear infinite; margin: 20px auto; }

    .hero-card { background: #eee8d5; padding: 16px; border-radius: 20px; display: flex; align-items: center; gap: 16px; border: 1.5px solid #ddd6c1; margin-bottom: 16px; }
    .hero-card.active { border-color: #859900; background: rgba(133, 153, 0, 0.05); }
    .hero-card.warning { border-color: #b58900; background: rgba(181, 137, 0, 0.05); }
    .icon-box { width: 48px; height: 48px; background: #fdf6e3; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 20px; border: 1px solid #ddd6c1; }

    label { display: block; font-size: 9px; font-weight: 900; color: #93a1a1; text-transform: uppercase; margin-bottom: 2px; }
    .status-name { font-size: 17px; font-weight: 900; color: #073642; }
    .dot { width: 8px; height: 8px; border-radius: 50%; background: #93a1a1; margin-left: 8px; display: inline-block; }
    .dot.online { background: #859900; box-shadow: 0 0 8px #859900; }
    .dot.waiting { background: #b58900; animation: pulse 1.5s infinite; }

    .card { background: #eee8d5; padding: 20px; border-radius: 20px; border: 1.5px solid #ddd6c1; }
    .card b { color: #073642; font-size: 15px; display: block; margin-bottom: 4px; }
    .card p { font-size: 13px; color: #586e75; line-height: 1.4; margin: 0; font-weight: 600; }

    .password-input-group { margin-top: 12px; display: flex; flex-direction: column; gap: 8px; }
    .input-primary { background: white; border: 1.5px solid #ddd6c1; padding: 12px 16px; border-radius: 14px; font-size: 14px; outline: none; }
    .input-primary:focus { border-color: #268bd2; }
    .error-text { color: #dc322f; font-size: 11px; margin-top: 8px; font-weight: 700; }

    .btn-primary { width: 100%; background: #268bd2; color: white; border: none; padding: 16px; border-radius: 16px; font-weight: 900; cursor: pointer; transition: 0.2s; }
    .btn-primary:active { transform: translateY(2px); }

    .modal-footer { padding: 16px 24px; background: #eee8d5; border-top: 1.5px solid #ddd6c1; display: flex; align-items: center; }
    .spacer { flex: 1; }
    .btn-secondary { background: #fdf6e3; color: #586e75; border: 1.5px solid #ddd6c1; padding: 10px 24px; border-radius: 12px; font-weight: 800; cursor: pointer; }
    .btn-danger-link { background: transparent; color: #dc322f; border: none; font-weight: 900; cursor: pointer; font-size: 12px; }

    @keyframes spin { to { transform: rotate(360deg); } }
    @keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.4; } }
</style>
