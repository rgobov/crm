import axios from 'axios';
import { token } from './stores/auth.js';
import { get } from 'svelte/store';

// ТЕХНИЧЕСКОЕ РЕШЕНИЕ: Относительный путь для корректной работы Vite Proxy в WSL + Эмулятор
const api = axios.create({
    baseURL: '/api'
});

// Автоматически добавляем токен в каждый запрос
api.interceptors.request.use(config => {
    const currentToken = get(token) || (typeof window !== 'undefined' ? localStorage.getItem('token') : null);
    if (currentToken) {
        config.headers.Authorization = `Bearer ${currentToken}`;
    }
    return config;
});

export default api;
