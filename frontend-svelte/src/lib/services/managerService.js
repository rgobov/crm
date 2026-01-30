import api from '$lib/api.js';

export const managerService = {
    async getWappiSettings() {
        const response = await api.get('/manager/wappi/settings');
        return response.data;
    },

    async updateWappiSettings(settings) {
        await api.put('/manager/wappi/settings', settings);
    },

    async sendTestMessage(phone) {
        await api.post(`/manager/wappi/test?phone=${phone}`);
    },

    // История записей для менеджера
    async getContactAppointments(contactId) {
        const response = await api.get(`/manager/contacts/${contactId}/appointments`);
        return response.data;
    },

    // Загрузка месяца (Workload)
    async getWorkloadForMonth(year, month) {
        const response = await api.get('/manager/workload', {
            params: { year, month }
        });
        return response.data;
    },

    // Записи на конкретный день
    async getAppointmentsForDay(date) {
        const dateStr = date instanceof Date ? date.toISOString().split('T')[0] : date;
        const response = await api.get('/manager/appointments/day', {
            params: { date: dateStr }
        });
        return response.data;
    },

    // Список сотрудников со сменами на дату
    async getStaffForSchedule(date) {
        const dateStr = date instanceof Date ? date.toISOString().split('T')[0] : date;
        const response = await api.get('/manager/schedule/staff', {
            params: { date: dateStr }
        });
        return response.data;
    }
};
