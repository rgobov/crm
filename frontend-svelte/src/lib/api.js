import axios from 'axios';
import { token } from './stores/auth.js';
import { get } from 'svelte/store';

// Vite автоматически подставит нужный URL в зависимости от режима (dev/prod)
const API_URL = import.meta.env.VITE_API_URL;

const api = axios.create({
    baseURL: API_URL
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
