import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
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
import { CalendarDays, Target, TrendingUp } from 'lucide-react';
import AppNav, { Brand } from './components/AppNav.jsx';
import heroImage from './assets/auth-hero.webp';

// Quellenhinweis der Übungsdaten (Attribution, bleibt sichtbar)
function Attribution() {
  return (
    <footer className="app-footer">
      <p>
        Exercise data:{' '}
        <a href="https://oss.exercisedb.dev/docs" target="_blank" rel="noreferrer">
          ExerciseDB / AscendAPI
        </a>
      </p>
    </footer>
  );
}

// Angemeldet: App-Layout mit Navigation.
// Gäste (Login, Registrierung): Bildbereich und Formular, ohne App-Tabs.
function Shell({ children }) {
  const { isAuthenticated } = useAuth();

  if (!isAuthenticated) {
    return (
      <div className="auth-shell">
        <section className="auth-hero">
          <img className="auth-hero-image" src={heroImage} alt="" />
          <Brand to="/login" />
          <div className="auth-hero-content">
            <h1 className="auth-headline">
              More than just a plan. <span>Your personal progress.</span>
            </h1>

            <ul className="auth-benefits">
              <li><CalendarDays size={24} aria-hidden="true" /> Personal training plan</li>
              <li><TrendingUp size={24} aria-hidden="true" /> Track your progress</li>
              <li><Target size={24} aria-hidden="true" /> Reach your goals</li>
            </ul>
          </div>
        </section>
        <div className="auth-panel">
          <main className="auth-main">{children}</main>
          <Attribution />
        </div>
      </div>
    );
  }

  return (
    <div className="app-shell">
      <AppNav />
      <div className="app-content">
        <main className="app-main">{children}</main>
        <Attribution />
      </div>
    </div>
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
          <Shell>
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
          </Shell>
        </WithPlans>
      </AuthProvider>
    </BrowserRouter>
  );
}