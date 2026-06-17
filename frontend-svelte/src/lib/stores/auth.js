import { writable } from 'svelte/store';

export const token = writable(null);
export const user = writable(null);

// Новые сторы для детекции окружения
export const isTelegram = writable(false);
export const telegramUser = writable(null);

export function initAuth() {
    // 1. Проверяем, запущены ли мы в Telegram
    const isTG = typeof window !== 'undefined' && !!window.Telegram?.WebApp?.initData;
    isTelegram.set(isTG);

    if (isTG) {
        const tg = window.Telegram.WebApp;
        telegramUser.set(tg.initDataUnsafe?.user || null);
        console.log('Environment: Telegram Mini App');
    } else {
        console.log('Environment: Standard Browser');
    }

    // 2. Восстанавливаем сессию из localStorage
    if (typeof window !== 'undefined') {
        const savedToken = localStorage.getItem('token');
        const savedUser = localStorage.getItem('user');

        if (savedToken && savedUser) {
            token.set(savedToken);
            user.set(JSON.parse(savedUser));
        }
    }
}

export function login(newToken, userData) {
    token.set(newToken);
    user.set(userData);
    localStorage.setItem('token', newToken);
    localStorage.setItem('user', JSON.stringify(userData));
}

export function logout() {
    token.set(null);
    user.set(null);
    localStorage.removeItem('token');
    localStorage.removeItem('user');
}
