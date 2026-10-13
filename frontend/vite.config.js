import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const backend = 'http://localhost:8080'

const api = {
  target: backend,
  bypass: (req) => (req.headers.accept?.includes('text/html') ? req.url : undefined),
}

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/health': backend,
      '/users': api,
      '/auth': api,
      '/profile': api,
    },
  },
})