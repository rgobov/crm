import api from '$lib/api.js';

export const adminService = {
    // --- DASHBOARD ---
    async getDashboardStats() {
        const now = new Date();
        const dateStr = now.toISOString().split('T')[0];
        const [staffCountRes, todayApps, contactsCount, resources] = await Promise.all([
            api.get('/admin/staff', { params: { size: 1 } }),
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
    },

    // --- COMMENTS (Синхронизировано с AdminController) ---
    async getCommentsForAppointment(appointmentId) {
        return (await api.get(`/admin/appointments/${appointmentId}/comments`)).data;
    },
    async addCommentToAppointment(appointmentId, text) {
        return (await api.post(`/admin/appointments/${appointmentId}/comments`, { text })).data;
    },

    // --- CLIENTS ---
    async getContactAppointments(contactId) {
        return (await api.get(`/admin/clients/${contactId}/appointments`)).data;
    }
};
