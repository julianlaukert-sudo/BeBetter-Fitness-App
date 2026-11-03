import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
// KI-Log Nr. 11
// Nur für angemeldete Nutzer. Schützt die Bedienung
export function RequireAuth({ children }) {
  const { isAuthenticated } = useAuth()
  if (!isAuthenticated) return <Navigate to="/login" replace />
  return children
}

// Login und Registrierung nur für nicht angemeldete Nutzer
export function GuestOnly({ children }) {
  const { isAuthenticated } = useAuth()
  if (isAuthenticated) return <Navigate to="/onboarding" replace />
  return children
}