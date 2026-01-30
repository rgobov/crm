import api from '$lib/api.js';

export const adminService = {
    async getDashboardStats() {
        const now = new Date();
        const dateStr = now.toISOString().split('T')[0]; // ГГГГ-ММ-ДД

        try {
            // Выполняем запросы параллельно (как в Flutter через Future.wait)
            const [staff, todayApps, contactsCount, resources] = await Promise.all([
                api.get('/staff'),
                api.get(`/appointments/day?date=${dateStr}`),
                api.get('/contacts/count'),
                api.get('/resources')
            ]);

            return {
                totalClients: contactsCount.data,
                todaysAppointmentsCount: todayApps.data.length,
                totalResources: resources.data.length,
                staff: staff.data
            };
        } catch (error) {
            console.error('Error loading admin stats:', error);
            throw error;
        }
    }
};
