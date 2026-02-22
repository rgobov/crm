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
    let isConnecting = false;
    let errorMessage = '';
    let unsubscribeWS;
    let isAuthorized = false;
    let connectTimeout;

    onMount(async () => {
        unsubscribeWS = telegramStatusSignal.subscribe(signal => {
            if (signal && signal.status) {
                console.log('📡 Telegram status update:', signal.status);
                status = signal.status;
                isAuthorized = (status === 'CONNECTED' || status.startsWith('FLOOD_WAIT'));

                if (status === 'CONNECTED') {
                    qrCode = '';
                    isConnecting = false;
                    clearTimeout(connectTimeout);
                }

                if (status.startsWith('FLOOD_WAIT')) {
                    waitSeconds = parseInt(status.split('_')[2]) || 300;
                    isConnecting = false;
                    clearTimeout(connectTimeout);
                }

                isLoading = false;
            }
        });

        await fetchCurrentStatus();
    });

    onDestroy(() => {
        if (unsubscribeWS) unsubscribeWS();
        if (connectTimeout) clearTimeout(connectTimeout);
    });

    async function fetchCurrentStatus() {
        isLoading = true;
        try {
            const data = await telegramService.getStatus();
            status = data.status;
            qrCode = data.qrCode || '';
            isAuthorized = (status === 'CONNECTED' || status.startsWith('FLOOD_WAIT'));
        } catch (e) {
            errorMessage = 'Ошибка связи с сервером';
        } finally {
            isLoading = false;
        }
    }

    // УСИЛЕННАЯ ЛОГИКА ПОДКЛЮЧЕНИЯ
    async function handleConnect() {
        if (isConnecting) return;
        isConnecting = true;
        qrCode = '';
        errorMessage = '';

        // Предохранитель на 25 секунд
        connectTimeout = setTimeout(() => {
            if (isConnecting && !qrCode) {
                isConnecting = false;
                errorMessage = 'Сервер долго генерирует QR. Попробуйте нажать Обновить.';
            }
        }, 25000);

        try {
            // Сначала сбрасываем всё старое для надежности
            await telegramService.disconnect();

            // Ждем короткую паузу и стартуем новую сессию
            setTimeout(async () => {
                try {
                    await telegramService.connect();
                    // Проверяем статус через 3 сек, чтобы подтянуть QR
                    setTimeout(async () => {
                        const data = await telegramService.getStatus();
                        if (data.qrCode) qrCode = data.qrCode;
                    }, 3000);
                } catch (innerError) {
                    errorMessage = 'Ошибка при создании сессии';
                    isConnecting = false;
                }
            }, 1000);

        } catch (e) {
            errorMessage = 'Не удалось очистить предыдущую попытку';
            isConnecting = false;
        }
    }

    async function handleRefreshQR() {
        // Кнопка обновить теперь использует ту же усиленную логику
        await handleConnect();
    }

    async function handleDisconnect() {
        if (!confirm('Отключить Telegram? Текущая сессия будет удалена.')) return;
        try {
            isLoading = true;
            await telegramService.disconnect();
            status = 'DISCONNECTED';
            isAuthorized = false;
            qrCode = '';
            await fetchCurrentStatus();
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
            <div class="loading-state" in:fade>
                <div class="spinner"></div>
                <p>Связь с сервером...</p>
            </div>
        {:else}
            <div class="content-wrapper" in:fade={{duration: 200}}>
                <section class="hero-card">
                    <div class="icon-box" class:success={isAuthorized} class:warning={status.startsWith('FLOOD')}>
                        {#if status === 'CONNECTED'}
                            ✓
                        {:else if status.startsWith('FLOOD')}
                            ⏳
                        {:else}
                            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="3">
                                <path d="m22 2-7 20-4-9-9-4Z"/><path d="M22 2 11 13"/>
                            </svg>
                        {/if}
                    </div>
                    <div class="hero-info">
                        <label>КАНАЛ СВЯЗИ</label>
                        <div class="status-row">
                            <span class="status-name">
                                {#if status === 'CONNECTED'}Активен
                                {:else if status.startsWith('FLOOD')}Ожидание
                                {:else}Отключен{/if}
                            </span>
                            <div class="dot" class:online={status === 'CONNECTED'} class:waiting={status.startsWith('FLOOD')}></div>
                        </div>
                    </div>
                </section>

                {#if errorMessage}
                    <div class="error-banner" in:slide>⚠️ {errorMessage}</div>
                {/if}

                <div class="content-stack">
                    {#if status.startsWith('FLOOD')}
                        <div class="card wait-card" in:slide>
                            <label>СИНХРОНИЗАЦИЯ</label>
                            <b>Telegram взял паузу</b>
                            <p>Это штатная проверка безопасности. Она займет несколько минут. Уведомления включатся сами.</p>
                            <div class="progress-bar"><div class="progress-fill"></div></div>
                        </div>
                    {:else if status === 'CONNECTED'}
                        <div class="card active-card" in:scale>
                            <label>ПОДКЛЮЧЕНО</label>
                            <b>Аккаунт готов к работе</b>
                            <p>Уведомления клиентам о записях будут уходить автоматически.</p>
                        </div>
                    {:else if qrCode}
                        <div class="card qr-card" in:slide>
                            <div class="qr-header">
                                <label>АВТОРИЗАЦИЯ</label>
                                <button class="btn-text" on:click={handleRefreshQR} disabled={isConnecting}>
                                    {isConnecting ? '...' : 'ОБНОВИТЬ'}
                                </button>
                            </div>
                            <div class="qr-image-wrap">
                                <img src="data:image/png;base64,{qrCode}" alt="QR" />
                            </div>
                            <p class="hint">Отсканируйте код в приложении Telegram (Настройки → Устройства)</p>
                        </div>
                    {:else}
                        <div class="card connect-card">
                            <label>ПОДКЛЮЧЕНИЕ</label>
                            <p>Привяжите аккаунт Telegram для рассылки уведомлений клиентам.</p>
                            <button class="btn-primary" on:click={handleConnect} disabled={isConnecting}>
                                {#if isConnecting}ПОДОЖДИТЕ...{:else}ПОДКЛЮЧИТЬ АККАУНТ{/if}
                            </button>
                        </div>
                    {/if}
                </div>
            </div>
        {/if}
    </div>

    <footer class="modal-footer">
        <div class="footer-layout">
            {#if isAuthorized && !isLoading}
                <button class="btn-danger-link" on:click={handleDisconnect}>ОТКЛЮЧИТЬ</button>
            {/if}
            <div class="spacer"></div>
            <button class="btn-close-main" on:click={() => dispatch('close')}>ЗАКРЫТЬ</button>
        </div>
    </footer>
</div>

<style>
    .modal-inner { display: flex; flex-direction: column; background: #fdf6e3; width: 100%; border-radius: 24px; overflow: hidden; }
    .modal-header { padding: 18px 24px; display: flex; align-items: center; justify-content: space-between; background: #eee8d5; border-bottom: 1.5px solid #ddd6c1; }
    .header-title { font-weight: 900; color: #073642; font-size: 13px; text-transform: uppercase; letter-spacing: 0.5px; }
    .btn-close-round { background: #fdf6e3; border: 1px solid #ddd6c1; width: 32px; height: 32px; border-radius: 50%; cursor: pointer; color: #586e75; display: flex; align-items: center; justify-content: center; }

    .modal-body { padding: 24px; min-height: 200px; }
    .loading-state { padding: 40px 0; text-align: center; }
    .spinner { width: 32px; height: 32px; border: 3px solid #eee8d5; border-top-color: #268bd2; border-radius: 50%; animation: spin 1s linear infinite; margin: 0 auto; }

    .hero-card { background: #eee8d5; padding: 16px; border-radius: 20px; display: flex; align-items: center; gap: 16px; border: 1.5px solid #ddd6c1; margin-bottom: 16px; }
    .icon-box { width: 48px; height: 48px; background: #fdf6e3; border-radius: 12px; display: flex; align-items: center; justify-content: center; color: #93a1a1; border: 1px solid #ddd6c1; font-size: 20px; }
    .icon-box.success { color: #859900; border-color: #859900; background: rgba(133, 153, 0, 0.1); }
    .icon-box.warning { color: #b58900; border-color: #b58900; }

    label { display: block; font-size: 9px; font-weight: 900; color: #93a1a1; text-transform: uppercase; margin-bottom: 2px; letter-spacing: 1px; }
    .status-name { font-size: 17px; font-weight: 900; color: #073642; }
    .dot { width: 8px; height: 8px; border-radius: 50%; background: #93a1a1; margin-left: 8px; display: inline-block; }
    .dot.online { background: #859900; box-shadow: 0 0 8px #859900; }
    .dot.waiting { background: #b58900; animation: pulse 1.5s infinite; }

    .card { background: #eee8d5; padding: 20px; border-radius: 20px; border: 1.5px solid #ddd6c1; }
    .card b { color: #073642; font-size: 15px; display: block; margin-bottom: 4px; }
    .card p { font-size: 13px; color: #586e75; line-height: 1.4; margin: 0; font-weight: 600; }

    .error-banner { background: rgba(220, 50, 47, 0.1); color: #dc322f; padding: 12px; border-radius: 12px; margin-bottom: 16px; font-size: 12px; font-weight: 700; border: 1px solid rgba(220, 50, 47, 0.2); }

    .qr-card { text-align: center; background: #fdf6e3; }
    .qr-image-wrap { background: white; padding: 12px; border-radius: 16px; display: inline-block; border: 1px solid #ddd6c1; margin: 12px 0; }
    .qr-image-wrap img { width: 180px; height: 180px; display: block; }
    .hint { font-size: 11px !important; color: #93a1a1 !important; }

    .btn-text { background: #eee8d5; border: 1px solid #ddd6c1; padding: 6px 12px; border-radius: 8px; color: #268bd2; font-size: 10px; font-weight: 800; cursor: pointer; }
    .btn-text:disabled { opacity: 0.5; }

    .btn-primary { width: 100%; background: #268bd2; color: white; border: none; padding: 16px; border-radius: 16px; font-weight: 900; cursor: pointer; margin-top: 12px; transition: 0.2s; }
    .btn-primary:hover { background: #2aa198; }
    .btn-primary:disabled { opacity: 0.5; cursor: wait; }

    .progress-bar { height: 4px; background: #ddd6c1; border-radius: 2px; margin-top: 12px; overflow: hidden; }
    .progress-fill { height: 100%; background: #b58900; width: 30%; animation: slide 2s infinite linear; }

    .modal-footer { padding: 16px 24px; background: #eee8d5; border-top: 1.5px solid #ddd6c1; }
    .footer-layout { display: flex; align-items: center; }
    .spacer { flex: 1; }
    .btn-close-main { background: #fdf6e3; color: #586e75; border: 1.5px solid #ddd6c1; padding: 10px 24px; border-radius: 12px; font-weight: 800; cursor: pointer; }
    .btn-danger-link { background: transparent; color: #dc322f; border: none; font-weight: 900; cursor: pointer; font-size: 12px; }

    @keyframes spin { to { transform: rotate(360deg); } }
    @keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.4; } }
    @keyframes slide { from { transform: translateX(-100%); } to { transform: translateX(300%); } }
</style>
