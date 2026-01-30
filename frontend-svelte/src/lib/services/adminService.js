import api from '$lib/api.js';

export const adminService = {
    async getDashboardStats() {
        const now = new Date();
        const dateStr = now.toISOString().split('T')[0];
        const [staff, todayApps, contactsCount, resources] = await Promise.all([
            api.get('/admin/staff').catch(() => ({ data: { content: [] } })),
            api.get(`/manager/appointments/day?date=${dateStr}`).catch(() => ({ data: [] })),
            api.get('/contacts/count').catch(() => ({ data: 0 })),
            api.get('/resources').catch(() => ({ data: [] }))
        ]);

        return {
            totalClients: contactsCount.data,
            todaysAppointmentsCount: todayApps.data.length,
            totalResources: resources.data.length,
            staff: staff.data.content
        };
    },

    // НОВОЕ: История записей для админа (Синхронно с Flutter)
    async getContactAppointments(contactId) {
        const response = await api.get(`/admin/clients/${contactId}/appointments`);
        return response.data;
    }
};
