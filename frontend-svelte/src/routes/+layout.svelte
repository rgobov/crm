<script>
    import "../app.css";
    import { onMount } from 'svelte';

    onMount(() => {
        // Инициализация Telegram WebApp
        if (window.Telegram && window.Telegram.WebApp) {
            const tg = window.Telegram.WebApp;
            tg.ready();

            // Добавляем класс 'tg' к body, чтобы применились стили из app.css
            document.body.classList.add('tg');

            // Устанавливаем цвета заголовка под тему TG
            tg.setHeaderColor(tg.themeParams.header_bg_color || 'secondary_bg_color');
            tg.setBackgroundColor(tg.themeParams.bg_color || 'bg_color');
        }
    });
</script>

<slot />
