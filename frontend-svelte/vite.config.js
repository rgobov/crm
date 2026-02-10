import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';

export default defineConfig({
	plugins: [sveltekit()],
	server: {
		proxy: {
			// Прокси для API
			'/api': {
				target: 'http://localhost:8080',
				changeOrigin: true
			},
			// Прокси для WebSocket (SockJS)
			'/ws': {
				target: 'http://localhost:8080',
				ws: true,
				changeOrigin: true
			}
		}
	}
});
