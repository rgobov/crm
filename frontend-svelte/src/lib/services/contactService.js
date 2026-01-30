import api from '$lib/api.js';

export const contactService = {
    async getContacts(query = '', page = 0, size = 25) {
        // Убираем лишние символы из телефона для поиска (как во Flutter)
        const cleanQuery = query.replace(/\D/g, '').length >= 6 ? query.replace(/\D/g, '') : query;

        const response = await api.get('/contacts', {
            params: { query: cleanQuery, page, size }
        });

        // Маппинг ответа Spring Data Page под формат Flutter-сервиса
        return {
            contacts: response.data.content,
            isLast: response.data.last,
            totalElements: response.data.totalElements
        };
    },

    async getContactById(id) {
        const response = await api.get(`/contacts/${id}`);
        return response.data;
    },

    async deleteContact(id) {
        await api.delete(`/contacts/${id}`);
    }
};
