import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';

export default function HomePage() {
  const { user } = useAuth();
  return (
    <section className="card">
      <h2>Welcome, {user.name}</h2>
      {user.role === 'ADMIN'
        ? <p>The admin area follows in Sprint 5.</p>
        : <>
            <p>Your personal training plan is ready to be created.</p>
            <Link to="/training">Go to training</Link>{' '}
            <Link to="/onboarding">My profile</Link>
          </>}
    </section>
  );
}