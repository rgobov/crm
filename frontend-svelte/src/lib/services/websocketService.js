import Stomp from 'stompjs';
import { writable } from 'svelte/store';

export const wsConnected = writable(false);
export const scheduleRefreshSignal = writable({ ts: 0 });
export const telegramStatusSignal = writable({ status: null, qrCode: null, ts: 0 });

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
    if (typeof window !== 'undefined') {
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const host = window.location.host;
        return `${protocol}//${host}/ws`;
    }
    return 'ws://localhost:5173/ws';
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

        console.log('🔌 WS: Connecting to:', url);

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
                    const data = JSON.parse(message.body);
                    scheduleRefreshSignal.set({
                        ts: Date.now(),
                        type: data.type,
                        appointmentId: data.appointmentId,
                        branchId: data.branchId,
                        date: data.date
                    });
                });

                stompClient.subscribe(`/topic/telegram/${tenantId}`, (message) => {
                    const data = JSON.parse(message.body);
                    console.log('📥 WS: Telegram Update:', data.status);
                    telegramStatusSignal.set({
                        status: data.status,
                        qrCode: data.qrCode || null, // ТЕПЕРЬ ПЕРЕДАЕМ И QR
                        ts: data.ts || Date.now()
                    });
                });

            }, (error) => {
                console.warn('⚠️ WS: Connection lost');
                isConnecting = false;
                wsConnected.set(false);
                this.reconnect();
            });
        } catch (e) {
            console.error('❌ WS: Setup error', e);
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
