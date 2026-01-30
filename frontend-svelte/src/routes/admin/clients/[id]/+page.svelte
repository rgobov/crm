<script>
    import ContactDetailScreen from '$lib/components/contacts/ContactDetailScreen.svelte';
    import { page } from '$app/stores';
    import { onMount } from 'svelte';
    import { goto } from '$app/navigation';

    const id = $page.params.id;

    onMount(() => {
        if (window.Telegram && window.Telegram.WebApp) {
            window.Telegram.WebApp.BackButton.show();
            window.Telegram.WebApp.BackButton.onClick(() => goto('/admin/clients'));
        }
    });
</script>

<div class="page-wrapper">
    <header class="page-header">
        <button class="back-btn" on:click={() => goto('/admin/clients')}>← К списку</button>
        <h1>Профиль клиента</h1>
        <button class="edit-btn" on:click={() => goto(`/admin/clients/${id}/edit`)}>✎</button>
    </header>

    <ContactDetailScreen contactId={id} />
</div>

<style>
    .page-wrapper { min-height: 100vh; background: var(--bg-color); }

    .page-header {
        display: flex; justify-content: space-between; align-items: center;
        padding: 20px; background: white;
    }

    .back-btn { background: none; border: none; color: var(--primary-color); font-weight: 700; cursor: pointer; }
    h1 { font-size: 18px; font-weight: 800; margin: 0; }
    .edit-btn {
        background: #eff6ff; color: var(--primary-color);
        border: none; width: 40px; height: 40px;
        border-radius: 12px; font-size: 18px;
    }
</style>
