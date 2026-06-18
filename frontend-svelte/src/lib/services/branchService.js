import api from '$lib/api.js';
import { user } from '$lib/stores/auth.js';
import { get } from 'svelte/store';

export const branchService = {
    async getBranches() {
        const currentUser = get(user);
        const endpoint = currentUser?.role === 'CLIENT' ? '/client/branches' : '/admin/branches';
        const res = await api.get(endpoint);
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
