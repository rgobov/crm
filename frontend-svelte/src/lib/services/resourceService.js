import api from '$lib/api.js';

export const resourceService = {
    // ИЗМЕНЕНО: Добавлен параметр branchId и переход на админский эндпоинт
    async getResources(branchId = null) {
        const response = await api.get('/admin/resources', {
            params: { branch_id: branchId }
        });
        return response.data;
    },

    async addResource(data) {
        await api.post('/admin/resources', data);
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
