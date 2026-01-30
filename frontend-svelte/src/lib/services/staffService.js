import api from '$lib/api.js';

export const staffService = {
    async getStaff() {
        const response = await api.get('/admin/staff');
        return response.data;
    },

    async addStaffMember(data) {
        // data включает name, specialty, phone, role, available, email, password и др.
        await api.post('/admin/staff', data);
    },

    async updateStaffMember(id, data) {
        await api.put(`/admin/staff/${id}`, data);
    },

    async deleteStaffMember(id) {
        await api.delete(`/admin/staff/${id}`);
    }
};
