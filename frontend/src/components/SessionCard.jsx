import { Link } from 'react-router-dom';
import { ArrowRight, CalendarDays, Lock, Pencil, RefreshCw } from 'lucide-react';
import {
  availability, exerciseTarget, sessionProgress, sessionState, shortDate, timing, weekdayName,
} from '../utils/planSummary.js';
import sessionA from '../assets/session-a.webp';
import sessionB from '../assets/session-b.webp';
import sessionC from '../assets/session-c.webp';

const IMAGES = { A: sessionA, B: sessionB, C: sessionC };

export default function SessionCard({ session, today }) {
  const state = sessionState(session, today);
  const access = availability(session, today);
  const progress = sessionProgress(session);
  const isToday = timing(session, today) === 'today';
  const image = Object.hasOwn(IMAGES, session.label) ? IMAGES[session.label] : null;
  const exercises = session.exercises ?? [];
  const titleId = `session-${session.sessionId}-title`;

  // Aktion nach Status und Bearbeitbarkeit: gesperrte Einheiten nur ansehen
  let action = 'View session';
  if (access.open) action = session.status === 'IN_PROGRESS' ? 'Continue session' : 'Open session';

  return (
    <article
      className={`session-card${isToday ? ' session-card--today' : ''}`}
      aria-labelledby={titleId}
      aria-current={isToday ? 'date' : undefined}
    >
      <div className="session-card-head">
        {image && <img className="session-card-image" src={image} alt="" loading="lazy" decoding="async" />}
        <div className="session-card-meta">
          <p className="session-date">
            <CalendarDays size={16} aria-hidden="true" />
            {weekdayName(session.date)} · {shortDate(session.date)}
          </p>
          <span className={`status-text status-text--${state.key}`}>{state.label}</span>
        </div>
        <h2 id={titleId}>Session {session.label}</h2>
        {session.carried && (
          <p className="session-carried">
            <RefreshCw size={14} aria-hidden="true" />
            Carried over from last week
          </p>
        )}
      </div>

      <div className="session-card-body">
        <ul className="session-exercises">
          {exercises.map(exercise => {
            const target = exerciseTarget(exercise);
            return (
              <li key={exercise.planExerciseId}>
                <span className="ex-name">{target.name}</span>
                <span className="ex-spec">{target.scheme}{target.load && ` · ${target.load}`}</span>
              </li>
            );
          })}
        </ul>

        <div className="session-progress">
          <span>{progress.recorded} / {progress.total} recorded</span>
          <div className="progress" role="progressbar" aria-label={`Session ${session.label} recorded`}
            aria-valuemin={0} aria-valuemax={100} aria-valuenow={progress.percent}>
            <span style={{ '--value': `${progress.percent}%` }} />
          </div>
        </div>

        <div className="session-foot">
          <p className={`session-availability${access.open ? '' : ' is-locked'}`}>
            {access.open ? <Pencil size={14} aria-hidden="true" /> : <Lock size={14} aria-hidden="true" />}
            {access.text}
          </p>
          <Link
            className={`session-link${access.open ? ' is-primary' : ''}`}
            to={`/session/${session.sessionId}`}
            aria-label={`${action} ${session.label}`}
          >
            {action}
            {access.open && <ArrowRight size={16} aria-hidden="true" />}
          </Link>
        </div>
      </div>
    </article>
  );
}