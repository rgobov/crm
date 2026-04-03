<script>
    import { createEventDispatcher } from 'svelte';
    import { contactService } from '$lib/services/contactService.js';
    import { fade, scale } from 'svelte/transition';

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

<div class="modal-backdrop" on:click|self={() => dispatch('close')} transition:fade={{duration: 200}}>
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
    .modal-backdrop { position: fixed; inset: 0; background: rgba(15, 23, 42, 0.6); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; z-index: 2000; padding: 20px; }
    .modal-content { background: white; width: 100%; max-width: 450px; border-radius: 28px; overflow: hidden; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25); }

    .modal-header { padding: 24px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #f1f5f9; }
    .modal-header h2 { margin: 0; font-size: 18px; font-weight: 800; color: #0f172a; }
    .close-btn { background: #f1f5f9; border: none; width: 32px; height: 32px; border-radius: 50%; cursor: pointer; color: #64748b; font-weight: bold; }

    .modal-body { padding: 24px; }
    .form-group { margin-bottom: 20px; }
    label { display: block; font-size: 10px; font-weight: 800; color: #94a3b8; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 8px; }

    input, textarea {
        width: 100%; padding: 12px 16px; border-radius: 12px; border: 1.5px solid #f1f5f9;
        background: #f8fafc; font-size: 15px; outline: none; box-sizing: border-box;
    }
    input:focus, textarea:focus { border-color: var(--primary-color); background: white; }
    .mb-8 { margin-bottom: 8px; }

    .add-sub-link { background: none; border: none; color: var(--primary-color); font-size: 12px; font-weight: 700; cursor: pointer; margin-top: 4px; }

    .modal-footer { padding: 24px; background: #f8fafc; }
    .save-btn {
        width: 100%; padding: 16px; border-radius: 16px; border: none;
        background: var(--primary-gradient); color: white; font-size: 15px;
        font-weight: 800; cursor: pointer; box-shadow: 0 10px 20px rgba(56, 151, 240, 0.2);
    }
</style>
