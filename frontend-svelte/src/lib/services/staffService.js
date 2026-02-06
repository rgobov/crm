import api from '$lib/api.js';

export const staffService = {
    // Получение списка с поиском
    async getStaff(query = '', page = 0, size = 100) {
        const response = await api.get('/admin/staff', {
            params: { query, page, size }
        });
        return response.data;
    },

    // Метод для получения одного сотрудника по ID
    async getStaffById(id) {
        const response = await api.get(`/admin/staff/${id}`);
        return response.data;
    },

    async addStaff(data) {
        const response = await api.post('/admin/staff', data);
        return response.data;
    },

    // Метод для обновления сотрудника
    async updateStaff(id, data) {
        const response = await api.put(`/admin/staff/${id}`, data);
        return response.data;
    },

    async deleteStaff(id) {
        await api.delete(`/admin/staff/${id}`);
    }
};
