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
        <h1>Личный кабинет сотрудника</h1>
        {#if $user}
            <p>Привет, <strong>{$user.name}</strong>!</p>
            <p>Твоя роль: Специалист</p>
        {/if}
        <button class="logout-btn" on:click={handleLogout}>Выйти</button>
    </div>
</div>

<style>
    .container {
        padding: 20px;
        display: flex;
        justify-content: center;
        align-items: center;
        min-height: 100vh;
    }
    .card {
        width: 100%;
        max-width: 400px;
        background: white;
        padding: 30px;
        border-radius: 24px;
        box-shadow: 0 10px 30px rgba(0,0,0,0.05);
        text-align: center;
    }
    h1 {
        color: var(--primary-color);
        font-size: 20px;
        margin-bottom: 16px;
    }
    strong {
        color: var(--primary-color);
    }
    .logout-btn {
        margin-top: 24px;
        width: 100%;
        padding: 12px;
        background: #f0f2f5;
        border: none;
        border-radius: 12px;
        color: #707579;
        font-weight: 600;
        cursor: pointer;
    }
</style>
