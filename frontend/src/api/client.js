const API_URL = import.meta.env.VITE_API_URL ?? ''

export async function getHealth() {
  let response
  try {
    response = await fetch(`${API_URL}/health`)
  } catch {
    throw new Error('Keine Verbindung zum Backend möglich.')
  }

  if (!response.ok) {
    throw new Error(
      `Das Backend ist nicht erreichbar oder meldet einen Fehler (HTTP ${response.status}).`,
    )
  }

  const data = await response.json()
  if (data.status !== 'ok') {
    throw new Error('Das Backend hat eine unerwartete Antwort geschickt.')
  }
  return data
}