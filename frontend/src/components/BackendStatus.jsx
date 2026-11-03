import { useEffect, useState } from 'react'
import { getHealth } from '../api/client.js'
// KI-Log Nr. 9
export default function BackendStatus() {
  const [state, setState] = useState({ phase: 'loading' })

  useEffect(() => {
    getHealth()
      .then((data) => setState({ phase: 'success', data }))
      .catch((error) => setState({ phase: 'error', message: error.message }))
  }, [])

  if (state.phase === 'loading') return <p className="status">Checking connection to the backend …</p>
  if (state.phase === 'error') return <p role="alert" className="status status--error">{state.message}</p>
  return <p className="status status--ok">Backend connected: {state.data.message}</p>
}