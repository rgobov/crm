import api from '$lib/api.js';
import { user } from '$lib/stores/auth.js';
import { get } from 'svelte/store';

function getRole() {
    const currentUser = get(user);
    if (currentUser?.role) return currentUser.role;
    const saved = typeof localStorage !== 'undefined' && localStorage.getItem('user');
    if (saved) return JSON.parse(saved).role;
    return null;
}

export const branchService = {
    async getBranches() {
        const role = getRole();
        const endpoint = role === 'ADMIN' ? '/admin/branches' : '/client/branches';
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
