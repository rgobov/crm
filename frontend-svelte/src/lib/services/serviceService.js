import api from '$lib/api.js';

export const serviceService = {
    async getServices(niche = null) {
        const params = niche ? { niche } : {};
        const response = await api.get('/services', { params });
        return response.data;
    },

    async addService(data) {
        const response = await api.post('/services', data);
        return response.data; // ВОЗВРАЩАЕМ СОЗДАННУЮ УСЛУГУ
    },

    async updateService(data) {
        const response = await api.put(`/services/${data.id}`, data);
        return response.data;
    },

    async deleteService(id) {
        await api.delete(`/services/${id}`);
    },

    async getServiceById(id) {
        const services = await this.getServices();
        return services.find(s => s.id === id);
    }
};
