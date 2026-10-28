import { BrowserRouter, Routes, Route, Navigate, Link, useNavigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext.jsx';
import { PlanProvider } from './context/PlanContext.jsx';
import { RequireAuth, GuestOnly } from './components/RouteGuards.jsx';
import LoginPage from './pages/LoginPage.jsx';
import RegisterPage from './pages/RegisterPage.jsx';
import OnboardingPage from './pages/OnboardingPage.jsx';
import HomePage from './pages/HomePage.jsx';
import TrainingPage from './pages/TrainingPage.jsx';
import SessionPage from './pages/SessionPage.jsx';
import './style.css';
import ProfilePage from './pages/ProfilePage.jsx';

function Header() {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  async function signOut() {
    try { await logout(); } finally { navigate('/login'); }
  }

  return (
    <header className="app-header">
      <Link to={isAuthenticated ? '/home' : '/login'}><strong>BeBetter</strong></Link>
      <nav>
        {isAuthenticated
          ? <>
              <Link to="/home">Start</Link>{' '}
              <Link to="/training">Training</Link>{' '}
              <Link to="/profile">My profile</Link>{' '}
              <button type="button" onClick={signOut}>Log out</button>
            </>
          : <>
              <Link to="/login">Log in</Link>{' '}
              <Link to="/register">Sign up</Link>
            </>}
      </nav>
      {isAuthenticated && <p>Hello {user.name} · Role: {user.role}</p>}
    </header>
  );
}

// key: beim Kontowechsel wird der Formularzustand neu aufgebaut
function OnboardingRoute() {
  const { user } = useAuth();
  return <OnboardingPage key={user.userId} />;
}

// key: beim Kontowechsel baut sich das Formular neu auf
function ProfileRoute() {
  const { user } = useAuth();
  return <ProfilePage key={user.userId} />;
}

// key: beim Kontowechsel bleibt kein alter Plan im Zustand
function WithPlans({ children }) {
  const { user } = useAuth();
  return <PlanProvider key={user?.userId ?? 'guest'}>{children}</PlanProvider>;
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <WithPlans>
          <div className="app">
            <Header />
            <main>
              <Routes>
                <Route path="/" element={<Navigate to="/login" replace />} />
                <Route path="/login" element={<GuestOnly><LoginPage /></GuestOnly>} />
                <Route path="/register" element={<GuestOnly><RegisterPage /></GuestOnly>} />
                <Route path="/onboarding" element={<RequireAuth><OnboardingRoute /></RequireAuth>} />
                <Route path="/profile" element={<RequireAuth><ProfileRoute /></RequireAuth>} />
                <Route path="/home" element={<RequireAuth><HomePage /></RequireAuth>} />
                <Route path="/training" element={<RequireAuth><TrainingPage /></RequireAuth>} />
                <Route path="/session/:id" element={<RequireAuth><SessionPage /></RequireAuth>} />
                <Route path="*" element={<p>Page not found.</p>} />
              </Routes>
            </main>
            <footer>
              <p>
                Exercise data:{' '}
                <a href="https://oss.exercisedb.dev/docs" target="_blank" rel="noreferrer">
                  ExerciseDB / AscendAPI
                </a>
              </p>
            </footer>
          </div>
        </WithPlans>
      </AuthProvider>
    </BrowserRouter>
  );
}