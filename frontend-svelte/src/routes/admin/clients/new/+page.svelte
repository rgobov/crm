<script>
    import { contactService } from '$lib/services/contactService.js';
    import { goto } from '$app/navigation';
    import { onMount } from 'svelte';

    let client = {
        name: '',
        phones: [''],
        email: '',
        notes: ''
    };

    let isSaving = false;

    onMount(() => {
        if (window.Telegram && window.Telegram.WebApp) {
            window.Telegram.WebApp.BackButton.show();
            window.Telegram.WebApp.BackButton.onClick(() => goto('/admin/clients'));
        }
    });

    async function handleSave() {
        if (!client.name.trim()) return alert('Введите имя клиента');
        if (!client.phones[0].trim()) return alert('Введите номер телефона');

        isSaving = true;
        try {
            // Очищаем номер от лишних символов перед сохранением
            client.phones = client.phones.map(p => p.replace(/\D/g, ''));

            await contactService.addContact(client);
            goto('/admin/clients');
        } catch (e) {
            console.error('Save failed', e);
            alert('Ошибка при сохранении. Возможно, такой номер уже есть в базе.');
        } finally {
            isSaving = false;
        }
    }

    function addPhoneField() {
        client.phones = [...client.phones, ''];
    }
</script>

<div class="edit-page">
    <header class="header">
        <button class="back-btn" on:click={() => goto('/admin/clients')}>‹</button>
        <h1>Новый клиент</h1>
    </header>

    <div class="content">
        <section class="card">
            <div class="form-group">
                <label>ФИО КЛИЕНТА</label>
                <input type="text" bind:value={client.name} placeholder="Напр: Иван Иванов" autoFocus />
            </div>

            <div class="form-group">
                <label>ТЕЛЕФОН</label>
                {#each client.phones as phone, i}
                    <div class="phone-input-row">
                        <input type="tel"
                               bind:value={client.phones[i]}
                               placeholder="+7 (___) ___-__-__" />
                    </div>
                {/each}
                <button class="add-sub-btn" on:click={addPhoneField}>+ Добавить еще номер</button>
            </div>

            <div class="form-group">
                <label>EMAIL (НЕОБЯЗАТЕЛЬНО)</label>
                <input type="email" bind:value={client.email} placeholder="example@mail.ru" />
            </div>

            <div class="form-group">
                <label>ЗАМЕТКИ</label>
                <textarea bind:value={client.notes} placeholder="Особенности клиента и т.д." rows="3"></textarea>
            </div>
        </section>

        <div class="actions">
            <button class="save-btn" on:click={handleSave} disabled={isSaving}>
                {isSaving ? 'СОХРАНЕНИЕ...' : 'СОЗДАТЬ КЛИЕНТА'}
            </button>
        </div>
    </div>
</div>

<style>
    .edit-page { background: #f8fafc; min-height: 100vh; display: flex; flex-direction: column; }

    .header { background: white; padding: 20px 24px; border-bottom: 1px solid #f1f5f9; display: flex; align-items: center; gap: 16px; position: sticky; top: 0; z-index: 50; }
    .back-btn { background: #f1f5f9; border: none; width: 36px; height: 36px; border-radius: 12px; font-size: 24px; cursor: pointer; color: var(--primary-color); display: flex; align-items: center; justify-content: center; padding-bottom: 4px; }
    h1 { font-size: 20px; font-weight: 800; margin: 0; color: #0f172a; }

    .content { padding: 20px; flex: 1; max-width: 600px; margin: 0 auto; width: 100%; box-sizing: border-box; }

    .card { background: white; padding: 24px; border-radius: 24px; box-shadow: 0 4px 15px rgba(0,0,0,0.02); border: 1px solid #f1f5f9; }

    .form-group { margin-bottom: 24px; }
    .form-group:last-child { margin-bottom: 0; }

    label { display: block; font-size: 11px; font-weight: 800; color: #94a3b8; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 10px; }

    input, textarea {
        width: 100%; padding: 14px 16px; border-radius: 14px; border: 1.5px solid #f1f5f9;
        background: #f8fafc; font-size: 15px; outline: none; box-sizing: border-box;
        transition: all 0.2s;
    }
    input:focus, textarea:focus { border-color: var(--primary-color); background: white; }

    .add-sub-btn { background: none; border: none; color: var(--primary-color); font-size: 13px; font-weight: 700; cursor: pointer; margin-top: 8px; padding: 0; }

    .actions { margin-top: 32px; }
    .save-btn {
        width: 100%; padding: 18px; border-radius: 18px; border: none;
        background: var(--primary-gradient); color: white; font-size: 16px;
        font-weight: 800; cursor: pointer; box-shadow: 0 10px 25px rgba(56, 151, 240, 0.3);
        transition: transform 0.2s;
    }
    .save-btn:active { transform: scale(0.98); }
    .save-btn:disabled { opacity: 0.6; cursor: not-allowed; }
</style>
