<script>
    import { onMount, createEventDispatcher } from 'svelte';
    import api from '$lib/api.js';
    import { fade, scale } from 'svelte/transition';

    const dispatch = createEventDispatcher();

    let templateId = null;
    let content = "Здравствуйте, {client}! Напоминаем о вашей записи на {service}: {date} в {time}.";
    let isLoading = true;
    let isSaving = false;

    const PLACEHOLDERS = [
        { tag: '{client}', label: 'Имя клиента' },
        { tag: '{service}', label: 'Услуга' },
        { tag: '{date}', label: 'Дата' },
        { tag: '{time}', label: 'Время' }
    ];

    onMount(async () => {
        await loadTemplate();
    });

    async function loadTemplate() {
        isLoading = true;
        try {
            const res = await api.get('/admin/notifications/templates');
            const found = res.data.find(t => t.type === 'REMINDER');
            if (found) {
                templateId = found.id;
                content = found.content;
            }
        } catch (e) {
            console.error('Failed to load template', e);
        } finally {
            isLoading = false;
        }
    }

    async function handleSave() {
        isSaving = true;
        try {
            const payload = {
                id: templateId,
                type: 'REMINDER',
                content: content
            };
            await api.post('/admin/notifications/templates', payload);
            alert('Шаблон сохранен');
            dispatch('close');
        } catch (e) {
            alert('Ошибка при сохранении');
        } finally {
            isSaving = false;
        }
    }

    function addPlaceholder(tag) {
        content += tag;
    }
</script>

<div class="modal-inner">
    <header class="modal-header">
        <div class="header-title">Шаблон напоминания</div>
        <button class="btn-close-round" on:click={() => dispatch('close')}>✕</button>
    </header>

    <div class="modal-body">
        {#if isLoading}
            <div class="loading-state"><div class="spinner"></div></div>
        {:else}
            <div class="editor-section" in:fade>
                <label>ТЕКСТ СООБЩЕНИЯ</label>
                <textarea
                    bind:value={content}
                    placeholder="Введите текст сообщения..."
                    rows="10"
                ></textarea>

                <div class="placeholders-box">
                    <span class="ph-label">Добавить данные (нажмите):</span>
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
            {isSaving ? 'СОХРАНЕНИЕ...' : 'СОХРАНИТЬ'}
        </button>
    </footer>
</div>

<style>
    .modal-inner { display: flex; flex-direction: column; background: #f8fafc; height: 100%; }
    .modal-header { padding: 16px 24px; display: flex; align-items: center; justify-content: space-between; background: white; border-bottom: 1px solid #f1f5f9; }
    .header-title { font-weight: 900; color: #1e293b; font-size: 14px; text-transform: uppercase; }
    .btn-close-round { background: #f1f5f9; border: none; width: 30px; height: 30px; border-radius: 50%; cursor: pointer; color: #64748b; }

    .modal-body { padding: 24px; flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 20px; }
    label { display: block; font-size: 10px; font-weight: 900; color: #94a3b8; text-transform: uppercase; margin-bottom: 8px; }

    textarea {
        width: 100%; padding: 16px; border-radius: 20px; border: 1px solid #e2e8f0;
        background: white; font-size: 15px; color: #1e293b; resize: none; line-height: 1.6;
        outline: none; transition: border-color 0.2s;
    }
    textarea:focus { border-color: #0ea5e9; }

    .ph-label { font-size: 11px; color: #94a3b8; display: block; margin-bottom: 8px; font-weight: 700; }
    .ph-grid { display: flex; flex-wrap: wrap; gap: 8px; }
    .ph-tag {
        background: #f1f5f9; border: 1px solid #e2e8f0; padding: 8px 12px; border-radius: 12px;
        font-size: 12px; font-weight: 700; color: #475569; cursor: pointer;
    }
    .ph-tag:hover { background: #e0f2fe; border-color: #0ea5e9; color: #0ea5e9; }

    .modal-footer { padding: 16px 24px; display: flex; justify-content: flex-end; gap: 12px; background: white; border-top: 1px solid #f1f5f9; }
    .btn-primary { background: #0ea5e9; color: white; border: none; padding: 14px 24px; border-radius: 16px; font-weight: 800; cursor: pointer; }
    .btn-secondary { background: #f1f5f9; color: #64748b; border: none; padding: 14px 24px; border-radius: 16px; font-weight: 700; cursor: pointer; }

    .loading-state { display: flex; justify-content: center; padding: 40px; }
    .spinner { width: 24px; height: 24px; border: 3px solid #f1f5f9; border-top-color: #0ea5e9; border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
</style>
