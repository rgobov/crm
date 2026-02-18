import api from '$lib/api.js';

export const branchService = {
    async getBranches() {
        const res = await api.get('/admin/branches');
        return res.data;
    },

    async createBranch(branch) {
        const res = await api.post('/admin/branches', branch);
        return res.data;
    },

    async updateBranch(id, branch) {
        const res = await api.put(`/admin/branches/${id}`, branch);
        return res.data;
    },

    async deleteBranch(id) {
        await api.delete(`/admin/branches/${id}`);
    }
};
