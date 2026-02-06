import SockJS from 'sockjs-client/dist/sockjs.js';
import Stomp from 'stompjs';
import { writable } from 'svelte/store';
import { user } from '$lib/stores/auth.js';
import { get } from 'svelte/store';

export const wsConnected = writable(false);
export const scheduleUpdates = writable(null);

let stompClient = null;
let reconnectTimeout = null;

// Автоматический выбор URL для веб-сокетов (dev/prod)
const WS_URL = import.meta.env.VITE_WS_URL;

export const websocketService = {
    connect() {
        if (typeof window === 'undefined') return;

        const currentUser = get(user);
        if (!currentUser || !currentUser.tenantId) return;

        try {
            console.log('WS: Attempting to connect to', WS_URL);
            const socket = new SockJS(WS_URL);
            stompClient = Stomp.over(socket);
            stompClient.debug = null;

            stompClient.connect({}, (frame) => {
                wsConnected.set(true);
                console.log('WS: Connected successfully');

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
