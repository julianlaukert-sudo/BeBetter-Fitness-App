import { useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { usePlan } from '../context/PlanContext.jsx';
import ExerciseRow from '../components/ExerciseRow.jsx';
import SessionStatus from '../components/SessionStatus.jsx';
import ApiDownBanner from '../components/ApiDownBanner.jsx';

export default function SessionPage() {
  const { id } = useParams();
  const { plan, busy, error, refresh } = usePlan();
  const reload = () => refresh().catch(() => {});

  useEffect(() => { if (!plan) refresh().catch(() => {}); }, [plan, refresh]);

  const session = plan?.sessions.find(s => String(s.sessionId) === id);

  return (
    <section className="card">
      <Link to="/training">Back to training overview</Link>
      {busy && <p role="status">Loading session...</p>}
      <ApiDownBanner missing={plan && !plan.exerciseDataAvailable}
        error={error} busy={busy} retry={reload} />
      {session
        ? <>
            <h2>Session {session.label}</h2>
            <p>{session.date}</p>
            <SessionStatus session={session} />
            <p>One result and one pair of values per exercise.
              With several sets, enter the values of your last completed set.</p>
            <p>Corrections are possible from the training day until Sunday of this week.</p>
            {!session.available && <p>This session cannot be edited right now.</p>}
            <ul>
              {session.exercises.map(e => (
                <ExerciseRow key={e.planExerciseId} exercise={e}
                  sessionId={session.sessionId} available={session.available} />
              ))}
            </ul>
          </>
        : !busy && <p>Session not found in your current plan. After a week change,
            please go back to the training overview.</p>}
    </section>
  );
}