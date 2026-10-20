import { useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { usePlan } from '../context/PlanContext.jsx';
import ExerciseRow from '../components/ExerciseRow.jsx';
import ApiDownBanner from '../components/ApiDownBanner.jsx';

export default function SessionPage() {
  const { id } = useParams();
  const { plan, busy, error, refresh } = usePlan();

  // Direktlink / Neuladen: Plan zuerst laden
  useEffect(() => { if (!plan) refresh(); }, [plan, refresh]);

  // Nur Einheiten aus dem EIGENEN Plan – fremde IDs werden nicht gefunden
  const session = plan?.sessions.find(s => String(s.sessionId) === id);

  return (
    <section className="card">
      <Link to="/training">Back to training overview</Link>
      {busy && <p role="status">Loading session...</p>}
      <ApiDownBanner missing={plan && !plan.exerciseDataAvailable}
        error={error} busy={busy} retry={refresh} />
      {session
        ? <>
            <h2>Session {session.label}</h2>
            <p>{session.date}</p>
            <ul>
              {session.exercises.map(e => <ExerciseRow key={e.planExerciseId} exercise={e} />)}
            </ul>
            <p>Recording your training values follows in Sprint 4.</p>
          </>
        : !busy && <p>Session not found in your current plan.</p>}
    </section>
  );
}