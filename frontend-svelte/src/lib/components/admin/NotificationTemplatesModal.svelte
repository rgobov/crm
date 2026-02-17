<script>
    import { onMount, createEventDispatcher } from 'svelte';
    import api from '$lib/api.js';
    import { fade, scale, slide } from 'svelte/transition';

    const dispatch = createEventDispatcher();

    let templates = [];
    let selectedType = 'APPOINTMENT_CONFIRMATION';
    let currentContent = '';
    let isLoading = true;
    let isSaving = false;

    const TYPES = [
        { id: 'APPOINTMENT_CONFIRMATION', label: 'Подтверждение записи', desc: 'Уходит сразу после создания визита' },
        { id: 'REMINDER_2_HOURS', label: 'Напоминание (2 часа)', desc: 'За 2 часа до начала визита' },
        { id: 'APPOINTMENT_CANCELLED', label: 'Отмена записи', desc: 'Если визит был отменен администратором' }
    ];

    const PLACEHOLDERS = [
        { tag: '{client}', label: 'Имя клиента' },
        { tag: '{service}', label: 'Услуга' },
        { tag: '{date}', label: 'Дата визита' },
        { tag: '{time}', label: 'Время визита' }
    ];

    onMount(async () => {
        await loadTemplates();
    });

    async function loadTemplates() {
        isLoading = true;
        try {
            const res = await api.get('/admin/notifications/templates');
            templates = res.data;
            updateEditor();
        } catch (e) {
            console.error('Failed to load templates', e);
        } finally {
            isLoading = false;
        }
    }

    function updateEditor() {
        const found = templates.find(t => t.type === selectedType);
        if (found) {
            currentContent = found.content;
        } else {
            // Тексты по умолчанию
            if (selectedType === 'APPOINTMENT_CONFIRMATION') currentContent = "Здравствуйте, {client}! Вы записаны на {service} на {date} в {time}.";
            if (selectedType === 'REMINDER_2_HOURS') currentContent = "Напоминаем: ждем вас сегодня в {time} на услугу {service}.";
            if (selectedType === 'APPOINTMENT_CANCELLED') currentContent = "Ваша запись на {service} ({date}, {time}) отменена.";
        }
    }

    async function handleSave() {
        isSaving = true;
        try {
            const template = templates.find(t => t.type === selectedType) || { type: selectedType };
            template.content = currentContent;

            await api.post('/admin/notifications/templates', template);
            await loadTemplates();
            alert('Шаблон сохранен');
        } catch (e) {
            alert('Ошибка при сохранении');
        } finally {
            isSaving = false;
        }
    }

    function addPlaceholder(tag) {
        currentContent += tag;
    }

    $: if (selectedType) updateEditor();
</script>

<div class="modal-inner">
    <header class="modal-header">
        <div class="header-title">Шаблоны сообщений</div>
        <button class="btn-close-round" on:click={() => dispatch('close')}>✕</button>
    </header>

    <div class="modal-body">
        {#if isLoading}
            <div class="loading-state"><div class="spinner"></div></div>
        {:else}
            <!-- ВЫБОР ТИПА -->
            <div class="type-selector">
                <label>ТИП СОБЫТИЯ</label>
                <div class="type-grid">
                    {#each TYPES as type}
                        <button
                            class="type-btn"
                            class:active={selectedType === type.id}
                            on:click={() => selectedType = type.id}>
                            <div class="type-label">{type.label}</div>
                            <div class="type-desc">{type.desc}</div>
                        </button>
                    {/each}
                </div>
            </div>

            <!-- РЕДАКТОР -->
            <div class="editor-section" in:fade>
                <label>ТЕКСТ СООБЩЕНИЯ</label>
                <textarea
                    bind:value={currentContent}
                    placeholder="Введите текст сообщения..."
                    rows="5"
                ></textarea>

                <div class="placeholders-box">
                    <span class="ph-label">Нажмите, чтобы вставить:</span>
                    <div class="ph-grid">
                        {#each PLACEHOLDERS as ph}
                            <button class="ph-tag" on:click={() => addPlaceholder(ph.tag)}>
                                {ph.tag}
                            </button>
                        {/each}
                    </div>
                </div>
            </div>
        {/if}
    </div>

    <footer class="modal-footer">
        <button class="btn-secondary" on:click={() => dispatch('close')}>ОТМЕНА</button>
        <button class="btn-primary" disabled={isSaving} on:click={handleSave}>
            {isSaving ? 'СОХРАНЕНИЕ...' : 'СОХРАНИТЬ ШАБЛОН'}
        </button>
    </footer>
</div>

<style>
    .modal-inner { display: flex; flex-direction: column; background: #f8fafc; height: 100%; }
    .modal-header { padding: 16px 24px; display: flex; align-items: center; justify-content: space-between; background: white; border-bottom: 1px solid #f1f5f9; }
    .header-title { font-weight: 900; color: #1e293b; font-size: 14px; text-transform: uppercase; }
    .btn-close-round { background: #f1f5f9; border: none; width: 30px; height: 30px; border-radius: 50%; cursor: pointer; color: #64748b; }

    .modal-body { padding: 20px; flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 20px; }

    label { display: block; font-size: 9px; font-weight: 900; color: #94a3b8; text-transform: uppercase; margin-bottom: 8px; letter-spacing: 0.5px; }

    .type-grid { display: flex; flex-direction: column; gap: 8px; }
    .type-btn {
        text-align: left; padding: 12px 16px; border-radius: 16px; border: 1px solid #f1f5f9;
        background: white; cursor: pointer; transition: all 0.2s;
    }
    .type-btn.active { border-color: #0ea5e9; background: #f0f9ff; box-shadow: 0 4px 12px rgba(14, 165, 233, 0.1); }
    .type-label { font-size: 13px; font-weight: 800; color: #1e293b; }
    .type-desc { font-size: 11px; color: #94a3b8; margin-top: 2px; }

    textarea {
        width: 100%; padding: 16px; border-radius: 20px; border: 1px solid #e2e8f0;
        background: white; font-size: 14px; color: #1e293b; resize: none; line-height: 1.5;
        outline: none; transition: border-color 0.2s;
    }
    textarea:focus { border-color: #0ea5e9; }

    .placeholders-box { margin-top: 12px; }
    .ph-label { font-size: 11px; color: #94a3b8; display: block; margin-bottom: 8px; }
    .ph-grid { display: flex; flex-wrap: wrap; gap: 6px; }
    .ph-tag {
        background: #f1f5f9; border: none; padding: 6px 10px; border-radius: 10px;
        font-size: 11px; font-weight: 700; color: #64748b; cursor: pointer;
    }
    .ph-tag:hover { background: #e2e8f0; color: #1e293b; }

    .modal-footer { padding: 16px 24px; display: flex; justify-content: flex-end; gap: 12px; background: white; border-top: 1px solid #f1f5f9; }
    .btn-primary { background: #0ea5e9; color: white; border: none; padding: 12px 20px; border-radius: 14px; font-weight: 800; cursor: pointer; font-size: 13px; }
    .btn-secondary { background: #f1f5f9; color: #64748b; border: none; padding: 12px 20px; border-radius: 14px; font-weight: 700; cursor: pointer; font-size: 13px; }

    .loading-state { display: flex; justify-content: center; padding: 40px; }
    .spinner { width: 30px; height: 30px; border: 3px solid #f1f5f9; border-top-color: #0ea5e9; border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
</style>
