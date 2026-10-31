import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';

export default function HomePage() {
  const { user } = useAuth();
  return (
    <section className="card">
      <h2>Welcome, {user.name}</h2>
      <p>Open Training to load your plan and record your results.</p>
      <Link to="/training">Go to training</Link>{' '}
      <Link to="/profile">Edit my profile</Link>
    </section>
  );
}