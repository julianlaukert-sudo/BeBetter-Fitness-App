import { BrowserRouter, Link, Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import BackendStatus from './components/BackendStatus.jsx'
import { GuestOnly, RequireAuth } from './components/RouteGuards.jsx'
import { AuthProvider, useAuth } from './context/AuthContext.jsx'
import LoginPage from './pages/LoginPage.jsx'
import RegisterPage from './pages/RegisterPage.jsx'
import './style.css'

// Platzhalter bis Commit 5 (OnboardingPage)
function OnboardingPlaceholder() {
  return <p className="card">Logged in. The profile form follows in the next step.</p>
}

function Header() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  async function handleLogout() {
    await logout()
    navigate('/login')
  }

  return (
    <header className="app-header">
      <h1>BeBetter</h1>
      <nav>
        {user ? (
          <>
            <Link to="/onboarding">My profile</Link>
            <button type="button" onClick={handleLogout}>Log out</button>
          </>
        ) : (
          <>
            <Link to="/login">Log in</Link>
            <Link to="/register">Sign up</Link>
          </>
        )}
      </nav>
      {user && <p className="user-info">Hello {user.name} · Role: {user.role}</p>}
    </header>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <div className="app">
          <Header />
          <main>
            <Routes>
              <Route path="/" element={<Navigate to="/login" replace />} />
              <Route path="/login" element={<GuestOnly><LoginPage /></GuestOnly>} />
              <Route path="/register" element={<GuestOnly><RegisterPage /></GuestOnly>} />
              <Route path="/onboarding" element={<RequireAuth><OnboardingPlaceholder /></RequireAuth>} />
              <Route path="*" element={<p className="card">Page not found.</p>} />
            </Routes>
          </main>
          <footer>
            <BackendStatus />
          </footer>
        </div>
      </AuthProvider>
    </BrowserRouter>
  )
}