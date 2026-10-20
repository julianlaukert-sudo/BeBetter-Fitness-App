import { Link } from 'react-router-dom';

export default function SessionCard({ session }) {
  const date = new Date(`${session.date}T12:00:00`).toLocaleDateString('en-GB',
    { weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric' });
  return (
    <article>
      <h3>Session {session.label}</h3>
      <p>{date}</p>
      <p>{session.exercises.length} exercises</p>
      <Link to={`/session/${session.sessionId}`}>View session</Link>
    </article>
  );
}