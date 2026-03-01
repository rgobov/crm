import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';

export default defineConfig({
	plugins: [sveltekit()],
	server: {
		host: '0.0.0.0',
		port: 5173,
		strictPort: true,
		proxy: {
			// Прокси для API - используем localhost для WSL
			'/api': {
				target: 'http://localhost:8080',
				changeOrigin: true,
				secure: false
			},
			// Прокси для WebSocket
			'/ws': {
				target: 'http://localhost:8080',
				ws: true,
				changeOrigin: true
			}
		}
	}
});
