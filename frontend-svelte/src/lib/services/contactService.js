import api from '$lib/api.js';

export const contactService = {
    // Возвращаем Page объект напрямую (content, totalElements и т.д.)
    async getContacts(query = '', showAll = false, page = 0, size = 25) {
        const response = await api.get('/admin/clients', {
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
            // Используем админский путь для поиска
            const response = await api.get('/admin/clients/by-phone', { params: { phone: clean } });
            return response.data;
        } catch (e) {
            return null;
        }
    },

    async getContactById(id) {
        const response = await api.get(`/admin/clients/${id}`);
        return response.data;
    },

    async addContact(contact) {
        const response = await api.post('/admin/clients', contact);
        return response.data;
    },

    async updateContact(id, contact) {
        const response = await api.put(`/admin/clients/${id}`, contact);
        return response.data;
    },

    async deleteContact(id) {
        await api.delete(`/admin/clients/${id}`);
    },

    async exportContacts(query = '', showAll = false) {
        const response = await api.get('/admin/clients/export', {
            params: {
                query: query.trim(),
                showAll: showAll
            },
            responseType: 'blob'
        });
        return response.data;
    }
};
