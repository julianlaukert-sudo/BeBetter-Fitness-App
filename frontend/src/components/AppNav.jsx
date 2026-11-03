import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Dumbbell, House, LogOut, User } from 'lucide-react';
import { useAuth } from '../context/AuthContext.jsx';

// Hauptbereiche der App
const ITEMS = [
  { to: '/home', label: 'Home', icon: House },
  { to: '/training', label: 'Training', icon: Dumbbell, alsoActive: '/session/' },
  { to: '/profile', label: 'Profile', icon: User },
];

function isActive(item, path) {
  return path === item.to
    || path.startsWith(`${item.to}/`)
    || Boolean(item.alsoActive && path.startsWith(item.alsoActive));
}

// Logo
export function Brand({ to, compact = false }) {
  return (
    <Link to={to} className="brand">
      <Dumbbell className="brand-mark icon-dumbbell" size={compact ? 26 : 32}
        strokeWidth={2.5} aria-hidden="true" />
      <span className="brand-text">
        <span className="brand-name">BeBetter</span>
        {!compact && <span className="brand-tagline">Your goal. Our plan.</span>}
      </span>
    </Link>
  );
}

function NavLinks({ path }) {
  return ITEMS.map(item => {
    const Icon = item.icon;
    const active = isActive(item, path);
    return (
      <Link key={item.to} to={item.to} aria-current={active ? 'page' : undefined}>
        <Icon size={20} aria-hidden="true"
          className={Icon === Dumbbell ? 'icon-dumbbell' : undefined} />
        <span>{item.label}</span>
      </Link>
    );
  });
}

// Desktop
export default function AppNav() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const initial = (user?.name || '').trim().charAt(0).toUpperCase() || '?';

  async function signOut() {
    try { await logout(); } finally { navigate('/login'); }
  }

  const logoutButton = (
    <button type="button" className="icon-button" onClick={signOut}
      aria-label="Log out" title="Log out">
      <LogOut size={20} aria-hidden="true" />
    </button>
  );

  return (
    <>
      <aside className="sidebar">
        <Brand to="/home" />
        <nav className="side-nav" aria-label="Main">
          <NavLinks path={pathname} />
        </nav>
        <div className="sidebar-user">
          <span className="avatar" aria-hidden="true">{initial}</span>
          <span className="sidebar-user-name">{user?.name}</span>
          {logoutButton}
        </div>
      </aside>

      <header className="mobile-bar">
        <Brand to="/home" compact />
        {logoutButton}
      </header>

      <nav className="tab-bar" aria-label="Main">
        <NavLinks path={pathname} />
      </nav>
    </>
  );
}