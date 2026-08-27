<script>
  import { onMount } from 'svelte';

  let deferredPrompt = null;
  let showModal = false;
  let isIOS = false;
  let isAlreadyInstalled = false;

  onMount(() => {
    console.log('[PWA TEST] PwaInstaller mounted');
    // 1. Проверяем, запущено ли уже приложение как установленное PWA
    isAlreadyInstalled = window.matchMedia('(display-mode: standalone)').matches 
                         || window.navigator.standalone === true;
    console.log('[PWA TEST] isAlreadyInstalled =', isAlreadyInstalled);

    if (isAlreadyInstalled) {
      console.log('[PWA TEST] PWA is already installed, aborting component action');
      return;
    }

    // 2. Определяем iOS (iPhone/iPad/iPod)
    isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent) && !window.MSStream;
    console.log('[PWA TEST] isIOS =', isIOS);

    // 3. Проверяем наличие намерения установки из sessionStorage
    const shouldPrompt = sessionStorage.getItem('pwa-trigger-install') === 'true';
    console.log('[PWA TEST] shouldPrompt (from sessionStorage) =', shouldPrompt);

    // 4. Если есть намерение установки — показываем модал в любом случае (имитация)
    if (shouldPrompt) {
      showModal = true;
      console.log('[PWA TEST] showModal set to true');
      if (window.deferredPrompt) {
        deferredPrompt = window.deferredPrompt;
        console.log('[PWA TEST] deferredPrompt retrieved from window');
      }
    }

    // 5. Слушаем кастомное событие готовности prompt (если он придет после монтирования)
    const handlePromptReady = () => {
      console.log('[PWA TEST] pwa-prompt-ready fired');
      if (window.deferredPrompt) {
        deferredPrompt = window.deferredPrompt;
        console.log('[PWA TEST] deferredPrompt updated from window via custom event');
      }
    };
    window.addEventListener('pwa-prompt-ready', handlePromptReady);

    // Слушаем стандартное событие на случай прямого захода без ранней загрузки
    const handleBeforeInstallPrompt = (e) => {
      console.log('[PWA TEST] beforeinstallprompt event captured directly in Svelte');
      e.preventDefault();
      deferredPrompt = e;
      window.deferredPrompt = e;
    };
    window.addEventListener('beforeinstallprompt', handleBeforeInstallPrompt);

    return () => {
      window.removeEventListener('pwa-prompt-ready', handlePromptReady);
      window.removeEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
    };
  });

  async function handleInstall() {
    if (isIOS) {
      alert("Для установки приложения на iPhone:\n\n1. Нажмите кнопку «Поделиться» (иконка квадрата со стрелкой вверх внизу экрана).\n2. Прокрутите меню и выберите «На экран \"Домой\"».\n3. Нажмите «Добавить» в правом верхнем углу.");
      showModal = false;
      sessionStorage.removeItem('pwa-trigger-install');
      return;
    }

    // Если prompt появился пока висел модал, забираем его
    if (!deferredPrompt && window.deferredPrompt) {
      deferredPrompt = window.deferredPrompt;
    }

    if (!deferredPrompt) {
      alert("Браузер подготавливает установку. Пожалуйста, подождите 2-3 секунды и нажмите кнопку снова. \n\nЕсли это не сработает, вы можете установить приложение вручную: нажмите на три точки в верхнем углу браузера и выберите «Установить» (или «Добавить на главный экран»).");
      return;
    }

    // Сбрасываем триггер, чтобы не донимать пользователя при следующей сессии
    sessionStorage.removeItem('pwa-trigger-install');

    // Показываем стандартный диалог
    deferredPrompt.prompt();
    const { outcome } = await deferredPrompt.userChoice;
    console.log(`PWA Install Choice: ${outcome}`);
    deferredPrompt = null;
    window.deferredPrompt = null;
    showModal = false;
  }

  function handleClose() {
    showModal = false;
    sessionStorage.removeItem('pwa-trigger-install');
  }
</script>

{#if showModal}
  {#if isIOS}
    <div class="ios-install-overlay" on:click={handleClose}>
      <div class="ios-install-sheet" on:click|stopPropagation>
        <button class="ios-close-btn" on:click={handleClose} aria-label="Закрыть">
          &times;
        </button>
        
        <div class="ios-header">
          <img src="/icon-192x192.png" alt="999 CRM Logo" class="ios-app-logo" />
          <div class="ios-app-meta">
            <h3>999 CRM</h3>
            <p>Система записи клиентов</p>
          </div>
        </div>
        
        <div class="ios-divider"></div>
        
        <h4 class="ios-instruction-title">Установка на iPhone / iPad</h4>
        
        <div class="ios-steps">
          <div class="ios-step">
            <span class="ios-step-num">1</span>
            <p>Нажмите кнопку <strong>«Поделиться»</strong> <span class="ios-icon-wrapper"><svg viewBox="0 0 24 24" class="ios-share-svg"><path d="M12 3a1 1 0 0 1 1 1v12a1 1 0 0 1-2 0V4a1 1 0 0 1 1-1zm6 8a1 1 0 0 1 1 1v7a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-7a1 1 0 0 1 2 0v7h10v-7a1 1 0 0 1 1-1zM9.29 6.71a1 1 0 0 1 0-1.42l2-2a1 1 0 0 1 1.42 0l2 2a1 1 0 0 1-1.42 1.42L12 5.41l-1.29 1.3a1 1 0 0 1-1.42 0z"/></svg></span> в панели Safari.</p>
          </div>
          <div class="ios-step">
            <span class="ios-step-num">2</span>
            <p>Прокрутите меню вниз и выберите <strong>«На экран "Домой"»</strong> <span class="ios-icon-wrapper"><svg viewBox="0 0 24 24" class="ios-plus-svg"><path d="M19 11h-6V5a1 1 0 0 0-2 0v6H5a1 1 0 0 0 0 2h6v6a1 1 0 0 0 2 0v-6h6a1 1 0 0 0 0-2z"/></svg></span>.</p>
          </div>
          <div class="ios-step">
            <span class="ios-step-num">3</span>
            <p>Нажмите <strong>«Добавить»</strong> в правом верхнем углу.</p>
          </div>
        </div>
        
        <div class="ios-pointer"></div>
      </div>
    </div>
  {:else}
    <div class="pwa-install-overlay" on:click={handleClose}>
      <div class="pwa-install-card" on:click|stopPropagation>
        <button class="pwa-close-btn" on:click={handleClose} aria-label="Закрыть">
          &times;
        </button>
        
        <div class="pwa-card-header">
          <img src="/icon-192x192.png" alt="Логотип 999 CRM" class="pwa-app-logo" />
          <div class="pwa-app-meta">
            <h3>999 CRM</h3>
            <p class="pwa-developer">Система записи клиентов</p>
          </div>
        </div>

        <button class="pwa-primary-btn" on:click={handleInstall}>
          Установить
        </button>

        <div class="pwa-screenshots-section">
          <h4 class="pwa-section-title">О приложении</h4>
          <div class="pwa-screenshots-container">
            <div class="pwa-screenshot">
              <div class="pwa-screenshot-mock">
                <span class="pwa-mock-tag">Панель записи</span>
                <div class="pwa-mock-content">
                  <div class="pwa-mock-line"></div>
                  <div class="pwa-mock-line short"></div>
                </div>
              </div>
            </div>
            <div class="pwa-screenshot">
              <div class="pwa-screenshot-mock">
                <span class="pwa-mock-tag">Календарь</span>
                <div class="pwa-mock-content">
                  <div class="pwa-mock-line"></div>
                  <div class="pwa-mock-line short"></div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <p class="pwa-disclaimer">Приложение устанавливается на главный экран устройства и использует защищенное соединение.</p>
      </div>
    </div>
  {/if}
{/if}

<style>
  .pwa-install-overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    background-color: rgba(0, 0, 0, 0.5);
    backdrop-filter: blur(4px);
    z-index: 99999;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 16px;
    box-sizing: border-box;
  }

  .pwa-install-card {
    background-color: #ffffff;
    border-radius: 28px;
    width: 100%;
    max-width: 380px;
    padding: 24px;
    box-sizing: border-box;
    position: relative;
    box-shadow: 0 16px 40px rgba(0, 0, 0, 0.2);
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    color: #202124;
    text-align: left;
    animation: scaleIn 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
  }

  @keyframes scaleIn {
    from {
      transform: scale(0.9);
      opacity: 0;
    }
    to {
      transform: scale(1);
      opacity: 1;
    }
  }

  .pwa-close-btn {
    position: absolute;
    top: 16px;
    right: 16px;
    background: none;
    border: none;
    font-size: 28px;
    color: #5f6368;
    cursor: pointer;
    line-height: 1;
    padding: 4px;
  }

  .pwa-close-btn:hover {
    color: #202124;
  }

  .pwa-card-header {
    display: flex;
    align-items: center;
    margin-top: 8px;
    margin-bottom: 24px;
  }

  .pwa-app-logo {
    width: 68px;
    height: 68px;
    border-radius: 16px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    object-fit: cover;
    margin-right: 16px;
  }

  .pwa-app-meta h3 {
    margin: 0 0 2px 0;
    font-size: 20px;
    font-weight: 500;
    color: #202124;
  }

  .pwa-developer {
    margin: 0 0 4px 0;
    font-size: 14px;
    color: #01875f;
    font-weight: 500;
  }

  .pwa-verified {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 11px;
    color: #5f6368;
  }

  .pwa-shield-icon {
    display: inline-block;
  }

  .pwa-stats-grid {
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-top: 1px solid #e8eaed;
    border-bottom: 1px solid #e8eaed;
    padding: 12px 4px;
    margin-bottom: 24px;
  }

  .pwa-stat-box {
    display: flex;
    flex-direction: column;
    align-items: center;
    flex: 1;
  }

  .pwa-stat-value {
    font-size: 14px;
    font-weight: 700;
    color: #202124;
  }

  .pwa-stat-label {
    font-size: 11px;
    color: #5f6368;
    margin-top: 2px;
    text-align: center;
  }

  .pwa-stat-divider {
    width: 1px;
    height: 24px;
    background-color: #e8eaed;
  }

  .pwa-primary-btn {
    width: 100%;
    background-color: #01875f;
    color: #ffffff;
    border: none;
    border-radius: 100px;
    padding: 12px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    text-align: center;
    transition: background-color 0.2s, box-shadow 0.2s;
    box-shadow: 0 2px 4px rgba(1, 135, 95, 0.2);
    margin-bottom: 24px;
  }

  .pwa-primary-btn:hover {
    background-color: #00704e;
    box-shadow: 0 4px 8px rgba(1, 135, 95, 0.3);
  }

  .pwa-primary-btn:active {
    background-color: #005a3e;
  }

  .pwa-screenshots-section {
    margin-bottom: 16px;
  }

  .pwa-section-title {
    margin: 0 0 10px 0;
    font-size: 14px;
    font-weight: 500;
    color: #202124;
  }

  .pwa-screenshots-container {
    display: flex;
    gap: 12px;
    overflow-x: auto;
    padding-bottom: 8px;
  }

  .pwa-screenshot {
    flex: 0 0 calc(50% - 6px);
  }

  .pwa-screenshot-mock {
    background-color: #f1f3f4;
    border-radius: 12px;
    padding: 12px;
    height: 100px;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    box-sizing: border-box;
    border: 1px solid #e8eaed;
  }

  .pwa-mock-tag {
    font-size: 10px;
    font-weight: 600;
    color: #5f6368;
    background: #ffffff;
    padding: 2px 6px;
    border-radius: 4px;
    width: max-content;
  }

  .pwa-mock-content {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .pwa-mock-line {
    height: 6px;
    background-color: #dadce0;
    border-radius: 3px;
    width: 80%;
  }

  .pwa-mock-line.short {
    width: 50%;
  }

  .pwa-disclaimer {
    font-size: 11px;
    color: #5f6368;
    margin: 0;
    line-height: 1.4;
    text-align: center;
  }

  /* iOS specific styles */
  .ios-install-overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    background-color: rgba(0, 0, 0, 0.4);
    backdrop-filter: blur(8px);
    -webkit-backdrop-filter: blur(8px);
    z-index: 99999;
    display: flex;
    align-items: flex-end;
    justify-content: center;
    padding: 16px;
    box-sizing: border-box;
  }

  .ios-install-sheet {
    background-color: rgba(255, 255, 255, 0.95);
    border-radius: 20px;
    width: 100%;
    max-width: 380px;
    padding: 24px 20px;
    box-sizing: border-box;
    position: relative;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    color: #000000;
    text-align: left;
    margin-bottom: 24px;
    animation: iosSlideUp 0.35s cubic-bezier(0.25, 1, 0.5, 1);
  }

  @keyframes iosSlideUp {
    from {
      transform: translateY(100px);
      opacity: 0;
    }
    to {
      transform: translateY(0);
      opacity: 1;
    }
  }

  .ios-close-btn {
    position: absolute;
    top: 14px;
    right: 14px;
    background: #e5e5ea;
    border: none;
    font-size: 18px;
    font-weight: bold;
    color: #8e8e93;
    cursor: pointer;
    width: 26px;
    height: 26px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0;
  }

  .ios-header {
    display: flex;
    align-items: center;
    margin-bottom: 16px;
  }

  .ios-app-logo {
    width: 56px;
    height: 56px;
    border-radius: 12px;
    box-shadow: 0 4px 10px rgba(0, 0, 0, 0.08);
    margin-right: 14px;
    object-fit: cover;
  }

  .ios-app-meta h3 {
    margin: 0;
    font-size: 19px;
    font-weight: 600;
    letter-spacing: -0.5px;
  }

  .ios-app-meta p {
    margin: 2px 0 0 0;
    font-size: 13px;
    color: #8e8e93;
  }

  .ios-divider {
    height: 1px;
    background-color: rgba(0, 0, 0, 0.1);
    margin: 0 0 16px 0;
  }

  .ios-instruction-title {
    margin: 0 0 16px 0;
    font-size: 13px;
    font-weight: 600;
    text-transform: uppercase;
    color: #8e8e93;
    letter-spacing: 0.5px;
  }

  .ios-steps {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .ios-step {
    display: flex;
    align-items: flex-start;
  }

  .ios-step-num {
    background-color: #007aff;
    color: white;
    width: 20px;
    height: 20px;
    border-radius: 50%;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    font-weight: 600;
    margin-right: 12px;
    flex-shrink: 0;
    margin-top: 1px;
  }

  .ios-step p {
    margin: 0;
    font-size: 14px;
    line-height: 1.4;
    color: #1c1c1e;
  }

  .ios-icon-wrapper {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    background-color: #e5e5ea;
    border-radius: 4px;
    padding: 2px 4px;
    vertical-align: middle;
  }

  .ios-share-svg, .ios-plus-svg {
    width: 16px;
    height: 16px;
    fill: #007aff;
  }

  .ios-pointer {
    position: absolute;
    bottom: -10px;
    left: 50%;
    transform: translateX(-50%);
    width: 0;
    height: 0;
    border-left: 10px solid transparent;
    border-right: 10px solid transparent;
    border-top: 10px solid rgba(255, 255, 255, 0.95);
  }
</style>
