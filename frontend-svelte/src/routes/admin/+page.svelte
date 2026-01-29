<script>
    import { user, logout } from '$lib/stores/auth.js';
    import { goto } from '$app/navigation';

    function handleLogout() {
        logout();
        goto('/');
    }
</script>

<div class="container">
    <div class="card">
        <h1>Панель Администратора</h1>
        {#if $user}
            <p>Добро пожаловать, <strong>{$user.name}</strong>!</p>
        {/if}
        <button on:click={handleLogout}>Выйти</button>
    </div>
</div>

<style>
    .container { padding: 20px; }
    h1 { color: var(--primary-color); }
</style>
