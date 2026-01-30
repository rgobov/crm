<script>
    import { onMount } from 'svelte';
    import { resourceService } from '$lib/services/resourceService.js';
    import { goto } from '$app/navigation';

    export let resourceId = null;
    const isEditing = !!resourceId;

    let formData = {
        name: '',
        description: ''
    };

    let isLoading = isEditing;
    let isSaving = false;
    let tg = null;

    onMount(async () => {
        if (window.Telegram && window.Telegram.WebApp) {
            tg = window.Telegram.WebApp;
            tg.BackButton.show();
            tg.BackButton.onClick(() => goto('/admin/resources'));
        }

        if (isEditing) {
            try {
                const res = await resourceService.getResourceById(resourceId);
                if (res) formData = { ...res };
            } catch (e) {
                console.error('Failed to load resource');
                goto('/admin/resources');
            } finally {
                isLoading = false;
            }
        }
    });

    async function handleSave() {
        if (!formData.name.trim()) return alert('Введите название ресурса');

        isSaving = true;
        try {
            if (isEditing) {
                await resourceService.updateResource(resourceId, formData);
            } else {
                await resourceService.addResource(formData);
            }
            if (tg) tg.HapticFeedback.notificationOccurred('success');
            goto('/admin/resources');
        } catch (e) {
            alert('Ошибка при сохранении');
        } finally {
            isSaving = false;
        }
    }
</script>

<div class="edit-screen">
    {#if isLoading}
        <div class="center"><span class="spinner"></span></div>
    {:else}
        <div class="form-container card">
            <div class="field">
                <label for="name">Название ресурса</label>
                <input type="text" id="name" bind:value={formData.name} placeholder="Напр: Кабинет 1" />
            </div>

            <div class="field">
                <label for="desc">Описание (необязательно)</label>
                <textarea id="desc" bind:value={formData.description} rows="4" placeholder="Детали..."></textarea>
            </div>

            <button class="save-btn" on:click={handleSave} disabled={isSaving}>
                {isSaving ? 'Сохранение...' : 'СОХРАНИТЬ РЕСУРС'}
            </button>
        </div>
    {/if}
</div>

<style>
    .edit-screen { padding: 20px; max-width: 500px; margin: 0 auto; }
    .form-container { padding: 24px; background: white; border-radius: 24px; box-shadow: var(--shadow); }

    .field { margin-bottom: 20px; }
    label { display: block; font-size: 12px; font-weight: 700; color: var(--primary-color); margin-bottom: 8px; text-transform: uppercase; }
    input, textarea { width: 100%; padding: 14px; border: 2px solid #f1f5f9; border-radius: 14px; font-size: 16px; background: #f8fafc; box-sizing: border-box; outline: none; }
    input:focus, textarea:focus { border-color: var(--primary-color); background: white; }

    .save-btn { width: 100%; padding: 18px; background: var(--primary-gradient); color: white; border: none; border-radius: 16px; font-weight: 800; font-size: 16px; margin-top: 20px; cursor: pointer; box-shadow: 0 10px 20px rgba(56, 151, 240, 0.2); }
    .save-btn:disabled { opacity: 0.7; }

    .center { display: flex; justify-content: center; padding: 40px; }
    .spinner { width: 30px; height: 30px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
</style>
