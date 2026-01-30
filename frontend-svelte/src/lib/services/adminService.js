import api from '$lib/api.js';

export const adminService = {
    // 1. Статистика Дашборда (Синхронно с новыми путями)
    async getDashboardStats() {
        const now = new Date();
        const dateStr = now.toISOString().split('T')[0];

        const [staffCountRes, todayApps, contactsCount, resources] = await Promise.all([
            api.get('/admin/staff', { params: { size: 1 } }), // Просто чтобы узнать totalElements
            api.get('/admin/appointments/day', { params: { date: dateStr } }),
            api.get('/contacts/count'),
            api.get('/admin/resources')
        ]);

        return {
            totalClients: contactsCount.data,
            todaysAppointmentsCount: todayApps.data.length,
            totalResources: resources.data.length,
            totalStaff: staffCountRes.data.totalElements
        };
    },

    // 2. Календарь и Загрузка (Admin Only)
    async getWorkloadForMonth(year, month) {
        const response = await api.get('/admin/workload', {
            params: { year, month }
        });
        return response.data;
    },

    // 3. Расписание дня (Admin Only)
    async getAppointmentsForDay(date) {
        const dateStr = date instanceof Date ? date.toISOString().split('T')[0] : date;
        const response = await api.get('/admin/appointments/day', {
            params: { date: dateStr }
        });
        return response.data;
    },

    // 4. Сотрудники и их смены на дату (Admin Only)
    async getStaffForSchedule(date) {
        const dateStr = date instanceof Date ? date.toISOString().split('T')[0] : date;
        const response = await api.get('/admin/schedule/staff', {
            params: { date: dateStr }
        });
        return response.data;
    },

    // 5. История клиента
    async getContactAppointments(contactId) {
        const response = await api.get(`/admin/clients/${contactId}/appointments`);
        return response.data;
    },

    // 6. Управление записяes
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
