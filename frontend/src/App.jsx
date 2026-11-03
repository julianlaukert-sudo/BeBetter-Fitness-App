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
import AppNav, { Brand } from './components/AppNav.jsx';

// Quellenhinweis der Übungsdaten
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

// App-Layout mit Navigation
function Shell({ children }) {
  const { isAuthenticated } = useAuth();

  if (!isAuthenticated) {
    return (
      <div className="guest-shell">
        <header className="guest-header"><Brand to="/login" /></header>
        <main className="guest-main">{children}</main>
        <Attribution />
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

function OnboardingRoute() {
  const { user } = useAuth();
  return <OnboardingPage key={user.userId} />;
}

function ProfileRoute() {
  const { user } = useAuth();
  return <ProfilePage key={user.userId} />;
}

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