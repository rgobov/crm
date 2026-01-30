<script>
    import ContactEditScreen from '$lib/components/contacts/ContactEditScreen.svelte';
    import { goto } from '$app/navigation';
</script>

<div class="page-wrapper">
    <header class="page-header">
        <button class="back-btn" on:click={() => goto('/admin/clients')}>✕</button>
        <h1>Новый клиент</h1>
    </header>

    <div class="container">
        <ContactEditScreen />
    </div>
</div>

<style>
    .page-wrapper { min-height: 100vh; background: var(--bg-color); }
    .page-header { display: flex; align-items: center; gap: 20px; padding: 20px; background: white; border-bottom: 1px solid #f1f5f9; }
    .back-btn { background: none; border: none; font-size: 20px; color: #94a3b8; cursor: pointer; }
    h1 { font-size: 18px; font-weight: 800; margin: 0; }
    .container { padding: 20px; }
</style>
