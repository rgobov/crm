import api from '$lib/api.js';

export const resourceService = {
    // ИЗМЕНЕНО: Добавлен параметр branchId и переход на админский эндпоинт
    async getResources(branchId = null) {
        const response = await api.get('/admin/resources', {
            params: { branchId: branchId }
        });
        return response.data;
    },

    async addResource(data) {
        await api.post('/admin/resources', data);
    },

    async getResourcePhoto(resourceId) {
        const response = await api.get(`/admin/resources/${resourceId}/photo`);
        return response.data;
    },

    async uploadResourcePhoto(resourceId, file) {
        const formData = new FormData();
        formData.append('file', file);
        const response = await api.post(`/admin/resources/${resourceId}/photo`, formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        });
        return response.data;
    },

    async deleteResourcePhoto(resourceId) {
        const response = await api.delete(`/admin/resources/${resourceId}/photo`);
        return response.data;
    },

    async updateResource(id, data) {
        await api.put(`/admin/resources/${id}`, data);
    },

    async deleteResource(id) {
        await api.delete(`/admin/resources/${id}`);
    },

    async getResourceById(id) {
        // Для деталей используем общий список без фильтра (или можно добавить фильтр позже)
        const resources = await this.getResources();
        return resources.find(r => r.id === id);
    }
};
