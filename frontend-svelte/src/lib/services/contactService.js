import api from '$lib/api.js';

export const contactService = {
    // Возвращаем данные напрямую (объект Page), чтобы поиск видел поле .content
    async getContacts(query = '', showAll = false, page = 0, size = 25) {
        const response = await api.get('/contacts', {
            params: {
                query: query.trim(),
                showAll: showAll,
                page,
                size
            }
        });
        return response.data;
    },

    async findContactByPhone(phone) {
        const clean = phone.replace(/\D/g, '');
        try {
            const response = await api.get('/contacts/by-phone', { params: { phone: clean } });
            return response.data;
        } catch (e) {
            return null;
        }
    },

    async getContactById(id) {
        const response = await api.get(`/contacts/${id}`);
        return response.data;
    },

    async addContact(contact) {
        const response = await api.post('/contacts', contact);
        return response.data;
    },

    async deleteContact(id) {
        await api.delete(`/contacts/${id}`);
    },

    async getContactAppointments(contactId) {
        const response = await api.get(`/contacts/${contactId}/appointments`);
        return response.data;
    }
};
