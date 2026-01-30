import api from '$lib/api.js';

export const serviceService = {
    async getServices() {
        const response = await api.get('/services');
        return response.data;
    },

    async addService(data) {
        await api.post('/services', data);
    },

    async updateService(data) {
        // Синхронизация с Flutter: используется POST для обновления
        await api.post('/services', data);
    },

    async deleteService(id) {
        await api.delete(`/services/${id}`);
    },

    async getServiceById(id) {
        const services = await this.getServices();
        return services.find(s => s.id === id);
    }
};
