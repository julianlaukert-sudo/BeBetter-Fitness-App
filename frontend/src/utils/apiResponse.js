// Fehlerklasse für alle Backendaufrufe
// KI-Log Nr. 19
export class ApiError extends Error {
  constructor(message, status, data) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.data = data;
    this.fields = data?.errors && typeof data.errors === 'object' ? data.errors : {};
  }
}

// Wertet eine Serverantwort kontrolliert aus
export function apiResponse(text, status, ok) {
  let data;
  try { data = JSON.parse(text); } catch { data = null; }
  if (!ok) {
    const message = typeof data?.message === 'string'
      ? data.message
      : `Backend responded with HTTP ${status}. Please try again later.`;
    throw new ApiError(message, status, data);
  }
  if (!data || typeof data !== 'object' || Array.isArray(data))
    throw new ApiError('Backend did not return a valid JSON response. Please check the connection.',
      status, null);
  return data;
}