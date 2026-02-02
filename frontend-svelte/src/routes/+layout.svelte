<script>
    import { onMount } from 'svelte';
    import { initAuth } from '$lib/stores/auth.js';
    import '../app.css';

    onMount(() => {
        initAuth();

        if (window.Telegram && window.Telegram.WebApp) {
            const tg = window.Telegram.WebApp;
            tg.expand();
            tg.ready();
            document.documentElement.style.setProperty('--bg-color', tg.backgroundColor || '#f8fafc');
            document.documentElement.style.setProperty('--primary-color', tg.buttonColor || '#3897f0');
        }
    });
</script>

<div class="app-shell">
    <div class="main-viewport">
        <slot />
    </div>
</div>

<style>
    /* ПРИЛОЖЕНИЕ ТЕПЕРЬ АДАПТИВНОЕ */
    .app-shell {
        min-height: 100vh;
        background-color: #f1f5f9;
        display: flex;
        flex-direction: column;
    }

    .main-viewport {
        width: 100%;
        /* УБРАЛИ max-width: 500px */
        margin: 0 auto;
        min-height: 100vh;
        background-color: white;
        display: flex;
        flex-direction: column;
        position: relative;
    }

    /* На ПК добавляем небольшие поля, чтобы контент не лип к краям,
       кроме Таймлайна, который будет растягиваться */
    @media (min-width: 1024px) {
        .app-shell { padding: 0; }
    }
</style>
