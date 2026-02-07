import api from '$lib/api.js';

// Хелпер для получения строки YYYY-MM-DD в локальном времени (без UTC сдвига)
function toLocalDbDate(date) {
    if (!date) return '';
    const d = new Date(date);
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

export const adminService = {
    async getDashboardStats() {
        const response = await api.get('/admin/dashboard/stats');
        return response.data;
    },

    async getStaff(query = '', page = 0, size = 100) {
        const response = await api.get('/admin/staff', {
            params: { query, page, size }
        });
        return response.data;
    },

    async getClients(query = '', page = 0, size = 100) {
        const response = await api.get('/admin/clients', {
            params: { query, page, size }
        });
        return response.data;
    },

    async getWorkloadForMonth(year, month) {
        return (await api.get('/admin/workload', { params: { year, month } })).data;
    },

    async getAppointmentsForDay(date) {
        // ФИКС: Используем локальную дату вместо toISOString()
        const dateStr = toLocalDbDate(date);
        return (await api.get('/admin/appointments/day', { params: { date: dateStr } })).data;
    },

    async getStaffForSchedule(date) {
        // ФИКС: Используем локальную дату
        const dateStr = toLocalDbDate(date);
        return (await api.get('/admin/schedule/staff', { params: { date: dateStr } })).data;
    },

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
