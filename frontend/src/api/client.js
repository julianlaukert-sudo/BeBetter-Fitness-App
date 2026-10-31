import { ApiError, apiResponse } from '../utils/apiResponse.js';

export { ApiError };
export const API_URL = (import.meta.env.VITE_API_URL || '').replace(/\/$/, '');
const DEFAULT_TIMEOUT = 8000; // Kontoanfragen: 8 s; Plananfragen übergeben 60 s

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
  return apiResponse(await response.text(), response.status, response.ok);
}

export function getHealth() {
  return request('/health');
}