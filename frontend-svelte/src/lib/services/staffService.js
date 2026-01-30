import api from '$lib/api.js';

export const staffService = {
    // Обновленный метод с поддержкой поиска и пагинации
    async getStaff(query = '', page = 0, size = 100) {
        const response = await api.get('/admin/staff', {
            params: { query, page, size }
        });
        // Spring Data Page возвращает объект с полем 'content'
        return response.data;
    },

    async getStaffMember(id) {
        const response = await api.get(`/admin/staff/${id}`);
        return response.data;
    },

    async addStaffMember(data) {
        await api.post('/admin/staff', data);
    },

    async updateStaffMember(id, data) {
        await api.put(`/admin/staff/${id}`, data);
    },

    async deleteStaffMember(id) {
        await api.delete(`/admin/staff/${id}`);
    }
};
