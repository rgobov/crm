import api from '$lib/api.js';

export const contactService = {
    // ИСПРАВЛЕНО: Добавлен параметр showAll
    async getContacts(query = '', showAll = false, page = 0, size = 25) {
        // Очистка телефона для поиска
        const cleanQuery = query.replace(/\D/g, '').length >= 6 ? query.replace(/\D/g, '') : query;

        const response = await api.get('/contacts', {
            params: {
                query: cleanQuery,
                showAll: showAll, // Передаем флаг на бэкенд
                page,
                size
            }
        });

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
    },

    // История записей (Синхронно с Flutter AdminService)
    async getContactAppointments(contactId) {
        const response = await api.get(`/contacts/${contactId}/appointments`);
        return response.data;
    }
};
