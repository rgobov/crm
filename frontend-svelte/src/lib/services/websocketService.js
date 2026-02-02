import SockJS from 'sockjs-client/dist/sockjs.js';
import Stomp from 'stompjs';
import { writable } from 'svelte/store';
import { user } from '$lib/stores/auth.js';
import { get } from 'svelte/store';

export const wsConnected = writable(false);
export const scheduleUpdates = writable(null);

let stompClient = null;
let reconnectTimeout = null;

// Используем тот же базовый URL, что и в api.js
const WS_URL = 'https://api.109.248.203.156.sslip.io/ws';

export const websocketService = {
    connect() {
        // Несмотря на отключенный SSR, оставляем проверку для надежности библиотек
        if (typeof window === 'undefined') return;

        const currentUser = get(user);
        if (!currentUser || !currentUser.tenantId) return;

        try {
            const socket = new SockJS(WS_URL);
            stompClient = Stomp.over(socket);
            stompClient.debug = null;

            stompClient.connect({}, (frame) => {
                wsConnected.set(true);
                console.log('WS: Connected to API');

                stompClient.subscribe(`/topic/schedule/${currentUser.tenantId}`, (message) => {
                    if (message.body) scheduleUpdates.set(message.body);
                });
            }, (error) => {
                console.error('WS Error:', error);
                wsConnected.set(false);
                this.reconnect();
            });
        } catch (e) {
            console.error('WS Setup failed:', e);
        }
    },

    reconnect() {
        if (reconnectTimeout) clearTimeout(reconnectTimeout);
        reconnectTimeout = setTimeout(() => this.connect(), 5000);
    },

    disconnect() {
        if (stompClient) {
            try {
                stompClient.disconnect();
            } catch (e) {}
            wsConnected.set(false);
        }
    }
};
