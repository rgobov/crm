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
    },

    // --- НОВОЕ: Управление сменами (для администратора) ---

    /**
     * Обновить смену конкретного мастера на конкретную дату
     */
    async updateShift(staffId, shiftData) {
        const response = await api.put(`/admin/staff/${staffId}/shift`, shiftData);
        return response.data;
    },

    /**
     * Копировать график мастера на указанное количество дней
     */
    async copyShift(staffId, sourceShift, days) {
        await api.post(`/admin/staff/${staffId}/shift/copy`, sourceShift, {
            params: { days }
        });
    },

    /**
     * Загрузить фото профиля сотрудника
     */
    async uploadStaffPhoto(staffId, file) {
        const formData = new FormData();
        formData.append('file', file);
        const response = await api.post(`/admin/staff/${staffId}/photo`, formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        });
        return response.data;
    },

    /**
     * Удалить фото профиля сотрудника
     */
    async deleteStaffPhoto(staffId) {
        const response = await api.delete(`/admin/staff/${staffId}/photo`);
        return response.data;
    }
};
