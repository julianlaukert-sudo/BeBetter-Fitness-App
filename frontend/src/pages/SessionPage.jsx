import { useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, CalendarDays, Info, Lock, Pencil, RefreshCw } from 'lucide-react';
import { usePlan } from '../context/PlanContext.jsx';
import ExerciseRow from '../components/ExerciseRow.jsx';
import ApiDownBanner from '../components/ApiDownBanner.jsx';
import {
  availability, sessionProgress, sessionState, shortDate, weekdayName,
} from '../utils/planSummary.js';

// KI-Log Nr. 27
export default function SessionPage() {
  const { id } = useParams();
  const { plan, busy, error, refresh } = usePlan();
  const reload = () => refresh().catch(() => {});

  useEffect(() => { if (!plan) refresh().catch(() => {}); }, [plan, refresh]);

  const session = plan?.sessions?.find(s => String(s.sessionId) === id);
  const state = session ? sessionState(session, plan.today) : null;
  const access = session ? availability(session, plan.today) : null;
  const progress = session ? sessionProgress(session) : null;
  const dateText = session?.date
    ? `${weekdayName(session.date)} · ${shortDate(session.date)}/${session.date.slice(0, 4)}`
    : '';

  return (
    <div className="session-detail">
      <Link className="back-link" to="/training">
        <ArrowLeft size={18} aria-hidden="true" />
        Back to training overview
      </Link>
      {busy && <p role="status" className="detail-status">Loading session...</p>}
      <div className="detail-banner">
        <ApiDownBanner missing={plan && !plan.exerciseDataAvailable}
          error={error} busy={busy} retry={reload} />
      </div>
      {session
        ? <>
            <header className="detail-header">
              <div className="detail-title-row">
                <h1 className="detail-title">Session {session.label}</h1>
                <span className={`status-text status-text--${state.key}`}>{state.label}</span>
              </div>
              <p className="detail-meta">
                {dateText && (
                  <span><CalendarDays size={16} aria-hidden="true" />{dateText}</span>
                )}
                {session.carried && (
                  <span><RefreshCw size={16} aria-hidden="true" />Carried over from last week (nothing was recorded).</span>
                )}
              </p>
              <div className="detail-progress">
                <span>{progress.recorded} / {progress.total} recorded</span>
                <div className="progress" role="progressbar" aria-label="Session recorded"
                  aria-valuemin={0} aria-valuemax={100} aria-valuenow={progress.percent}>
                  <span style={{ '--value': `${progress.percent}%` }} />
                </div>
                <span className="progress-value">{progress.percent} %</span>
              </div>
              <p className={`session-availability${access.open ? '' : ' is-locked'}`}>
                {access.open ? <Pencil size={14} aria-hidden="true" /> : <Lock size={14} aria-hidden="true" />}
                {access.text}
              </p>
            </header>

            <aside className="notice">
              <span className="notice-icon"><Info size={20} aria-hidden="true" /></span>
              <div>
                <p>One result and one pair of values per exercise.
                  With several sets, enter the values of your last completed set.</p>
                <p>Corrections are possible from the training day until Sunday of this week.</p>
                {!session.available && <p className="notice-strong">This session cannot be edited right now.</p>}
              </div>
            </aside>

            <h2 className="visually-hidden">Exercises</h2>
            <ul className="exercise-list">
              {(session.exercises ?? []).map(e => (
                <ExerciseRow key={e.planExerciseId} exercise={e}
                  sessionId={session.sessionId} available={session.available} />
              ))}
            </ul>
          </>
        : !busy && <p className="detail-empty">Session not found in your current plan. After a week change,
            please go back to the training overview.</p>}
    </div>
  );
}