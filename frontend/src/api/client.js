const API_URL = (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '')
const TIMEOUT_MS = 8000

export class ApiError extends Error {
  constructor(message, status, data) {
    super(message)
    this.status = status
    this.data = data
    // Feldfehler vom Backend, z. B. { password: '...' }
    this.fields = data?.errors ?? {}
  }
}

export async function request(path, { method = 'GET', body, token } = {}) {
  const headers = { Accept: 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (token) headers.Authorization = `Bearer ${token}`

  let response
  try {
    response = await fetch(`${API_URL}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
      signal: AbortSignal.timeout(TIMEOUT_MS),
    })
  } catch (error) {
    if (error.name === 'TimeoutError') {
      throw new ApiError('The backend did not respond in time. Please try again.', 0)
    }
    throw new ApiError('Unable to connect to the backend.', 0)
  }

  const data = await response.json().catch(() => null)

  if (!response.ok) {
    const message =
      data?.message ??
      `The backend is unreachable or returned an error (HTTP ${response.status}).`
    throw new ApiError(message, response.status, data)
  }
  return data
}

export async function getHealth() {
  const data = await request('/health')
  if (data?.status !== 'ok') {
    throw new ApiError('The backend sent an unexpected response.', 200, data)
  }
  return data
}