import api from '$lib/api.js';

export const adminService = {
    // --- DASHBOARD ---
    async getDashboardStats() {
        // Запрашиваем всё одним махом через новый эндпоинт
        const response = await api.get('/admin/dashboard/stats');
        return response.data;
    },

    // --- STAFF (Сотрудники) ---
    async getStaff(query = '', page = 0, size = 100) {
        const response = await api.get('/admin/staff', {
            params: { query, page, size }
        });
        return response.data; // Возвращает Page объект
    },

    // --- CLIENTS (Клиенты) ---
    async getClients(query = '', page = 0, size = 100) {
        // Используем админский путь для клиентов
        const response = await api.get('/admin/clients', {
            params: { query, page, size }
        });
        return response.data;
    },

    // --- CALENDAR & SCHEDULE ---
    async getWorkloadForMonth(year, month) {
        return (await api.get('/admin/workload', { params: { year, month } })).data;
    },
    async getAppointmentsForDay(date) {
        const dateStr = date.toISOString().split('T')[0];
        return (await api.get('/admin/appointments/day', { params: { date: dateStr } })).data;
    },
    async getStaffForSchedule(date) {
        const dateStr = date.toISOString().split('T')[0];
        return (await api.get('/admin/schedule/staff', { params: { date: dateStr } })).data;
    },

    // --- APPOINTMENTS ---
    async createAppointment(data) {
        return await api.post('/admin/appointments', data);
    },
    async updateAppointment(id, data) {
        return await api.put(`/admin/appointments/${id}`, data);
    },
    async deleteAppointment(id) {
        return await api.delete(`/admin/appointments/${id}`);
    }
};
