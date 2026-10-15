import { request } from './client.js'

export function registerUser({ name, email, password }) {
  return request('/users', { method: 'POST', body: { name, email, password } })
}