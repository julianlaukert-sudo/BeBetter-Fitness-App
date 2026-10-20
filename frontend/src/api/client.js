export const API_URL = (import.meta.env.VITE_API_URL || '').replace(/\/$/, '');
const DEFAULT_TIMEOUT = 8000; // Kontoanfragen: 8 s; Plananfragen übergeben 60 s

export class ApiError extends Error {
  constructor(message, status, data) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.data = data;
    this.fields = data?.errors ?? {};
  }
}

export async function request(path, { method = 'GET', body, token, timeout = DEFAULT_TIMEOUT } = {}) {
  let response;
  try {
    response = await fetch(`${API_URL}${path}`, {
      method,
      headers: {
        Accept: 'application/json',
        ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
      signal: AbortSignal.timeout(timeout),
    });
  } catch (e) {
    if (e?.name === 'TimeoutError')
      throw new ApiError('The server took too long to respond. Please try again.', 0, null);
    throw new ApiError('Backend not reachable. Please try again.', 0, null);
  }
  let data = null;
  try { data = await response.json(); } catch { data = null; }
  if (!response.ok) {
    throw new ApiError(data?.message || `HTTP ${response.status}`, response.status, data);
  }
  return data;
}

export function getHealth() {
  return request('/health');
}