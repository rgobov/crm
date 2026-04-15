import axios from 'axios';
import { token } from './stores/auth.js';
import { get } from 'svelte/store';

const API_URL = import.meta.env.VITE_API_URL || '/api';

const api = axios.create({
    baseURL: API_URL
});

// Кэш для активных (pending) GET-запросов
const pendingRequests = new Map();

api.interceptors.request.use(config => {
    const currentToken = get(token) || (typeof window !== 'undefined' ? localStorage.getItem('token') : null);
    if (currentToken) {
        config.headers.Authorization = `Bearer ${currentToken}`;
    }
    return config;
});

// Перехват ответов для очистки кэша
api.interceptors.response.use(
    response => {
        const key = response.config._requestKey;
        if (key) pendingRequests.delete(key);
        return response;
    },
    error => {
        const key = error.config?._requestKey;
        if (key) pendingRequests.delete(key);
        return Promise.reject(error);
    }
);

const originalGet = api.get;

// Безопасная дедупликация только через метод .get()
api.get = function(url, config = {}) {
    // Формируем уникальный ключ запроса
    const requestKey = `GET:${url}:${JSON.stringify(config.params || {})}`;

    if (pendingRequests.has(requestKey)) {
        console.log(`[Deduplicator] Joining existing request: ${url}`);
        return pendingRequests.get(requestKey);
    }

    // Помечаем конфиг ключом, чтобы перехватчик ответа знал, что удалять
    config._requestKey = requestKey;

    const promise = originalGet.call(this, url, config);
    pendingRequests.set(requestKey, promise);

    return promise;
};

export default api;
