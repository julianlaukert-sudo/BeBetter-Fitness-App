import { request } from './client.js'

// Feldnamen gemäß API-Vertrag (docs/API.md) bzw. Referenz prüfen!
export function registerUser({ email, password }) {
  return request('/users', { method: 'POST', body: { email, password } })
}