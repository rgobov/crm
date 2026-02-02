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

<div class="app-viewport">
    <slot />
</div>

<style>
    :global(body) {
        margin: 0;
        padding: 0;
        font-family: 'Inter', -apple-system, sans-serif;
        background-color: #f8fafc;
    }

    .app-viewport {
        min-height: 100vh;
        width: 100%;
        display: flex;
        flex-direction: column;
    }
</style>
