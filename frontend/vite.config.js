import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

const target = 'http://localhost:8080';

// API-Aufrufe ans Backend
const api = {
  target,
  bypass: req => (req.headers.accept?.includes('text/html') ? '/index.html' : undefined),
};

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      '/health': target,
      '/users': api,
      '/auth': api,
      '/profile': api,
      '/plans': api,
      '/marks': api,
    },
  },
});