import { writable } from 'svelte/store';

// Хранилище для данных пользователя
export const user = writable(null);
export const token = writable(null);

// Функция для очистки данных при выходе
export function logout() {
    localStorage.removeItem('token');
    user.set(null);
    token.set(null);
}
