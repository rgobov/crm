import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';

export default defineConfig({
	plugins: [sveltekit()],
	server: {
		host: '0.0.0.0',
		port: 5173,
		strictPort: true,
		proxy: {
			// Прокси для API
			'/api': {
				target: 'http://127.0.0.1:8080',
				changeOrigin: true,
				secure: false,
				// Добавляем поддержку WebSocket для прокси, если нужно
				ws: true
			},
			// Прокси для WebSocket
			'/ws': {
				target: 'http://127.0.0.1:8080',
				ws: true,
				changeOrigin: true
			}
		}
	}
});
