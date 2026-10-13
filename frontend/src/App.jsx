import { BrowserRouter, Link, Route, Routes } from 'react-router-dom'
import BackendStatus from './components/BackendStatus.jsx'
import RegisterPage from './pages/RegisterPage.jsx'

function StartPage() {
  return <p>Willkommen bei BeBetter.</p>
}

export default function App() {
  return (
    <BrowserRouter>
      <main style={{ fontFamily: 'sans-serif', padding: '2rem', maxWidth: 480 }}>
        <h1>BeBetter</h1>
        <nav style={{ display: 'flex', gap: '1rem', marginBottom: '1rem' }}>
          <Link to="/">Start</Link>
          <Link to="/register">Registrieren</Link>
        </nav>

        <Routes>
          <Route path="/" element={<StartPage />} />
          <Route path="/register" element={<RegisterPage />} />
        </Routes>

        <footer style={{ marginTop: '2rem' }}>
          <BackendStatus />
        </footer>
      </main>
    </BrowserRouter>
  )
}