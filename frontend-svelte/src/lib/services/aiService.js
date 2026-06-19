import api from '$lib/api.js';

export const aiService = {
    async getConfig() {
        const res = await api.get('/admin/ai/config');
        return res.data;
    },

    async saveConfig(config) {
        await api.put('/admin/ai/config', config);
    },

    async getKnowledge(category) {
        const res = await api.get('/admin/ai/knowledge', {
            params: { category: category || undefined }
        });
        return res.data;
    },

    async addKnowledge(entry) {
        const res = await api.post('/admin/ai/knowledge', entry);
        return res.data;
    },

    async deleteKnowledge(id) {
        await api.delete(`/admin/ai/knowledge/${id}`);
    }
};