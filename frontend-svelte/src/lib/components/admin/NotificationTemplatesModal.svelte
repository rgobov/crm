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
    .modal-inner { display: flex; flex-direction: column; background: #fdf6e3; width: 100%; border-radius: 28px; overflow: hidden; border: 1.5px solid #ddd6c1; }
    .modal-header { padding: 18px 24px; display: flex; align-items: center; justify-content: space-between; background: #eee8d5; border-bottom: 1.5px solid #ddd6c1; }
    .header-title { font-weight: 850; color: #073642; font-size: 16px; text-transform: uppercase; margin: 0; letter-spacing: 0.5px; }
    .btn-close-round { background: #fdf6e3; border: 1px solid #ddd6c1; width: 32px; height: 32px; border-radius: 50%; cursor: pointer; color: #586e75; display: flex; align-items: center; justify-content: center; font-weight: 800; }

    .modal-body { padding: 24px; flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 20px; }
    label { display: block; font-size: 10px; font-weight: 850; color: #93a1a1; text-transform: uppercase; margin-bottom: 8px; letter-spacing: 0.5px; }

    textarea {
        width: 100%; padding: 18px; border-radius: 22px; border: 2px solid #ddd6c1;
        background: white; font-size: 16px; color: #073642; resize: none; line-height: 1.6;
        outline: none; transition: border-color 0.2s; font-weight: 600;
        box-sizing: border-box;
    }
    textarea:focus { border-color: #268bd2; }

    .placeholders-box { background: #eee8d5; padding: 20px; border-radius: 22px; border: 1.5px solid #ddd6c1; }
    .ph-label { font-size: 11px; color: #586e75; display: block; margin-bottom: 12px; font-weight: 800; text-transform: uppercase; }
    .ph-grid { display: flex; flex-wrap: wrap; gap: 10px; }
    .ph-tag {
        background: #fdf6e3; border: 1.5px solid #ddd6c1; padding: 10px 14px; border-radius: 14px;
        font-size: 13px; font-weight: 800; color: #586e75; cursor: pointer; transition: all 0.2s;
    }
    .ph-tag:hover { background: #268bd2; border-color: #268bd2; color: white; transform: translateY(-2px); }

    .modal-footer { padding: 18px 24px; display: flex; justify-content: flex-end; gap: 12px; background: #eee8d5; border-top: 1.5px solid #ddd6c1; }
    .btn-primary { background: #268bd2; color: white; border: none; padding: 16px 28px; border-radius: 18px; font-weight: 900; cursor: pointer; font-size: 14px; text-transform: uppercase; letter-spacing: 0.5px; transition: all 0.2s; }
    .btn-primary:active { transform: scale(0.98); }
    .btn-secondary { background: #fdf6e3; color: #586e75; border: 1.5px solid #ddd6c1; padding: 16px 28px; border-radius: 18px; font-weight: 850; cursor: pointer; font-size: 13px; text-transform: uppercase; }

    .loading-state { display: flex; justify-content: center; padding: 40px; }
    .spinner { width: 32px; height: 32px; border: 3px solid #eee8d5; border-top-color: #268bd2; border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
</style>
