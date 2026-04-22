<script>
    import { onMount, onDestroy, createEventDispatcher } from 'svelte';
    import { telegramService } from '$lib/services/telegramService.js';
    import { telegramStatusSignal } from '$lib/services/websocketService.js';
    import { fade, scale, slide } from 'svelte/transition';

    const dispatch = createEventDispatcher();

    let status = 'DISCONNECTED';
    let qrCode = '';
    let isLoading = true;
    let isProcessing = false;
    let connectStep = '';
    let errorMessage = '';
    let unsubscribeWS;

    let lastUpdateTs = 0;
    let userInitiated = false;
    let qrJustUpdated = false;
    let qrVersion = 0; // ИСПОЛЬЗУЕМ ДЛЯ ПРИНУДИТЕЛЬНОЙ ПЕРЕРИСОВКИ

    let cloudPassword = '';
    let phoneNumber = '';
    let verificationCode = '';

    $: isAuthorized = status === 'CONNECTED';
    $: isFloodWait = status && status.startsWith('FLOOD_WAIT');
    $: isWaitPassword = status === 'WAITING_PASSWORD' || status === 'PASSWORD_ERROR';
    $: isWaitCode = status === 'WAITING_CODE' || status === 'CODE_ERROR';

    onMount(async () => {
        unsubscribeWS = telegramStatusSignal.subscribe(signal => {
            if (signal && signal.status && signal.ts > lastUpdateTs) {
                console.log('📡 Telegram WS Sync:', signal.status);
                lastUpdateTs = signal.ts;

                if (signal.status === 'WAITING_QR') {
                    // ПРИНУДИТЕЛЬНО ОБНОВЛЯЕМ ВЕРСИЮ, чтобы Svelte перерисовал картинку
                    qrVersion = Date.now();
                    if (signal.qrCode) {
                        applyStatusUpdate(signal.status, signal.qrCode);
                    } else {
                        refreshStatus(false);
                    }
                } else {
                    applyStatusUpdate(signal.status, '');
                }

                if (isAuthorized || isFloodWait || signal.status === 'WAITING_QR' || isWaitPassword) {
                    isProcessing = false;
                    connectStep = '';
                }
            }
        });

        await refreshStatus(true);
    });

    onDestroy(() => {
        if (unsubscribeWS) unsubscribeWS();
    });

    async function refreshStatus(isInitial = false) {
        if (isInitial) isLoading = true;
        try {
            const data = await telegramService.getStatus();
            applyStatusUpdate(data.status, data.qrCode || '');
        } catch (e) {
            errorMessage = 'Ошибка связи';
        } finally {
            if (isInitial) isLoading = false;
        }
    }

    function applyStatusUpdate(newStatus, newQr) {
        status = newStatus;

        if (newStatus === 'WAITING_QR' && newQr) {
            // Даже если строка base64 та же самая, мы обновляем версию для анимации
            if (qrCode !== newQr) {
                console.log('🖼 New QR Code texture detected');
                qrCode = newQr;
                qrVersion = Date.now();
                qrJustUpdated = true;
                setTimeout(() => { qrJustUpdated = false; }, 1500);
            }
        } else if (newStatus !== 'WAITING_QR') {
            qrCode = '';
        }

        if (isAuthorized || isFloodWait || status === 'DISCONNECTED') {
            if (!isProcessing) userInitiated = false;
        }
    }

    async function handleConnect() {
        console.log('🔘 handleConnect called');
        if (isProcessing) {
            console.log('⚠️ Already processing');
            return;
        }
        isProcessing = true;
        userInitiated = true;
        errorMessage = '';
        qrCode = '';
        connectStep = 'ЗАПУСК...';
        console.log('✅ userInitiated set to true');

        try {
            await telegramService.connect();
            console.log('✅ connect API call succeeded');
            isProcessing = false;
        } catch (e) {
            console.error('❌ connect failed:', e);
            errorMessage = 'Ошибка запуска';
            isProcessing = false;
            userInitiated = false;
        }
    }

    async function handleSendCode() {
        if (!phoneNumber || isProcessing) return;
        isProcessing = true;
        errorMessage = '';
        connectStep = 'ОТПРАВКА КОДА...';

        try {
            await telegramService.sendCode(phoneNumber);
            status = 'WAITING_CODE';
            isProcessing = false;
        } catch (e) {
            errorMessage = 'Ошибка отправки кода';
            isProcessing = false;
        }
    }

    async function handleSignIn() {
        if (!verificationCode || isProcessing) return;
        isProcessing = true;
        errorMessage = '';
        connectStep = 'ПРОВЕРКА КОДА...';

        try {
            await telegramService.signIn(verificationCode);
            verificationCode = '';
            isProcessing = false;
        } catch (e) {
            errorMessage = 'Неверный код';
            isProcessing = false;
        }
    }

    async function submitPassword() {
        if (!cloudPassword || isProcessing) return;
        isProcessing = true;
        errorMessage = '';
        try {
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

    async function handleClose() {
        // Отменяем генерацию QR если модальное окно закрывается в статусе WAITING_QR
        if (status === 'WAITING_QR') {
            try {
                await telegramService.cancelQrGeneration();
            } catch (e) {
                console.error('Failed to cancel QR generation:', e);
            }
        }
        dispatch('close');
    }
</script>

<div class="modal-inner">
    <header class="modal-header">
        <div class="header-main">
            <h2 class="header-title">Telegram Уведомления</h2>
            <div class="header-status-badge" class:online={isAuthorized} class:warning={isWaitPassword || isWaitCode}>
                {#if isAuthorized}ПОДКЛЮЧЕНО{:else if isWaitPassword}НУЖЕН ПАРОЛЬ{:else if isWaitCode}ОЖИДАНИЕ КОДА{:else if status === 'WAITING_QR'}ОЖИДАНИЕ QR{:else}ОТКЛЮЧЕНО{/if}
            </div>
        </div>
        <button class="btn-close-round" on:click={handleClose}>✕</button>
    </header>

    <div class="modal-body">
        {#if isLoading && !userInitiated}
            <div class="loading-state" in:fade>
                <div class="spinner"></div>
                <p>Проверка статуса...</p>
            </div>
        {:else}
            <div class="content-wrapper" in:fade={{duration: 200}}>
                <section class="hero-card" class:active={isAuthorized} class:warning={isWaitPassword || isWaitCode}>
                    <div class="icon-box">
                        {#if isAuthorized}✓{:else if isWaitPassword}🔐{:else if isWaitCode}📱{:else}📱{/if}
                    </div>
                    <div class="hero-info">
                        <label for="st-info">КАНАЛ СВЯЗИ</label>
                        <div id="st-info" class="status-row">
                            <span class="status-name">Telegram User API</span>
                            <div class="dot" class:online={isAuthorized} class:waiting={isWaitPassword || isWaitCode}></div>
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
                    {:else if isWaitCode}
                        <div class="card setup-card" in:slide>
                            <label>ПОДТВЕРЖДЕНИЕ КОДА</label>
                            <b>Введите код из Telegram</b>
                            <p>Код был отправлен на ваш номер телефона.</p>
                            <div class="password-input-group">
                                <input
                                    type="text"
                                    bind:value={verificationCode}
                                    placeholder="Код из Telegram"
                                    class="input-primary"
                                    on:keydown={(e) => e.key === 'Enter' && handleSignIn()}
                                />
                                <button class="btn-primary" on:click={handleSignIn} disabled={isProcessing}>
                                    {#if isProcessing}{connectStep}{:else}ПОДТВЕРДИТЬ{/if}
                                </button>
                            </div>
                            {#if status === 'CODE_ERROR'}
                                <p class="error-text">❌ Неверный код. Попробуйте еще раз.</p>
                            {/if}
                        </div>
                    {:else if userInitiated}
                        <div class="card setup-card" in:slide>
                            <label>НОМЕР ТЕЛЕФОНА</label>
                            <b>Введите номер телефона</b>
                            <p>На этот номер будет отправлен код подтверждения.</p>
                            <div class="password-input-group">
                                <input
                                    type="tel"
                                    bind:value={phoneNumber}
                                    placeholder="79991234567"
                                    class="input-primary"
                                    on:keydown={(e) => e.key === 'Enter' && handleSendCode()}
                                />
                                <button class="btn-primary" on:click={handleSendCode} disabled={isProcessing}>
                                    {#if isProcessing}{connectStep}{:else}ОТПРАВИТЬ КОД{/if}
                                </button>
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
            <button class="btn-secondary" on:click={handleClose}>ЗАКРЫТЬ</button>
            {#if isAuthorized}
                <button class="btn-danger" on:click={handleDisconnect}>ОТКЛЮЧИТЬ</button>
            {/if}
        </div>
    </footer>
</div>

<style>
    .modal-inner { display: flex; flex-direction: column; background: #fdf6e3; width: 100%; border-radius: 28px; overflow: hidden; border: 1.5px solid #ddd6c1; }
    .modal-header { padding: 18px 24px; display: flex; align-items: center; justify-content: space-between; background: #eee8d5; border-bottom: 1.5px solid #ddd6c1; }
    .header-title { font-weight: 850; color: #073642; font-size: 16px; text-transform: uppercase; margin: 0; letter-spacing: 0.5px; }
    .header-status-badge { font-size: 10px; font-weight: 900; color: #93a1a1; background: #fdf6e3; padding: 4px 10px; border-radius: 8px; border: 1.5px solid #ddd6c1; margin-top: 4px; display: inline-block; }
    .header-status-badge.online { color: #859900; border-color: #859900; background: #f0fdf4; }
    .header-status-badge.warning { color: #b58900; border-color: #b58900; background: #fffbeb; }

    .btn-close-round { background: #fdf6e3; border: 1px solid #ddd6c1; width: 32px; height: 32px; border-radius: 50%; cursor: pointer; color: #586e75; display: flex; align-items: center; justify-content: center; font-weight: 800; }
    .modal-body { padding: 24px; min-height: 200px; }
    .spinner { width: 32px; height: 32px; border: 3px solid #eee8d5; border-top-color: #268bd2; border-radius: 50%; animation: spin 1s linear infinite; margin: 0 auto; }
    .spinner-small { width: 24px; height: 24px; border: 2px solid #ddd6c1; border-top-color: #268bd2; border-radius: 50%; animation: spin 1s linear infinite; margin: 20px auto; }

    .hero-card { background: #eee8d5; padding: 18px; border-radius: 22px; display: flex; align-items: center; gap: 16px; border: 1.5px solid #ddd6c1; margin-bottom: 20px; }
    .hero-card.active { border-color: #859900; background: #f0fdf4; }
    .hero-card.warning { border-color: #b58900; background: #fffbeb; }
    .icon-box { width: 52px; height: 52px; background: #fdf6e3; border-radius: 14px; display: flex; align-items: center; justify-content: center; font-size: 22px; border: 1.5px solid #ddd6c1; box-shadow: 0 4px 12px rgba(0,0,0,0.03); }

    label { display: block; font-size: 10px; font-weight: 850; color: #93a1a1; text-transform: uppercase; margin-bottom: 4px; letter-spacing: 0.5px; }
    .status-name { font-size: 18px; font-weight: 850; color: #073642; }
    .dot { width: 10px; height: 10px; border-radius: 50%; background: #93a1a1; margin-left: 10px; display: inline-block; }
    .dot.online { background: #859900; box-shadow: 0 0 10px rgba(133, 153, 0, 0.4); }
    .dot.waiting { background: #b58900; animation: pulse 1.5s infinite; }

    .card { background: #eee8d5; padding: 22px; border-radius: 22px; border: 1.5px solid #ddd6c1; }
    .card b { color: #073642; font-size: 16px; display: block; margin-bottom: 6px; font-weight: 850; }
    .card p { font-size: 14px; color: #586e75; line-height: 1.5; margin: 0; font-weight: 700; }

    .qr-container { background: white; padding: 20px; border-radius: 20px; border: 2px solid #ddd6c1; margin: 16px 0; display: flex; justify-content: center; }
    .qr-container img { width: 100%; max-width: 240px; height: auto; display: block; }

    .password-input-group { margin-top: 16px; display: flex; flex-direction: column; gap: 10px; }
    .input-primary { background: white; border: 2px solid #ddd6c1; padding: 14px 18px; border-radius: 16px; font-size: 15px; outline: none; font-weight: 700; color: #073642; }
    .input-primary:focus { border-color: #268bd2; }
    .error-text { color: #dc322f; font-size: 12px; margin-top: 10px; font-weight: 800; }

    .btn-primary { width: 100%; background: #268bd2; color: white; border: none; padding: 18px; border-radius: 18px; font-weight: 900; cursor: pointer; transition: all 0.2s; font-size: 14px; text-transform: uppercase; letter-spacing: 0.5px; }
    .btn-primary:active { transform: scale(0.98); }

    .modal-footer { padding: 18px 24px; background: #eee8d5; border-top: 1.5px solid #ddd6c1; display: flex; align-items: center; }
    .footer-layout { display: flex; justify-content: space-between; width: 100%; }
    .btn-secondary { background: #fdf6e3; color: #586e75; border: 1.5px solid #ddd6c1; padding: 12px 24px; border-radius: 14px; font-weight: 850; cursor: pointer; font-size: 13px; text-transform: uppercase; }
    .btn-danger { background: #dc322f; color: white; border: none; padding: 12px 24px; border-radius: 14px; font-weight: 850; cursor: pointer; font-size: 13px; text-transform: uppercase; letter-spacing: 0.5px; }
    .btn-danger:hover { background: #cb4b16; }

    @keyframes spin { to { transform: rotate(360deg); } }
    @keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.4; } }
</style>
