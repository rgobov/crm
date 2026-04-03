<script>
    import { onMount } from 'svelte';
    import { contactService } from '$lib/services/contactService.js';
    import { goto } from '$app/navigation';

    export let contactId = null; // Если null - создание нового
    const isEditing = !!contactId;

    let firstName = '';
    let lastName = '';
    let middleName = '';
    let phones = ['']; // Массив телефонов
    let email = '';
    let notes = '';

    let isLoading = isEditing;
    let isSaving = false;

    onMount(async () => {
        if (isEditing) {
            try {
                const contact = await contactService.getContactById(contactId);
                // Разделяем ФИО (как во Flutter)
                const parts = contact.name.split(' ');
                lastName = parts[0] || '';
                firstName = parts[1] || '';
                middleName = parts.slice(2).join(' ') || '';

                phones = contact.phones && contact.phones.length > 0 ? [...contact.phones] : [''];
                email = contact.email || '';
                notes = contact.notes || '';
            } catch (e) {
                console.error('Failed to load contact');
                goto('/admin/clients');
            } finally {
                isLoading = false;
            }
        }
    });

    function addPhone() {
        phones = [...phones, ''];
    }

    function removePhone(index) {
        if (phones.length > 1) {
            phones = phones.filter((_, i) => i !== index);
        }
    }

    async function handleSave() {
        if (!firstName || !lastName) return alert('Фамилия и Имя обязательны');

        const cleanPhones = phones.map(p => p.replace(/\D/g, '')).filter(p => p.length >= 10);
        if (cleanPhones.length === 0) return alert('Добавьте хотя бы один корректный номер телефона');

        isSaving = true;
        const fullName = [lastName, firstName, middleName].filter(s => s).join(' ');

        try {
            const data = {
                name: fullName,
                phones: cleanPhones,
                email: email || null,
                notes: notes || null
            };

            if (isEditing) {
                // В Java методе updateContact ожидается объект с ID
                await api.put(`/contacts/${contactId}`, data);
            } else {
                await api.post('/contacts', data);
            }

            goto(isEditing ? `/admin/clients/${contactId}` : '/admin/clients');
        } catch (e) {
            alert(e.response?.data?.message || 'Ошибка сохранения');
        } finally {
            isSaving = false;
        }
    }
</script>

<div class="edit-screen">
    {#if isLoading}
        <div class="center"><span class="spinner"></span></div>
    {:else}
        <div class="form">
            <section class="card">
                <label class="section-label">Личные данные</label>
                <div class="field">
                    <input type="text" bind:value={lastName} placeholder="Фамилия *" />
                </div>
                <div class="field">
                    <input type="text" bind:value={firstName} placeholder="Имя *" />
                </div>
                <div class="field">
                    <input type="text" bind:value={middleName} placeholder="Отчество" />
                </div>
            </section>

            <section class="card">
                <label class="section-label">Контакты</label>
                {#each phones as phone, i}
                    <div class="phone-row">
                        <div class="field flex-1">
                            <input type="tel" bind:value={phones[i]} placeholder="+7 (___) ___-__-__" />
                        </div>
                        {#if phones.length > 1}
                            <button class="remove-btn" on:click={() => removePhone(i)}>✕</button>
                        {/if}
                    </div>
                {/each}
                <button class="add-btn-text" on:click={addPhone}>+ Добавить номер</button>

                <div class="field border-top">
                    <label>Email</label>
                    <input type="email" bind:value={email} placeholder="example@mail.com" />
                </div>
            </section>

            <section class="card">
                <label class="section-label">Дополнительно</label>
                <textarea bind:value={notes} placeholder="Заметки о клиенте..." rows="3"></textarea>
            </section>

            <button class="save-btn" on:click={handleSave} disabled={isSaving}>
                {isSaving ? 'Сохранение...' : (isEditing ? 'ОБНОВИТЬ КЛИЕНТА' : 'СОЗДАТЬ КЛИЕНТА')}
            </button>
        </div>
    {/if}
</div>

<style>
    .edit-screen { padding-bottom: 100px; }
    .section-label { font-size: 11px; font-weight: 800; color: var(--primary-color); letter-spacing: 1px; text-transform: uppercase; margin-bottom: 16px; display: block; }

    .card { padding: 20px; margin-bottom: 16px; background: white; border-radius: 20px; }
    .field { margin-bottom: 12px; }
    .field.border-top { border-top: 1px solid #f1f5f9; margin-top: 16px; padding-top: 16px; }

    input, textarea { width: 100%; padding: 14px; border: 1.5px solid #f1f5f9; border-radius: 14px; background: #f8fafc; font-size: 15px; box-sizing: border-box; outline: none; }
    input:focus, textarea:focus { border-color: var(--primary-color); background: white; }

    .phone-row { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
    .flex-1 { flex: 1; }
    .remove-btn { width: 32px; height: 32px; border-radius: 50%; border: none; background: #fee2e2; color: #ef4444; cursor: pointer; }
    .add-btn-text { background: none; border: none; color: var(--primary-color); font-weight: 700; font-size: 13px; margin-top: 8px; cursor: pointer; }

    .save-btn { width: 100%; padding: 18px; background: var(--primary-gradient); color: white; border: none; border-radius: 16px; font-weight: 800; margin-top: 20px; box-shadow: 0 10px 20px rgba(56, 151, 240, 0.2); }

    .spinner { width: 30px; height: 30px; border: 3px solid #f1f5f9; border-top-color: var(--primary-color); border-radius: 50%; animation: spin 1s linear infinite; display: inline-block; }
    @keyframes spin { to { transform: rotate(360deg); } }
    .center { text-align: center; padding: 40px; }
</style>
