<script>
    import { onMount } from 'svelte';
    import { aiService } from '$lib/services/aiService.js';
    import { goto } from '$app/navigation';

    let config = {
        llm_provider: 'openrouter',
        llm_model: 'openrouter/auto',
        api_key: '',
        stt_provider: 'vosk'
    };

    let customModel = '';

    let knowledge = [];
    let newEntry = { question: '', answer: '', category: 'FAQ' };
    let isLoading = true;
    let isSaving = false;
    let showAddForm = false;
    let error = '';

    $: if (customModel) config.llm_model = customModel;

    onMount(async () => {
        if (window.Telegram && window.Telegram.WebApp && window.Telegram.WebApp.initData) {
            goto('/admin');
            return;
        }
        await loadData();
    });

    async function loadData() {
        isLoading = true;
        error = '';
        try {
            const [cfg, kn] = await Promise.all([
                aiService.getConfig(),
                aiService.getKnowledge()
            ]);
            if (cfg) config = { ...config, ...cfg };
            if (kn) knowledge = kn;
        } catch (e) {
            console.error('Failed to load AI settings', e);
            error = 'Не удалось загрузить настройки';
        } finally {
            isLoading = false;
        }
    }

    async function handleSave() {
        isSaving = true;
        error = '';
        try {
            await aiService.saveConfig(config);
            alert('Настройки AI сохранены');
        } catch (e) {
            error = 'Ошибка: ' + (e.response?.data?.message || 'Не удалось сохранить');
        } finally {
            isSaving = false;
        }
    }

    async function addEntry() {
        if (!newEntry.question || !newEntry.answer) return;
        error = '';
        try {
            await aiService.addKnowledge(newEntry);
            newEntry = { question: '', answer: '', category: 'FAQ' };
            showAddForm = false;
            knowledge = await aiService.getKnowledge();
        } catch (e) {
            error = 'Ошибка: ' + (e.response?.data?.message || 'Не удалось добавить запись');
        }
    }

    async function deleteEntry(id) {
        error = '';
        try {
            await aiService.deleteKnowledge(id);
            knowledge = await aiService.getKnowledge();
        } catch (e) {
            error = 'Ошибка удаления';
        }
    }
</script>

<div class="page">
    <div class="header">
        <button class="back-btn" on:click={() => goto('/admin')}>← Назад</button>
        <h1>AI Настройки</h1>
    </div>

    {#if isLoading}
        <div class="center"><span class="spinner"></span></div>
    {:else}
        {#if error}
            <div class="error-banner">{error}</div>
        {/if}

        <div class="form-container">
            <div class="card config-card">
                <h3>🤖 AI Провайдер</h3>
                <div class="field">
                    <label>Провайдер</label>
                    <select bind:value={config.llm_provider} disabled>
                        <option value="openrouter">OpenRouter</option>
                    </select>
                    <p class="hint">Используется только OpenRouter — доступ к 200+ моделям одним ключом</p>
                </div>
                <div class="field">
                    <label>Модель</label>
                    <select bind:value={config.llm_model}>
                        <option value="openrouter/auto">🤖 OpenRouter Auto (авто-выбор)</option>
                        <optgroup label="🆓 Бесплатные">
                            <option value="meta-llama/llama-3.1-8b-instruct:free">Llama 3.1 8B (free)</option>
                            <option value="google/gemma-2-9b-it:free">Gemma 2 9B (free)</option>
                            <option value="mistralai/mistral-7b-instruct:free">Mistral 7B (free)</option>
                            <option value="microsoft/phi-3-mini-128k-instruct:free">Phi-3 Mini (free)</option>
                            <option value="qwen/qwen-2-7b-instruct:free">Qwen 2 7B (free)</option>
                        </optgroup>
                        <optgroup label="💎 Популярные платные">
                            <option value="openai/gpt-4o">GPT-4o</option>
                            <option value="anthropic/claude-3.5-sonnet">Claude 3.5 Sonnet</option>
                            <option value="google/gemini-pro-1.5">Gemini 1.5 Pro</option>
                            <option value="meta-llama/llama-3.1-405b-instruct">Llama 3.1 405B</option>
                        </optgroup>
                    </select>
                </div>
                <div class="field">
                    <label>Или ввести свой ID модели</label>
                    <input type="text" bind:value={customModel} placeholder="напр. meta-llama/llama-3.1-70b-instruct" />
                    <p class="hint">Полный каталог — <a href="https://openrouter.ai/models" target="_blank">openrouter.ai/models</a>. Бесплатные модели имеют суффикс <code>:free</code></p>
                </div>
                <div class="field">
                    <label>API-ключ</label>
                    <input type="password" bind:value={config.api_key} placeholder="Ваш API-ключ" />
                    <p class="hint">Каждый тенант использует свой ключ. Ключ хранится зашифрованным.</p>
                </div>
                <div class="field">
                    <label>Распознавание речи (STT)</label>
                    <select bind:value={config.stt_provider}>
                        <option value="vosk">Vosk (локальный, бесплатно)</option>
                    </select>
                </div>
            </div>

            <div class="actions">
                <button class="save-btn" on:click={handleSave} disabled={isSaving}>
                    {isSaving ? 'Сохранение...' : 'СОХРАНИТЬ НАСТРОЙКИ'}
                </button>
            </div>

            <div class="section-divider">
                <span>📚 БАЗА ЗНАНИЙ</span>
            </div>

            <div class="card knowledge-card">
                <p class="hint">Вопросы и ответы, которые AI-агент будет использовать при общении с клиентами.</p>

                {#each knowledge as entry}
                    <div class="knowledge-row">
                        <div class="k-content">
                            <b>{entry.question}</b>
                            <p>{entry.answer}</p>
                            <span class="k-category">{entry.category}</span>
                        </div>
                        <button class="delete-btn" on:click={() => deleteEntry(entry.id)}>✕</button>
                    </div>
                {:else}
                    <p class="empty-state">База знаний пуста. Добавьте часто задаваемые вопросы.</p>
                {/each}

                {#if showAddForm}
                    <div class="add-form">
                        <input type="text" bind:value={newEntry.question} placeholder="Вопрос" />
                        <textarea bind:value={newEntry.answer} placeholder="Ответ" rows="3"></textarea>
                        <div class="add-form-actions">
                            <select bind:value={newEntry.category}>
                                <option value="FAQ">FAQ</option>
                                <option value="prices">Цены</option>
                                <option value="rules">Правила</option>
                                <option value="contacts">Контакты</option>
                            </select>
                            <div class="add-btns">
                                <button class="cancel-btn" on:click={() => showAddForm = false}>Отмена</button>
                                <button class="confirm-btn" on:click={addEntry}>Добавить</button>
                            </div>
                        </div>
                    </div>
                {:else}
                    <button class="add-btn" on:click={() => showAddForm = true}>+ Добавить запись</button>
                {/if}
            </div>
        </div>
    {/if}
</div>

<style>
    .page { padding: 20px; max-width: 600px; margin: 0 auto; background: #fdf6e3; min-height: 100vh; }
    .header { display: flex; align-items: center; gap: 16px; margin-bottom: 24px; }
    .back-btn { background: none; border: none; color: #268bd2; font-weight: 700; cursor: pointer; }
    h1 { font-size: 22px; font-weight: 800; margin: 0; color: #073642; }

    .form-container { display: grid; gap: 16px; padding-bottom: 100px; }

    .card { background: #eee8d5; border: 1px solid #ddd6c1; border-radius: 20px; padding: 20px; }

    .error-banner { background: #dc322f; color: white; padding: 12px 16px; border-radius: 12px; font-weight: 600; font-size: 13px; }

    .config-card h3 { font-size: 14px; font-weight: 700; color: #586e75; text-transform: uppercase; margin-bottom: 16px; }
    .field { margin-bottom: 16px; }
    .field:last-child { margin-bottom: 0; }
    .field label { display: block; font-size: 12px; margin-bottom: 6px; color: #586e75; font-weight: 700; }
    input, select, textarea { width: 100%; padding: 14px; border-radius: 14px; border: 1.5px solid #ddd6c1; background: #fdf6e3; font-size: 15px; box-sizing: border-box; outline: none; color: #073642; }
    input::placeholder, textarea::placeholder { color: #93a1a1; }
    input:focus, textarea:focus { border-color: #268bd2; }
    .hint { font-size: 11px; color: #586e75; margin-top: 6px; }

    .actions { margin-top: 8px; }
    .save-btn { width: 100%; background: #268bd2; color: white; border: none; padding: 18px; border-radius: 16px; font-weight: 800; cursor: pointer; font-size: 14px; }
    .save-btn:disabled { opacity: 0.5; }

    .section-divider { text-align: center; margin: 8px 0; }
    .section-divider span { font-size: 10px; font-weight: 900; color: #93a1a1; letter-spacing: 1px; background: #fdf6e3; padding: 0 12px; }

    .knowledge-card { display: grid; gap: 12px; }
    .knowledge-row { display: flex; align-items: flex-start; gap: 12px; background: #fdf6e3; border-radius: 12px; padding: 12px; border: 1px solid #ddd6c1; }
    .k-content { flex: 1; min-width: 0; }
    .k-content b { display: block; font-size: 14px; color: #073642; margin-bottom: 4px; }
    .k-content p { margin: 0; font-size: 13px; color: #586e75; }
    .k-category { display: inline-block; font-size: 9px; font-weight: 800; color: #93a1a1; text-transform: uppercase; letter-spacing: 0.5px; margin-top: 4px; background: #eee8d5; padding: 2px 8px; border-radius: 4px; }
    .delete-btn { background: none; border: none; color: #dc322f; font-size: 18px; cursor: pointer; padding: 4px; flex-shrink: 0; }
    .empty-state { text-align: center; color: #93a1a1; font-size: 13px; padding: 20px; }

    .add-form { display: grid; gap: 10px; background: #fdf6e3; border-radius: 16px; padding: 16px; border: 1.5px dashed #268bd2; }
    .add-form-actions { display: flex; gap: 10px; align-items: center; }
    .add-form-actions select { flex: 1; }
    .add-btns { display: flex; gap: 8px; }
    .cancel-btn { background: #eee8d5; border: none; padding: 10px 16px; border-radius: 10px; font-weight: 700; color: #586e75; cursor: pointer; }
    .confirm-btn { background: #268bd2; color: white; border: none; padding: 10px 16px; border-radius: 10px; font-weight: 700; cursor: pointer; }
    .add-btn { width: 100%; background: none; border: 1.5px dashed #93a1a1; border-radius: 14px; padding: 14px; font-weight: 700; color: #586e75; cursor: pointer; transition: 0.2s; }
    .add-btn:hover { border-color: #268bd2; color: #268bd2; }

    .spinner { width: 30px; height: 30px; border: 3px solid #eee8d5; border-top-color: #268bd2; border-radius: 50%; animation: spin 1s linear infinite; display: inline-block; }
    @keyframes spin { to { transform: rotate(360deg); } }
    .center { text-align: center; padding: 40px; }
</style>