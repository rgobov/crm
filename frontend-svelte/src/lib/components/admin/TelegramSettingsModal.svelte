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

    onMount(async () => {
        // Подписываемся на сигнал из основного вебсокета
        unsubscribeWS = telegramStatusSignal.subscribe(signal => {
            if (signal && signal.ts > 0) {
                console.log('⚡ Signal received, refreshing status in 500ms...');
                // Небольшая задержка, чтобы бэкенд успел обновить стейт
                setTimeout(refreshStatus, 500);
            }
        });

        await refreshStatus();

        if (status === 'DISCONNECTED' || (status === 'INITIALIZING' && !qrCode)) {
            await handleConnect();
        }
    });

    onDestroy(() => {
        if (unsubscribeWS) unsubscribeWS();
    });

    async function refreshStatus() {
        try {
            console.log('Requesting status...');
            const data = await telegramService.getStatus();
            console.log('Status received:', data);

            status = data.status;
            qrCode = data.qrCode;
            errorMessage = '';
        } catch (e) {
            console.error('Refresh status error', e);
            errorMessage = 'Ошибка связи с сервером';
        } finally {
            isLoading = false;
        }
    }

    async function handleConnect() {
        isLoading = true;
        try {
            await telegramService.connect();
            // Даем время на старт
            setTimeout(refreshStatus, 2000);
        } catch (e) {
            errorMessage = 'Не удалось запустить подключение';
        }
    }

    async function handleDisconnect() {
        if (!confirm('Отключить Telegram? Это удалит текущую сессию.')) return;
        try {
            isLoading = true;
            await telegramService.disconnect();
            // Сбрасываем локально
            status = 'INITIALIZING';
            qrCode = '';
            setTimeout(refreshStatus, 1500);
        } catch (e) {
            alert('Ошибка при отключении');
            isLoading = false;
        }
    }
</script>

<div class="appt-edit-root">
    <header class="modal-header">
        <button class="btn-close-icon" on:click={() => dispatch('close')}>✕</button>
        <div class="header-title">Настройки уведомлений</div>
    </header>

    <div class="tiles-layout" in:fade>

        <section class="tile-hero">
            <div class="avatar {status === 'CONNECTED' ? 'bg-success' : ''}">
                {#if status === 'CONNECTED'}
                    ✓
                {:else}
                    <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="3">
                        <path d="m22 2-7 20-4-9-9-4Z"/><path d="M22 2 11 13"/>
                    </svg>
                {/if}
            </div>
            <div class="hero-body">
                <label>ИНТЕГРАЦИЯ</label>
                <div style="display: flex; align-items: center; gap: 8px;">
                    <h2 style="margin:0; font-size: 20px; font-weight: 900;">Telegram</h2>
                    <div class="dot {status === 'CONNECTED' ? 'online' : 'offline'}"></div>
                </div>
            </div>
        </section>

        <div class="tiles-stack">
            {#if errorMessage}
                <div class="tile-card error-card" in:slide>
                    <p>⚠️ {errorMessage}</p>
                    <button class="btn-retry" on:click={refreshStatus}>Повторить</button>
                </div>
            {/if}

            {#if status === 'CONNECTED'}
                <div class="tile-card success-card" in:scale={{start: 0.95}}>
                    <label>СТАТУС</label>
                    <div class="status-box">
                        <div class="badge">АКТИВНО</div>
                        <div class="text">
                            <b>Аккаунт подключен</b>
                            <p>Система готова к отправке автоматических уведомлений.</p>
                        </div>
                    </div>
                </div>
            {:else if qrCode}
                <div class="tile-card qr-section" in:slide>
                    <label>АВТОРИЗАЦИЯ</label>
                    <p class="instr">Отсканируйте код через камеру в Telegram (Настройки → Устройства)</p>
                    <div class="qr-frame">
                        <img src="data:image/png;base64,{qrCode}" alt="QR" />
                    </div>
                </div>
            {:else if !errorMessage}
                <div class="tile-card loading-card">
                    <span class="spinner-small"></span>
                    <p>{isLoading ? 'Загрузка...' : 'Подготовка сессии...'}</p>
                    <span class="sub-text">Ожидаем ответа от серверов Telegram</span>
                </div>
            {/if}

            <div class="tile-card info-card">
                <label>СПРАВКА</label>
                <p class="small-text">Если QR-код не появляется более 20 секунд, попробуйте нажать кнопку «СБРОСИТЬ» ниже.</p>
            </div>
        </div>

        <div class="footer-actions">
            {#if status === 'CONNECTED' || qrCode}
                <button class="btn-danger-lite" on:click={handleDisconnect}>
                    {status === 'CONNECTED' ? 'ОТКЛЮЧИТЬ' : 'СБРОСИТЬ СЕССИЮ'}
                </button>
            {/if}
            <button class="btn-secondary" on:click={() => dispatch('close')}>ЗАКРЫТЬ</button>
        </div>
    </div>
</div>

<style>
    .appt-edit-root { height: 100%; display: flex; flex-direction: column; background: #f8fafc; }

    .modal-header { padding: 20px; display: flex; align-items: center; background: white; border-bottom: 1px solid #f1f5f9; }
    .btn-close-icon { background: #f1f5f9; border: none; width: 32px; height: 32px; border-radius: 50%; cursor: pointer; color: #64748b; margin-right: 16px; }
    .header-title { font-weight: 800; color: #1e293b; font-size: 16px; }

    .tiles-layout { padding: 24px; flex: 1; overflow-y: auto; max-width: 500px; margin: 0 auto; width: 100%; }

    .tile-hero { background: white; padding: 24px; border-radius: 32px; display: flex; align-items: center; gap: 20px; box-shadow: 0 10px 30px rgba(0,0,0,0.03); border: 1px solid #f1f5f9; margin-bottom: 16px; }
    .avatar { width: 64px; height: 64px; background: #f0f9ff; border-radius: 22px; display: flex; align-items: center; justify-content: center; font-size: 24px; color: #0ea5e9; }
    .avatar.bg-success { background: #f0fdf4; color: #22c55e; }

    .hero-body { flex: 1; }
    label { display: block; font-size: 10px; font-weight: 900; color: #94a3b8; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 4px; }

    .dot { width: 8px; height: 8px; border-radius: 50%; }
    .dot.online { background: #22c55e; box-shadow: 0 0 10px #22c55e; }
    .dot.offline { background: #cbd5e1; }

    .tiles-stack { display: flex; flex-direction: column; gap: 12px; }
    .tile-card { background: white; padding: 20px; border-radius: 28px; border: 1px solid #f1f5f9; }

    .status-box { display: flex; flex-direction: column; gap: 12px; margin-top: 8px; }
    .badge { background: #f0fdf4; color: #166534; font-size: 10px; font-weight: 900; padding: 4px 8px; border-radius: 6px; width: fit-content; }
    .text b { font-size: 16px; color: #1e293b; display: block; }
    .text p { margin: 4px 0 0 0; font-size: 13px; color: #64748b; line-height: 1.4; }

    .qr-section { text-align: center; }
    .instr { font-size: 12px; color: #64748b; margin: 8px 0 16px 0; line-height: 1.4; }
    .qr-frame { background: #f8fafc; padding: 20px; border-radius: 24px; display: inline-block; }
    .qr-frame img { width: 200px; height: 200px; mix-blend-mode: multiply; }

    .loading-card { text-align: center; padding: 40px 20px; }
    .sub-text { font-size: 11px; color: #94a3b8; display: block; margin-top: 8px; }

    .info-card { background: #f0f9ff; border-color: #e0f2fe; }
    .small-text { font-size: 12px; color: #0369a1; line-height: 1.5; margin-top: 4px; }

    .footer-actions { display: flex; flex-direction: column; gap: 12px; margin-top: 32px; padding-bottom: 40px; }
    .btn-danger-lite { background: #fff1f2; color: #ef4444; border: none; padding: 18px; border-radius: 24px; font-weight: 800; cursor: pointer; }
    .btn-secondary { background: white; color: #64748b; border: 1.5px solid #e2e8f0; padding: 18px; border-radius: 24px; font-weight: 700; cursor: pointer; }

    .spinner-small { width: 24px; height: 24px; border: 3px solid #f1f5f9; border-top-color: #0ea5e9; border-radius: 50%; animation: spin 1s linear infinite; display: block; margin: 0 auto 10px; }
    @keyframes spin { to { transform: rotate(360deg); } }
</style>
