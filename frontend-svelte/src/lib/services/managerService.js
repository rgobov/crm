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

    async createAppointment(data, force = false) {
        const response = await api.post('/manager/appointments', data, { params: { force } });
        return response.data;
    },

    async updateAppointment(id, data, force = false, updateMode = 'single') {
        const response = await api.put(`/manager/appointments/${id}`, data, { params: { force, updateMode } });
        return response.data;
    },

    async deleteAppointment(id, deleteMode = 'single') {
        await api.delete(`/manager/appointments/${id}`, { params: { deleteMode } });
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
