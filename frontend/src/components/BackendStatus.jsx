import { useEffect, useState } from 'react'
import { getHealth } from '../api/client.js'

export default function BackendStatus() {
  const [state, setState] = useState({ phase: 'loading' })

  useEffect(() => {
    getHealth()
      .then((data) => setState({ phase: 'success', data }))
      .catch((error) => setState({ phase: 'error', message: error.message }))
  }, [])

    if (state.phase === 'loading') return <p>Checking connection to the backend …</p>
    if (state.phase === 'error') return <p role="alert" style={{ color: 'crimson' }}>{state.message}</p>
    return <p style={{ color: 'green' }}>Backend connected: {state.data.message}</p>
    }