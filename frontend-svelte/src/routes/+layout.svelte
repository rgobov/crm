<script>
    import "../app.css";
    import { onMount } from 'svelte';
    import { page } from '$app/stores';
    import { user, token } from '$lib/stores/auth.js';
    import BottomNav from '$lib/components/BottomNav.svelte';
    import api from '$lib/api.js';

    // Скрываем меню на странице логина
    $: showNav = $page.url.pathname !== '/' && $user !== null;

    onMount(async () => {
        if (window.Telegram && window.Telegram.WebApp) {
            const tg = window.Telegram.WebApp;
            tg.ready();
            document.body.classList.add('tg');
        }

        // Проверка авторизации при загрузке
        const savedToken = localStorage.getItem('token');
        if (savedToken && !$user) {
            token.set(savedToken);
            try {
                const response = await api.get('/auth/me');
                user.set(response.data);
            } catch (e) {
                localStorage.removeItem('token');
            }
        }
    });
</script>

<main class:with-nav={showNav}>
    <slot />
</main>

{#if showNav}
    <BottomNav />
{/if}

<style>
    main {
        min-height: 100vh;
        width: 100%;
        box-sizing: border-box;
    }

    /* Добавляем отступ снизу, чтобы контент не перекрывался меню */
    .with-nav {
        padding-bottom: 80px;
    }
</style>
