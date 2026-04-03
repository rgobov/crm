<script>
    import { onMount } from 'svelte';
    import { serviceService } from '$lib/services/serviceService.js';
    import { goto } from '$app/navigation';

    export let serviceId = null;
    const isEditing = !!serviceId;

    let formData = {
        name: '',
        durationInMinutes: 30
    };

    let isLoading = isEditing;
    let isSaving = false;
    let tg = null;

    onMount(async () => {
        if (window.Telegram && window.Telegram.WebApp) {
            tg = window.Telegram.WebApp;
            tg.BackButton.show();
            tg.BackButton.onClick(() => goto('/admin/services'));
        }

        if (isEditing) {
            try {
                const res = await serviceService.getServiceById(serviceId);
                if (res) formData = { ...res };
            } catch (e) {
                console.error('Failed to load service');
                goto('/admin/services');
            } finally {
                isLoading = false;
            }
        }
    });

    async function handleSave() {
        if (!formData.name.trim()) return alert('Введите название услуги');
        if (formData.durationInMinutes <= 0) return alert('Длительность должна быть больше 0');

        isSaving = true;
        try {
            if (isEditing) {
                await serviceService.updateService(formData);
            } else {
                await serviceService.addService(formData);
            }
            if (tg) tg.HapticFeedback.notificationOccurred('success');
            goto('/admin/services');
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
                <label for="name">Название услуги</label>
                <input type="text" id="name" bind:value={formData.name} placeholder="Напр: Стрижка" />
            </div>

            <div class="field">
                <label for="duration">Длительность (мин)</label>
                <input type="number" id="duration" bind:value={formData.durationInMinutes} min="5" step="5" />
            </div>

            <div class="duration-presets">
                {#each [15, 30, 45, 60, 90, 120] as mins}
                    <button class="preset-btn" on:click={() => formData.durationInMinutes = mins}>
                        {mins}м
                    </button>
                {/each}
            </div>

            <button class="save-btn" on:click={handleSave} disabled={isSaving}>
                {isSaving ? 'Сохранение...' : 'СОХРАНИТЬ УСЛУГУ'}
            </button>
        </div>
    {/if}
</div>

<style>
    .edit-screen {
        min-height: 100vh;
        min-height: 100dvh;
        display: flex;
        align-items: flex-start;
        justify-content: center;
        padding: 18vh 20px 20px;
        overflow-y: auto;
        box-sizing: border-box;
    }
    .edit-screen .form-container { width: 100%; max-width: 480px; }
    .form-container { padding: 32px; background: white; border-radius: 24px; box-shadow: 0 8px 30px rgba(0,0,0,0.08); }

    @media (max-width: 640px) {
        .edit-screen { padding-top: 10vh; }
        .form-container { padding: 24px 20px; }
    }

    .field { margin-bottom: 24px; }
    label { display: block; font-size: 12px; font-weight: 700; color: var(--primary-color); margin-bottom: 10px; text-transform: uppercase; }
    input { width: 100%; padding: 16px; border: 2px solid #f1f5f9; border-radius: 16px; font-size: 16px; background: #f8fafc; box-sizing: border-box; outline: none; }
    input:focus { border-color: var(--primary-color); background: white; }

    .duration-presets { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 32px; }
    .preset-btn { padding: 8px 12px; border-radius: 10px; border: 1px solid #e2e8f0; background: white; font-size: 13px; font-weight: 600; cursor: pointer; }
    .preset-btn:active { background: #f1f5f9; }

    .save-btn { width: 100%; padding: 18px; background: var(--primary-gradient); color: white; border: none; border-radius: 16px; font-weight: 800; font-size: 16px; cursor: pointer; box-shadow: 0 10px 20px rgba(56, 151, 240, 0.2); }
    .save-btn:disabled { opacity: 0.7; }

    .center { display: flex; justify-content: center; padding: 40px; }
    .spinner { width: 30px; height: 30px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
</style>
