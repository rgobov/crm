import api from '$lib/api.js';

export const resourceService = {
    async getResources() {
        const response = await api.get('/resources');
        return response.data;
    },

    async addResource(data) {
        await api.post('/resources', data);
    },

    async updateResource(id, data) {
        // Синхронизация с Flutter: используем POST для сохранения с ID
        await api.post('/resources', { id, ...data });
    },

    async deleteResource(id) {
        await api.delete(`/resources/${id}`);
    },

    async getResourceById(id) {
        const resources = await this.getResources();
        return resources.find(r => r.id === id);
    }
};
