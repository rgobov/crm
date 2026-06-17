<script>
    import ContactDetailScreen from '$lib/components/contacts/ContactDetailScreen.svelte';
    import { page } from '$app/stores';
    import { onMount } from 'svelte';
    import { goto } from '$app/navigation';

    const id = $page.params.id;
    let clientName = 'Профиль клиента'; // Заголовок по умолчанию

    function handleLoaded(event) {
        // Устанавливаем реальное имя клиента из компонента
        clientName = event.detail.name;
    }

    onMount(() => {
        if (window.Telegram && window.Telegram.WebApp && window.Telegram.WebApp.BackButton) {
            window.Telegram.WebApp.BackButton.show();
            window.Telegram.WebApp.BackButton.onClick(() => goto('/admin/clients'));
        }
    });
</script>

<div class="page-wrapper">
    <header class="page-header">
        <button class="back-btn" on:click={() => goto('/admin/clients')}>←</button>
        <h1>{clientName}</h1>
        <button class="edit-btn" on:click={() => goto(`/admin/clients/${id}/edit`)}>✎</button>
    </header>

    <ContactDetailScreen contactId={id} on:loaded={handleLoaded} />
</div>

<style>
    .page-wrapper { min-height: 100vh; background: var(--bg-color); }

    .page-header {
        display: flex; justify-content: space-between; align-items: center;
        padding: 16px 20px; background: white; border-bottom: 1px solid #f1f5f9;
        position: sticky; top: 0; z-index: 10;
    }

    .back-btn { background: none; border: none; color: var(--primary-color); font-size: 20px; cursor: pointer; font-weight: 700; }
    h1 { font-size: 18px; font-weight: 800; margin: 0; color: #0f172a; flex: 1; text-align: center; }
    .edit-btn {
        background: #eff6ff; color: var(--primary-color);
        border: none; width: 40px; height: 40px;
        border-radius: 12px; font-size: 18px; cursor: pointer;
    }
</style>
