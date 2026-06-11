<script>
  import { onMount } from 'svelte';

  let deferredPrompt = null;
  let showModal = false;
  let isIOS = false;
  let isAlreadyInstalled = false;

  onMount(() => {
    // 1. Проверяем, запущено ли уже приложение как установленное PWA
    isAlreadyInstalled = window.matchMedia('(display-mode: standalone)').matches 
                         || window.navigator.standalone === true;

    if (isAlreadyInstalled) {
      return;
    }

    // 2. Определяем iOS (iPhone/iPad/iPod)
    isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent) && !window.MSStream;

    // 3. Проверяем наличие намерения установки из sessionStorage
    const shouldPrompt = sessionStorage.getItem('pwa-trigger-install') === 'true';

    // 4. Если есть намерение установки — показываем модал в любом случае (имитация)
    if (shouldPrompt) {
      showModal = true;
      if (window.deferredPrompt) {
        deferredPrompt = window.deferredPrompt;
      }
    }

    // 5. Слушаем кастомное событие готовности prompt (если он придет после монтирования)
    const handlePromptReady = () => {
      if (window.deferredPrompt) {
        deferredPrompt = window.deferredPrompt;
      }
    };
    window.addEventListener('pwa-prompt-ready', handlePromptReady);

    // Слушаем стандартное событие на случай прямого захода без ранней загрузки
    const handleBeforeInstallPrompt = (e) => {
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
  <div class="pwa-install-overlay" on:click={handleClose}>
    <div class="pwa-install-card" on:click|stopPropagation>
      <button class="pwa-close-btn" on:click={handleClose} aria-label="Закрыть">
        &times;
      </button>
      
      <div class="pwa-card-header">
        <img src="/icon-192x192.png" alt="999 CRM Logo" class="pwa-app-logo" />
        <div class="pwa-app-meta">
          <h3>999 CRM</h3>
          <p class="pwa-developer">Try Neuro Corp.</p>
          <span class="pwa-verified">
            <svg class="pwa-shield-icon" viewBox="0 0 24 24" width="12" height="12">
              <path fill="#01875f" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
            </svg>
            Проверено Play Защитой
          </span>
        </div>
      </div>
      
      <div class="pwa-stats-grid">
        <div class="pwa-stat-box">
          <span class="pwa-stat-value">4.9 ★</span>
          <span class="pwa-stat-label">120+ отзывов</span>
        </div>
        <div class="pwa-stat-divider"></div>
        <div class="pwa-stat-box">
          <span class="pwa-stat-value">120 КБ</span>
          <span class="pwa-stat-label">Размер</span>
        </div>
        <div class="pwa-stat-divider"></div>
        <div class="pwa-stat-box">
          <span class="pwa-stat-value">3+</span>
          <span class="pwa-stat-label">Для всех</span>
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

      <p class="pwa-disclaimer">Установка приложения займет всего несколько секунд и не расходует память телефона.</p>
    </div>
  </div>
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
</style>
