import { request } from './client.js'

export function registerUser({ name, email, password }) {
  return request('/users', { method: 'POST', body: { name, email, password } })
}

export function login({ email, password }) {
  return request('/auth', { method: 'POST', body: { email, password } })
}

export function logout(token) {
  return request('/auth', { method: 'DELETE', token })
}