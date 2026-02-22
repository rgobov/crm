import Stomp from 'stompjs';
import { writable } from 'svelte/store';

export const wsConnected = writable(false);
export const scheduleRefreshSignal = writable({ ts: 0 });
// ТЕПЕРЬ СТОР ХРАНИТ ПОЛНЫЙ ОБЪЕКТ СТАТУСА
export const telegramStatusSignal = writable({ status: null, ts: 0 });

let stompClient = null;
let reconnectTimeout = null;
let isConnecting = false;

function getTenantFromToken() {
    if (typeof window === 'undefined') return null;
    const token = localStorage.getItem('token');
    if (!token) return null;
    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        return payload.tenantId || payload.tenant_id || payload.tenant || payload.sub;
    } catch (e) {
        return null;
    }
}

const getWsUrl = () => {
    if (import.meta.env.VITE_WS_URL) return import.meta.env.VITE_WS_URL;
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = window.location.hostname === 'localhost' ? 'localhost:8080' : window.location.host;
    return `${protocol}//${host}/ws`;
};

export const websocketService = {
    connect() {
        if (typeof window === 'undefined') return;
        if (stompClient?.connected || isConnecting) return;

        const tenantId = getTenantFromToken();
        if (!tenantId) return;

        isConnecting = true;
        const url = getWsUrl();
        const token = localStorage.getItem('token');

        try {
            const socket = new WebSocket(url);
            stompClient = Stomp.over(socket);
            stompClient.debug = null;

            const headers = token ? { 'Authorization': 'Bearer ' + token } : {};

            stompClient.connect(headers, (frame) => {
                isConnecting = false;
                wsConnected.set(true);
                console.log('✅ WS: Connected');

                stompClient.subscribe(`/topic/schedule/${tenantId}`, (message) => {
                    scheduleRefreshSignal.set({ ts: Date.now() });
                });

                stompClient.subscribe(`/topic/telegram/${tenantId}`, (message) => {
                    const data = JSON.parse(message.body);
                    console.log('📥 WS: Telegram status update:', data.status);
                    // ОБНОВЛЯЕМ СТОР РЕАЛЬНЫМИ ДАННЫМИ
                    telegramStatusSignal.set({
                        status: data.status,
                        ts: data.ts || Date.now()
                    });
                });

            }, (error) => {
                isConnecting = false;
                wsConnected.set(false);
                this.reconnect();
            });
        } catch (e) {
            isConnecting = false;
            this.reconnect();
        }
    },

    reconnect() {
        if (reconnectTimeout) clearTimeout(reconnectTimeout);
        reconnectTimeout = setTimeout(() => this.connect(), 5000);
    },

    disconnect() {
        if (stompClient) {
            stompClient.disconnect();
            wsConnected.set(false);
        }
    }
};

if (typeof window !== 'undefined') {
    setTimeout(() => websocketService.connect(), 1000);
}
