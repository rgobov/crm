import api from '$lib/api.js';

export const adminService = {
    async getDashboardStats() {
        const now = new Date();
        const dateStr = now.toISOString().split('T')[0];

        try {
            console.log('Fetching dashboard stats...');
            const [staff, todayApps, contactsCount, resources] = await Promise.all([
                api.get('/staff').catch(e => ({ data: [] })),
                api.get(`/appointments/day?date=${dateStr}`).catch(e => ({ data: [] })),
                api.get('/contacts/count').catch(e => ({ data: 0 })),
                api.get('/resources').catch(e => ({ data: [] }))
            ]);

            console.log('API Response - Contacts Count:', contactsCount.data);

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
