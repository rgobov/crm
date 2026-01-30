import api from '$lib/api.js';

export const staffService = {
    async getStaff() {
        const response = await api.get('/admin/staff');
        return response.data;
    },

    async getStaffMember(id) {
        const response = await api.get(`/admin/staff/${id}`);
        return response.data;
    },

    async addStaffMember(data) {
        await api.post('/admin/staff', data);
    },

    async updateStaffMember(id, data) {
        await api.put(`/admin/staff/${id}`, data);
    },

    async deleteStaffMember(id) {
        await api.delete(`/admin/staff/${id}`);
    }
};
