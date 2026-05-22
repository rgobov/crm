import api from '$lib/api.js';

function toLocalDbDate(date) {
    if (!date) return '';
    const d = new Date(date);
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

export const clientService = {
    async getBranches() {
        const response = await api.get('/client/branches');
        return response.data;
    },

    async getServices() {
        const response = await api.get('/client/services');
        return response.data;
    },

    async getAppointmentsForDay(date, branchId, options = {}) {
        const dateStr = toLocalDbDate(date);
        return (await api.get('/client/appointments/day', {
            params: { date: dateStr, branchId: branchId },
            ...options
        })).data;
    },

    async getStaffForSchedule(date, branchId, options = {}) {
        const dateStr = toLocalDbDate(date);
        return (await api.get('/client/schedule/staff', {
            params: { date: dateStr, branchId: branchId },
            ...options
        })).data;
    },

    async createAppointment(data) {
        const response = await api.post('/client/appointments', data);
        return response.data;
    },

    async deleteAppointment(id) {
        return await api.delete(`/client/appointments/${id}`);
    }
};
