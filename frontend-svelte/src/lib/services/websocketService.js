import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';
import { writable } from 'svelte/store';
import { user } from '$lib/stores/auth.js';
import { get } from 'svelte/store';

// Стор для отслеживания состояния подключения
export const wsConnected = writable(false);
// Стор для входящих событий обновления
export const scheduleUpdates = writable(null);

let stompClient = null;
let reconnectTimeout = null;

export const websocketService = {
    connect() {
        const currentUser = get(user);
        if (!currentUser || !currentUser.tenantId) return;

        // Пытаемся подключиться к эндпоинту, настроенному в Spring Boot
        const socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);

        // Отключаем лишние логи в консоли
        stompClient.debug = () => {};

        stompClient.connect({}, (frame) => {
            wsConnected.set(true);
            console.log('WS: Connected to Spring Boot');

            // Подписка на обновления расписания для конкретной компании (Tenant)
            stompClient.subscribe(`/topic/schedule/${currentUser.tenantId}`, (message) => {
                if (message.body) {
                    console.log('WS: Received schedule update:', message.body);
                    scheduleUpdates.set(message.body); // 'refresh' или JSON с данными
                }
            });
        }, (error) => {
            console.error('WS: Error', error);
            wsConnected.set(false);
            this.reconnect();
        });
    },

    reconnect() {
        if (reconnectTimeout) clearTimeout(reconnectTimeout);
        reconnectTimeout = setTimeout(() => {
            console.log('WS: Trying to reconnect...');
            this.connect();
        }, 5000);
    },

    disconnect() {
        if (stompClient) {
            stompClient.disconnect();
            wsConnected.set(false);
        }
        if (reconnectTimeout) clearTimeout(reconnectTimeout);
    }
};
