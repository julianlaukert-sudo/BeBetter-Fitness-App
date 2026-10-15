import { BrowserRouter, Link, Route, Routes, useNavigate } from 'react-router-dom'
import BackendStatus from './components/BackendStatus.jsx'
import { AuthProvider, useAuth } from './context/AuthContext.jsx'
import LoginPage from './pages/LoginPage.jsx'
import RegisterPage from './pages/RegisterPage.jsx'

function StartPage() {
  const { isAuthenticated } = useAuth()
  return <p>{isAuthenticated ? 'You are logged in.' : 'Welcome to BeBetter.'}</p>
}

function Navigation() {
  const { isAuthenticated, logout } = useAuth()
  const navigate = useNavigate()

  async function handleLogout() {
    await logout()
    navigate('/login')
  }

  return (
    <nav style={{ display: 'flex', gap: '1rem', marginBottom: '1rem' }}>
      <Link to="/">Home</Link>
      {isAuthenticated ? (
        <button type="button" onClick={handleLogout}>Log out</button>
      ) : (
        <>
          <Link to="/login">Log in</Link>
          <Link to="/register">Sign up</Link>
        </>
      )}
    </nav>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <main style={{ fontFamily: 'sans-serif', padding: '2rem', maxWidth: 480 }}>
          <h1>BeBetter</h1>
          <Navigation />

          <Routes>
            <Route path="/" element={<StartPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/login" element={<LoginPage />} />
          </Routes>

          <footer style={{ marginTop: '2rem' }}>
            <BackendStatus />
          </footer>
        </main>
      </AuthProvider>
    </BrowserRouter>
  )
}