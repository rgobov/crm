import api from '$lib/api.js';

function toLocalDbDate(date) {
    if (!date) return '';
    if (typeof date === 'string') return date;
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

    // Используем camelCase для branchId в соответствии с новыми DTO
    async getWorkloadForMonth(year, month, branchId) {
        return (await api.get('/admin/workload', {
            params: { year, month, branchId: branchId }
        })).data;
    },

    async getAppointmentsForDay(date, branchId, options = {}) {
        const dateStr = toLocalDbDate(date);
        return (await api.get('/admin/appointments/day', {
            params: { date: dateStr, branchId: branchId },
            ...options
        })).data;
    },

    async getStaffForSchedule(date, branchId, options = {}) {
        const dateStr = toLocalDbDate(date);
        return (await api.get('/admin/schedule/staff', {
            params: { date: dateStr, branchId: branchId },
            ...options
        })).data;
    },

    async createAppointment(data, force = false) {
        return await api.post('/admin/appointments', data, { params: { force } });
    },
    async updateAppointment(id, data, force = false, updateMode = 'single') {
        return await api.put(`/admin/appointments/${id}`, data, { params: { force, updateMode } });
    },
    async deleteAppointment(id, deleteMode = 'single') {
        return await api.delete(`/admin/appointments/${id}`, { params: { deleteMode } });
    },
    async exportAppointments(startDate, endDate, contactId = null) {
        const response = await api.get('/admin/appointments/export', {
            params: {
                startDate,
                endDate,
                contactId
            },
            responseType: 'blob'
        });
        return response.data;
    },

    async getReturnReminderCandidates(days = 30) {
        const response = await api.get('/admin/return-reminders', { params: { days } });
        return response.data;
    },

    async sendReturnReminder(contactId, message) {
        return await api.post('/admin/return-reminders/send', { contactId, message });
    }
};
