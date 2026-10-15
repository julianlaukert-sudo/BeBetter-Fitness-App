import { createContext, useContext, useState } from 'react'
import { login as loginRequest, logout as logoutRequest } from '../api/auth.js'

// Schlüssel im Browser-Speicher. Ggf. an die Referenz anpassen.
const STORAGE_KEY = 'bebetter.auth'

const AuthContext = createContext(null)

function readStoredAuth() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(readStoredAuth)

  async function login(email, password) {
    const data = await loginRequest({ email, password })
    const next = { token: data.token, role: data.role }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
    setAuth(next)
    return next
  }

  async function logout() {
    const token = auth?.token
    // Lokal sofort abmelden, auch wenn das Backend gerade nicht erreichbar ist
    localStorage.removeItem(STORAGE_KEY)
    setAuth(null)
    if (token) {
      try {
        await logoutRequest(token)
      } catch {
      }
    }
  }

  const value = {
    auth,
    isAuthenticated: Boolean(auth?.token),
    login,
    logout,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider')
  }
  return context
}