import { useEffect } from 'react';
import { usePlan } from '../context/PlanContext.jsx';
import ApiDownBanner from '../components/ApiDownBanner.jsx';
import SessionCard from '../components/SessionCard.jsx';

export default function TrainingPage() {
  const { plan, busy, error, refresh } = usePlan();
  // Fehler werden über "error" angezeigt; hier nur abfangen
  const reload = () => refresh().catch(() => {});

  useEffect(() => { refresh().catch(() => {}); }, [refresh]);

  return (
    <section className="card">
      <h2>Your training</h2>
      {busy && <p role="status">Loading training plan...</p>}
      <ApiDownBanner missing={plan && !plan.exerciseDataAvailable}
        error={error} busy={busy} retry={reload} />
      {plan && <>
        <p>Week {plan.weekStart} to {plan.weekEnd}</p>
        <p>Start suggestions are technical model values, not measured strength values.</p>
        {plan.sessions.map(s => <SessionCard key={s.sessionId} session={s} />)}
        <button type="button" disabled={busy} onClick={reload}>Refresh plan</button>
      </>}
    </section>
  );
}