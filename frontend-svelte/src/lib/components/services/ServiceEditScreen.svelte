<script>
    import { onMount } from 'svelte';
    import { serviceService } from '$lib/services/serviceService.js';
    import { goto } from '$app/navigation';

    export let serviceId = null;
    const isEditing = !!serviceId;

    let formData = {
        name: '',
        durationInMinutes: 30,
        priceMin: null,
        priceMax: null
    };

    let priceType = 'none';
    let isLoading = isEditing;
    let isSaving = false;
    let tg = null;

    onMount(async () => {
        if (window.Telegram && window.Telegram.WebApp) {
            tg = window.Telegram.WebApp;
            if (tg.BackButton) {
                tg.BackButton.show();
                tg.BackButton.onClick(() => goto('/admin/services'));
            }
        }

        if (isEditing) {
            try {
                const res = await serviceService.getServiceById(serviceId);
                if (res) {
                    formData = { ...res };
                    if (formData.priceMin !== null && formData.priceMin !== undefined) {
                        if (formData.priceMax !== null && formData.priceMax !== undefined) {
                            priceType = 'range';
                        } else {
                            priceType = 'fixed';
                        }
                    } else {
                        priceType = 'none';
                    }
                }
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

        let pMin = null;
        let pMax = null;

        if (priceType === 'fixed') {
            pMin = formData.priceMin;
            if (pMin === null || pMin === undefined || pMin === '') {
                return alert('Введите стоимость услуги');
            }
            pMin = parseInt(pMin);
            if (pMin < 0) return alert('Стоимость не может быть отрицательной');
        } else if (priceType === 'range') {
            pMin = formData.priceMin;
            pMax = formData.priceMax;
            if (pMin === null || pMin === undefined || pMin === '') {
                return alert('Введите минимальную стоимость');
            }
            if (pMax === null || pMax === undefined || pMax === '') {
                return alert('Введите максимальную стоимость');
            }
            pMin = parseInt(pMin);
            pMax = parseInt(pMax);
            if (pMin < 0 || pMax < 0) return alert('Стоимость не может быть отрицательной');
            if (pMax < pMin) return alert('Максимальная цена не может быть меньше минимальной');
        }

        formData.priceMin = pMin;
        formData.priceMax = pMax;

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
            console.error('Ошибка при сохранении услуги:', e);
            const errMsg = e.response?.data?.message || e.message || 'Неизвестная ошибка';
            alert(`Ошибка при сохранении: ${errMsg}`);
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

            <div class="field">
                <label for="price-type">Тип стоимости</label>
                <select id="price-type" bind:value={priceType}>
                    <option value="none">Не указана</option>
                    <option value="fixed">Фиксированная</option>
                    <option value="range">Диапазон цен</option>
                </select>
            </div>

            {#if priceType === 'fixed'}
                <div class="field">
                    <label for="price-min">Стоимость (руб)</label>
                    <input type="number" id="price-min" bind:value={formData.priceMin} min="0" placeholder="Напр: 1500" />
                </div>
            {/if}

            {#if priceType === 'range'}
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 24px;">
                    <div class="field" style="margin-bottom: 0;">
                        <label for="price-min">Цена от (руб)</label>
                        <input type="number" id="price-min" bind:value={formData.priceMin} min="0" placeholder="Напр: 2000" />
                    </div>
                    <div class="field" style="margin-bottom: 0;">
                        <label for="price-max">Цена до (руб)</label>
                        <input type="number" id="price-max" bind:value={formData.priceMax} min="0" placeholder="Напр: 3000" />
                    </div>
                </div>
            {/if}

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
        padding: 10vh 20px 20px;
        overflow-y: auto;
        box-sizing: border-box;
        background: #fdf6e3;
    }
    .edit-screen .form-container { width: 100%; max-width: 480px; }
    .form-container { padding: 32px; background: #eee8d5; border-radius: 24px; border: 1px solid #ddd6c1; }

    @media (max-width: 640px) {
        .edit-screen { padding-top: 5vh; }
        .form-container { padding: 24px 20px; }
    }

    .field { margin-bottom: 24px; }
    label { display: block; font-size: 12px; font-weight: 700; color: #268bd2; margin-bottom: 10px; text-transform: uppercase; }
    input, select { width: 100%; padding: 16px; border: 2px solid #ddd6c1; border-radius: 16px; font-size: 16px; background: #fdf6e3; box-sizing: border-box; outline: none; color: #073642; }
    input::placeholder { color: #93a1a1; }
    input:focus, select:focus { border-color: #268bd2; background: #fdf6e3; }

    .duration-presets { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 32px; }
    .preset-btn { padding: 8px 12px; border-radius: 10px; border: 1px solid #ddd6c1; background: #fdf6e3; font-size: 13px; font-weight: 600; cursor: pointer; color: #073642; }
    .preset-btn:active { background: #eee8d5; }

    .save-btn { width: 100%; padding: 18px; background: #268bd2; color: white; border: none; border-radius: 16px; font-weight: 800; font-size: 16px; cursor: pointer; }
    .save-btn:disabled { opacity: 0.7; }

    .center { display: flex; justify-content: center; padding: 40px; }
    .spinner { width: 30px; height: 30px; border: 3px solid #eee8d5; border-top-color: #268bd2; border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
</style>
