import { useEffect, useState } from 'react'
import { getHealth } from './api/client.js'

export default function App() {
  const [state, setState] = useState({ phase: 'loading' })

  useEffect(() => {
    getHealth()
      .then((data) => setState({ phase: 'success', data }))
      .catch((error) => setState({ phase: 'error', message: error.message }))
  }, [])

  return (
    <main style={{ fontFamily: 'sans-serif', padding: '2rem' }}>
      <h1>BeBetter</h1>

      {state.phase === 'loading' && <p>Verbindung zum Backend wird geprüft …</p>}

      {state.phase === 'success' && (
        <p style={{ color: 'green' }}>
          Backend verbunden: {state.data.message}
        </p>
      )}

      {state.phase === 'error' && (
        <p role="alert" style={{ color: 'crimson' }}>
          {state.message}
        </p>
      )}
    </main>
  )
}