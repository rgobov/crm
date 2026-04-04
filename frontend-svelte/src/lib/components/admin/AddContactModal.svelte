<script>
    import { createEventDispatcher } from 'svelte';
    import { contactService } from '$lib/services/contactService.js';
    import { fade, scale } from 'svelte/transition';
    import { portal } from '$lib/actions/portal.js';

    const dispatch = createEventDispatcher();

    let client = {
        name: '',
        phones: [''],
        email: '',
        notes: ''
    };

    let isSaving = false;

    async function handleSave() {
        if (!client.name.trim()) return alert('Введите имя клиента');
        if (!client.phones[0].trim()) return alert('Введите номер телефона');

        isSaving = true;
        try {
            client.phones = client.phones.map(p => p.replace(/\D/g, ''));
            const result = await contactService.addContact(client);
            dispatch('success', result);
        } catch (e) {
            alert('Ошибка при сохранении. Возможно, номер уже занят.');
        } finally {
            isSaving = false;
        }
    }

    function addPhoneField() {
        client.phones = [...client.phones, ''];
    }
</script>

<div class="modal-backdrop" use:portal on:click|self={() => dispatch('close')} transition:fade={{duration: 200}}>
    <div class="modal-content" transition:scale={{start: 0.9, duration: 200}}>
        <div class="modal-header">
            <h2>Новый клиент</h2>
            <button class="close-btn" on:click={() => dispatch('close')}>✕</button>
        </div>

        <div class="modal-body">
            <div class="form-group">
                <label>ФИО КЛИЕНТА</label>
                <input type="text" bind:value={client.name} placeholder="Иван Иванов" autoFocus />
            </div>

            <div class="form-group">
                <label>ТЕЛЕФОН</label>
                {#each client.phones as phone, i}
                    <input type="tel"
                           bind:value={client.phones[i]}
                           placeholder="+7 (___) ___-__-__"
                           class="mb-8" />
                {/each}
                <button class="add-sub-link" on:click={addPhoneField}>+ Еще номер</button>
            </div>

            <div class="form-group">
                <label>ЗАМЕТКИ</label>
                <textarea bind:value={client.notes} placeholder="Особенности клиента..." rows="2"></textarea>
            </div>
        </div>

        <div class="modal-footer">
            <button class="save-btn" on:click={handleSave} disabled={isSaving}>
                {isSaving ? 'СОХРАНЕНИЕ...' : 'СОЗДАТЬ КЛИЕНТА'}
            </button>
        </div>
    </div>
</div>

<style>
    .modal-backdrop { position: fixed; inset: 0; background: rgba(0, 43, 54, 0.4); backdrop-filter: blur(8px); display: flex; align-items: center; justify-content: center; z-index: 99999; padding: 20px; box-sizing: border-box; }
    .modal-content { background: #fdf6e3; width: 100%; max-width: 480px; border-radius: 32px; overflow: hidden; box-shadow: 0 32px 64px -16px rgba(0, 0, 0, 0.3); max-height: 95vh; display: flex; flex-direction: column; border: 1.5px solid #ddd6c1; }

    .modal-header { padding: 20px 24px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1.5px solid #ddd6c1; background: #eee8d5; }
    .modal-header h2 { margin: 0; font-size: 16px; font-weight: 850; color: #073642; text-transform: uppercase; letter-spacing: 0.5px; }
    .close-btn { background: #fdf6e3; border: 1.5px solid #ddd6c1; width: 36px; height: 36px; border-radius: 50%; cursor: pointer; color: #586e75; font-weight: 800; display: flex; align-items: center; justify-content: center; transition: all 0.2s; }
    .close-btn:hover { background: white; border-color: #268bd2; color: #268bd2; }

    .modal-body { padding: 24px; background: #fdf6e3; overflow-y: auto; }
    .form-group { margin-bottom: 24px; }
    label { display: block; font-size: 10px; font-weight: 850; color: #93a1a1; text-transform: uppercase; letter-spacing: 0.8px; margin-bottom: 10px; }

    input, textarea {
        width: 100%; padding: 14px 18px; border-radius: 16px; border: 2px solid #ddd6c1;
        background: white; font-size: 16px; outline: none; box-sizing: border-box; color: #073642; font-weight: 600; transition: all 0.2s;
    }
    input::placeholder, textarea::placeholder { color: #93a1a1; font-weight: 500; }
    input:focus, textarea:focus { border-color: #268bd2; box-shadow: 0 0 0 4px rgba(38, 139, 210, 0.1); }
    .mb-8 { margin-bottom: 10px; }

    .add-sub-link { background: #eee8d5; border: 1.5px solid #ddd6c1; color: #268bd2; font-size: 12px; font-weight: 850; cursor: pointer; margin-top: 6px; padding: 8px 16px; border-radius: 12px; transition: all 0.2s; }
    .add-sub-link:hover { background: #268bd2; color: white; border-color: #268bd2; }

    .modal-footer { padding: 20px 24px; background: #eee8d5; border-top: 1.5px solid #ddd6c1; }
    .save-btn {
        width: 100%; padding: 18px; border-radius: 18px; border: none;
        background: #268bd2; color: white; font-size: 15px; text-transform: uppercase;
        font-weight: 900; cursor: pointer; letter-spacing: 0.5px; transition: all 0.2s;
    }
    .save-btn:active { transform: scale(0.98); }
    .save-btn:disabled { opacity: 0.6; cursor: not-allowed; }
</style>
