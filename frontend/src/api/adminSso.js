import { submitAdmin } from '../utils/adminBridge.js';

// Lokal ist VITE_API_URL leer(Backend direkt auf Port 8080)
export function openAdmin(token) {
  submitAdmin(token, import.meta.env.VITE_API_URL || 'http://localhost:8080');
}