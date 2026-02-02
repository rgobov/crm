<script>
    import { onMount } from 'svelte';
    import { initAuth } from '$lib/stores/auth.js';
    import '../app.css';

    onMount(() => {
        // Инициализируем авторизацию и детекцию TG один раз при запуске
        initAuth();

        if (window.Telegram && window.Telegram.WebApp) {
            const tg = window.Telegram.WebApp;
            tg.expand();
            tg.ready();
            // Подстраиваем цвета под тему Телеграма
            document.documentElement.style.setProperty('--bg-color', tg.backgroundColor || '#f8fafc');
            document.documentElement.style.setProperty('--text-color', tg.textColor || '#0f172a');
            document.documentElement.style.setProperty('--hint-color', tg.hintColor || '#94a3b8');
            document.documentElement.style.setProperty('--primary-color', tg.buttonColor || '#3897f0');
        }
    });
</script>

<div class="app-shell">
    <div class="main-container">
        <slot />
    </div>
</div>

<style>
    /* Глобальный контейнер-оболочка */
    .app-shell {
        min-height: 100vh;
        background-color: #f1f5f9; /* Внешний фон для десктопа */
        display: flex;
        justify-content: center;
    }

    /* Адаптивное ядро приложения (Мобильный вид в центре) */
    .main-container {
        width: 100%;
        max-width: 500px; /* Ограничиваем ширину как у современного приложения */
        min-height: 100vh;
        background-color: white;
        box-shadow: 0 0 40px rgba(0,0,0,0.05);
        display: flex;
        flex-direction: column;
        position: relative;
    }

    /* На очень маленьких экранах убираем тени и внешние отступы */
    @media (max-width: 500px) {
        .app-shell { background-color: white; }
        .main-container { box-shadow: none; }
    }
</style>
