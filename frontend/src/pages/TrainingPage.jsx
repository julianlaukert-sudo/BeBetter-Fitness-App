import { useEffect } from 'react';
import { Info, RefreshCw } from 'lucide-react';
import { usePlan } from '../context/PlanContext.jsx';
import ApiDownBanner from '../components/ApiDownBanner.jsx';
import SessionCard from '../components/SessionCard.jsx';
import { shortDate } from '../utils/planSummary.js';

// KI-Log Nr. 13, 16
export default function TrainingPage() {
  const { plan, busy, error, refresh } = usePlan();
  // Fehler werden über "error" angezeigt; hier nur abfangen
  const reload = () => refresh().catch(() => {});

  useEffect(() => { refresh().catch(() => {}); }, [refresh]);

  const sessions = plan?.sessions ?? [];
  const allCarried = sessions.length > 0 && sessions.every(s => s.carried);

  return (
    <div className="training">
      <header className="training-header">
        <div>
          <h1 className="training-title">Training plan</h1>
          {plan && (
            <p className="training-sub">
              {shortDate(plan.weekStart)} – {shortDate(plan.weekEnd)}/{plan.weekEnd.slice(0, 4)}
              {' · '}Sessions {sessions.map(s => s.label).join(', ')}
            </p>
          )}
        </div>
        <button type="button" className="refresh-button" disabled={busy} onClick={reload}>
          <RefreshCw size={16} aria-hidden="true" />
          Refresh plan
        </button>
      </header>

      {busy && <p role="status" className="training-status">Loading training plan...</p>}
      <div className="training-banner">
        <ApiDownBanner missing={plan && !plan.exerciseDataAvailable}
          error={error} busy={busy} retry={reload} />
      </div>

      {plan && (
        <>
          {allCarried && (
            <aside className="notice">
              <span className="notice-icon"><RefreshCw size={20} aria-hidden="true" /></span>
              <div>
                <h2>Last week had no recorded results</h2>
                <p>All sessions were carried over unchanged.</p>
              </div>
            </aside>
          )}

          <div className="session-grid">
            {sessions.map(s => <SessionCard key={s.sessionId} session={s} today={plan.today} />)}
          </div>

          <details className="panel plan-info">
            <summary>
              <Info size={18} aria-hidden="true" />
              How your plan works
            </summary>
            <div className="plan-info-body">
              <p>Start suggestions are technical model values, not measured strength values.</p>
              <ul>
                <li>You train three sessions per week on your training days, with five exercises each.</li>
                <li>Sets and repetitions depend on your goal: 2 sets for Health, otherwise 3 sets; 8–12 repetitions for Muscle gain, otherwise 12–15.</li>
                <li>Training targets are calculated from your fitness level, body data and age. Dumbbell weights are per hand.</li>
                <li>You can record and correct results from the training day until Sunday. Future sessions can be viewed but not edited yet.</li>
                <li>Each new week keeps the same targets. There is no automatic increase.</li>
                <li>At the end of a week, open exercises of partially recorded sessions count as not done. Sessions without any results are carried over.</li>
                <li>Changes to your profile apply from the next Monday.</li>
              </ul>
            </div>
          </details>
        </>
      )}
    </div>
  );
}