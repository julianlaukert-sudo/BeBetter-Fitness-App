import { createContext, useContext, useState } from 'react'
import { login as loginRequest, logout as logoutRequest } from '../api/auth.js'
import { ApiError, request } from '../api/client.js'

const STORAGE_KEY = 'bebetter.session'
const INVALID_TOKEN_MESSAGE = 'Token is wrong.'

const AuthContext = createContext(null)

function readSession() {
    try {
        const data = JSON.parse(sessionStorage.getItem(STORAGE_KEY));

        // Admins arbeiten im Backend-Adminbereich.
        if (data?.role === 'ADMIN') {
            sessionStorage.removeItem(STORAGE_KEY);
            return null;
        }

        return data
            && typeof data.token === 'string'
            && Number.isFinite(data.userId)
            ? data
            : null;
    } catch {
        return null;
    }
}

function writeSession(value) {
    try {
        if (value) sessionStorage.setItem(STORAGE_KEY, JSON.stringify(value))
        else sessionStorage.removeItem(STORAGE_KEY)
        return true
    } catch {
        return false
    }
}

export function AuthProvider({ children }) {
    const [user, setUser] = useState(readSession)
    const [storageWarning, setStorageWarning] = useState('')

    function changeUser(value) {
        setUser(value)

        if (!writeSession(value)) {
            setStorageWarning(
                'Your login cannot be stored in this browser. It ends when you reload the page.'
            )
        } else {
            setStorageWarning('')
        }
    }

    // Speichert die komplette Login-Antwort:
    // token, userId, name, role und profileCompleted.
    async function login(email, password) {
        const data = await loginRequest({ email, password })
        // Admin-Token nicht im Browser speichern
        if (data.role === 'ADMIN') return data;
        changeUser(data)
        return data
    }

    // Für geschützte Anfragen: Hängt das Token an und
    // meldet den Nutzer bei einem ungültigen Token ab.
    async function authorized(path, options = {}) {
        if (!user) throw new ApiError('Please log in.', 0)

        try {
            return await request(path, { ...options, token: user.token })
        } catch (error) {
            if (error.message === INVALID_TOKEN_MESSAGE) {
                changeUser(null)
            }
            throw error
        }
    }

    async function logout() {
        const token = user?.token
        changeUser(null)

        if (token) {
            try {
                await logoutRequest(token)
            } catch {
                // Lokal ist der Nutzer trotzdem abgemeldet.
            }
        }
    }

    function markProfileCompleted() {
        if (user) changeUser({ ...user, profileCompleted: true })
    }

    const value = {
        user,
        isAuthenticated: Boolean(user),
        login,
        logout,
        authorized,
        markProfileCompleted,
    }

    return (
        <AuthContext.Provider value={value}>
            {storageWarning && <p role="alert">{storageWarning}</p>}
            {children}
        </AuthContext.Provider>
    )
}

export function useAuth() {
    const context = useContext(AuthContext)

    if (!context) {
        throw new Error('useAuth must be used inside AuthProvider')
    }

    return context
}