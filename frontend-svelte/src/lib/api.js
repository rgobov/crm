import axios from 'axios';
import { token } from './stores/auth.js';
import { get } from 'svelte/store';

const API_URL = 'https://api.109.248.203.156.sslip.io/api';

const api = axios.create({
    baseURL: API_URL
});

// Автоматически добавляем токен в каждый запрос
api.interceptors.request.use(config => {
    const currentToken = get(token) || localStorage.getItem('token');
    if (currentToken) {
        config.headers.Authorization = `Bearer ${currentToken}`;
    }
    return config;
});

export default api;
