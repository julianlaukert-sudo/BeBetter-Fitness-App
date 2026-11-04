import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  ArrowRight, CalendarDays, Check, ChevronRight, Dumbbell, Flame, ListChecks, Target, User, X,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext.jsx';
import { usePlan } from '../context/PlanContext.jsx';
import { loadProfile } from '../api/profile.js';
import ApiDownBanner from '../components/ApiDownBanner.jsx';
import {
    STATUS_LABELS, nextSession, shortDate, statusLabel, timing, weekDays, weekStats, weekdayName,
} from '../utils/planSummary.js';
import nextImage from '../assets/dashboard-next.webp';
import goalImage from '../assets/goal.webp';
// KI-Log Nr. 13
const GOALS = {
  muscle_gain: { title: 'Muscle gain', text: 'Train consistently, record your progress and build muscle.' },
  fat_loss: { title: 'Fat loss', text: 'Stay consistent, record every session and work towards your goal.' },
  health: { title: 'Health', text: 'Stay active, train regularly and keep track of your progress.' },
};

const exerciseCount = n => `${n} ${n === 1 ? 'exercise' : 'exercises'}`;

// Klassenname aus dem Status; fehlende oder unbekannte Werte werden zu "unknown"
const statusKey = status => (Object.hasOwn(STATUS_LABELS, status) ? status.toLowerCase() : 'unknown');

function NextTraining({ plan, session }) {
  if (!session) {
    return (
      <section className="media-card next-card" aria-labelledby="next-title">
        <img className="media-card-image" src={nextImage} alt="" />
        <div className="media-card-body">
          <p className="eyebrow">Next training</p>
          <h2 id="next-title">All sessions are recorded</h2>
          <p className="next-note">Every session of this week has a saved result.</p>
          <Link className="link-button" to="/training">View training plan</Link>
        </div>
      </section>
    );
  }

  const when = timing(session, plan.today);
  const dayText = `${weekdayName(session.date)}, ${shortDate(session.date)}`;
  const recorded = session.exercises.filter(e => e.mark).length;
  const started = session.status === 'IN_PROGRESS';

  // Hauptaktion nach Status und Bearbeitbarkeit: nicht bearbeitbare Einheiten nur ansehen
  let action = 'View session';
  if (session.available) action = started ? 'Continue training' : 'Start training';
  let note = '';
    if (!session) {
      // "Alles erfasst" nur, wenn jede Einheit einen bekannten Ergebnisstatus hat
      const sessions = plan.sessions ?? [];
      const allRecorded = sessions.length > 0
        && sessions.every(s => ['DONE', 'PARTIAL', 'FAILED'].includes(s.status));
      return (
        <section className="media-card next-card" aria-labelledby="next-title">
          <img className="media-card-image" src={nextImage} alt="" />
          <div className="media-card-body">
            <p className="eyebrow">Next training</p>
            <h2 id="next-title">{allRecorded ? 'All sessions are recorded' : 'No session to start'}</h2>
            <p className="next-note">
              {allRecorded ? 'Every session of this week has a saved result.' : 'Open the training plan to see your sessions.'}
            </p>
            <Link className="link-button" to="/training">View training plan</Link>
          </div>
        </section>
      );
    } else if (when === 'earlier') {
    note = 'Still open. You can record it until Sunday.';
  }

  return (
    <section className="media-card next-card" aria-labelledby="next-title">
      <img className="media-card-image" src={nextImage} alt="" />
      <div className="media-card-body">
        <p className="eyebrow">Next training</p>
        <h2 id="next-title">Session {session.label}</h2>
        <ul className="meta-list">
          <li><CalendarDays size={16} aria-hidden="true" />{when === 'today' ? 'Today' : dayText}</li>
          <li><Dumbbell size={16} aria-hidden="true" className="icon-dumbbell" />{exerciseCount(session.exercises.length)}</li>
          {started && (
            <li><ListChecks size={16} aria-hidden="true" />{recorded} of {session.exercises.length} recorded</li>
          )}
        </ul>
        <Link
          className={`link-button${session.available ? ' btn-primary' : ''}`}
          to={`/session/${session.sessionId}`}
        >
          {action}
          {session.available && <ArrowRight size={18} aria-hidden="true" />}
        </Link>
        {note && <p className="next-note">{note}</p>}
        {session.carried && <p className="next-note">Carried over from last week (nothing was recorded).</p>}
      </div>
    </section>
  );
}

function WeekCard({ plan, days }) {
  return (
    <section className="panel week-card" aria-labelledby="week-title">
      <div className="panel-head">
        <h2 id="week-title">This week</h2>
        <span className="panel-range">{shortDate(plan.weekStart)} – {shortDate(plan.weekEnd)}</span>
      </div>
      <ol className="week-strip">
        {days.map(day => {
          const name = weekdayName(day.date);
          const label = day.session
            ? `${name} ${day.day}: Session ${day.session.label},${statusLabel(day.session.status)}`
            : `${name} ${day.day}: rest day`;
          const className = `day day--${day.state}${day.isToday ? ' day--today' : ''}`;
          const content = (
            <>
              <span className="day-name">{day.label}</span>
              <span className="day-num">{day.day}</span>
                            {day.session && <span className="day-session">{day.session.label}</span>}
              <span className="day-mark" aria-hidden="true">
                {(day.state === 'done' || day.state === 'partial') && <Check size={13} strokeWidth={3} />}
                {day.state === 'failed' && <X size={13} strokeWidth={3} />}
              </span>
            </>
          );
          return (
            <li key={day.date}>
              {day.session
                ? <Link className={className} to={`/session/${day.session.sessionId}`}
                    aria-label={label} aria-current={day.isToday ? 'date' : undefined}>{content}</Link>
                : <div className={className} role="img" aria-label={label}
                    aria-current={day.isToday ? 'date' : undefined}>{content}</div>}
            </li>
          );
        })}
      </ol>
      <ul className="week-legend" aria-hidden="true">
        <li><span className="legend-mark legend-mark--done" />Done</li>
        <li><span className="legend-mark legend-mark--started" />Started</li>
        <li><span className="legend-mark" />Open</li>
        <li><span className="legend-mark legend-mark--rest" />Rest day</li>
      </ul>
    </section>
  );
}

function Stats({ stats }) {
  return (
    <section className="stats" aria-label="This week in numbers">
      <div className="panel stat stat--progress">
        <span className="stat-icon"><ListChecks size={22} aria-hidden="true" /></span>
        <div className="stat-text">
          <h3>Exercises recorded</h3>
          <p>{stats.exercisesRecorded} of {stats.exercisesTotal} this week</p>
        </div>
        <div className="progress-row">
          <div className="progress" role="progressbar" aria-label="Exercises recorded"
            aria-valuemin={0} aria-valuemax={100} aria-valuenow={stats.recordedPercent}>
            <span style={{ '--value': `${stats.recordedPercent}%` }} />
          </div>
          <span className="progress-value">{stats.recordedPercent} %</span>
        </div>
      </div>
      <div className="panel stat">
        <span className="stat-icon"><Dumbbell size={22} aria-hidden="true" className="icon-dumbbell" /></span>
        <div className="stat-text">
          <h3>Sessions completed</h3>
          <p>This week</p>
        </div>
        <span className="stat-value">{stats.sessionsCompleted} / {stats.sessionsTotal}</span>
      </div>
      <div className="panel stat">
        <span className="stat-icon"><Flame size={22} aria-hidden="true" /></span>
        <div className="stat-text">
          <h3>Exercises completed</h3>
          <p>This week</p>
        </div>
        <span className="stat-value">{stats.exercisesCompleted} / {stats.exercisesTotal}</span>
      </div>
    </section>
  );
}

function SessionList({ sessions }) {
  return (
    <section className="panel list-card" aria-labelledby="sessions-title">
      <h2 id="sessions-title">Sessions this week</h2>
      <ul className="session-list">
        {sessions.map(s => (
          <li key={s.sessionId}>
            <Link className="session-row" to={`/session/${s.sessionId}`}>
                <span className={`session-row-icon session-row-icon--${statusKey(s.status)}`} aria-hidden="true">
                {s.status === 'DONE' && <Check size={18} strokeWidth={2.5} />}
                {s.status === 'PARTIAL' && <Check size={18} strokeWidth={2.5} />}
                {s.status === 'FAILED' && <X size={18} strokeWidth={2.5} />}
              </span>
              <span className="session-row-text">
                <strong>Session {s.label}</strong>
                <span>{weekdayName(s.date).slice(0, 3)} {shortDate(s.date)} · {exerciseCount(s.exercises.length)}</span>
              </span>
              <span className={`status-text status-text--${statusKey(s.status)}`}>{statusLabel(s.status)}</span>
              <ChevronRight size={18} aria-hidden="true" className="row-chevron" />
            </Link>
          </li>
        ))}
      </ul>
    </section>
  );
}

function GoalCard({ goal }) {
  return (
    <section className="media-card goal-card" aria-labelledby="goal-title">
      <img className="media-card-image" src={goalImage} alt="" />
      <div className="media-card-body goal-card-body">
        <span className="goal-icon"><Target size={30} aria-hidden="true" /></span>
        <div>
          <p className="eyebrow">Your goal</p>
          <h2 id="goal-title">{goal.title}</h2>
          <p>{goal.text}</p>
        </div>
      </div>
    </section>
  );
}

export default function HomePage() {
  const { user, authorized } = useAuth();
  const { plan, busy, error, refresh } = usePlan();
  const [goal, setGoal] = useState(null);
  const name = String(user?.name ?? '').trim();
  const reload = () => refresh().catch(() => {});

  // Nur laden, wenn noch kein Plan im Context liegt (wie SessionPage)
  useEffect(() => { if (!plan) refresh().catch(() => {}); }, [plan, refresh]);

  // Zielkarte: bestehender Profilabruf. Bei Fehlern bleibt die Karte einfach weg.
  useEffect(() => {
    let active = true;
    loadProfile(authorized)
      .then(data => { if (active) setGoal(GOALS[data?.training?.goal] ?? null); })
      .catch(() => {});
    return () => { active = false; };
  }, [user?.userId]);

  const next = plan ? nextSession(plan) : null;
  const days = weekDays(plan);
  const sessions = days.filter(d => d.session).map(d => d.session);
  const trainingToday = Boolean(plan && next && timing(next, plan.today) === 'today');

  return (
    <div className="dashboard">
      <header className="dash-header">
        {plan && <p className="dash-date">{weekdayName(plan.today)}, {shortDate(plan.today)}</p>}
        <h1 className={`dash-title${name.length > 20 ? ' dash-title--long' : ''}`}>{name ? `Hello ${name},` : 'Hello,'}</h1>
        <p className="dash-sub">{trainingToday ? 'Ready for today\'s training?' : 'Here is your week at a glance.'}</p>
      </header>

      <nav className="quick-links" aria-label="Quick links">
        <Link className="quick-link quick-link--primary" to="/training">
          <span className="quick-link-icon"><Dumbbell size={22} aria-hidden="true" className="icon-dumbbell" /></span>
          <span className="quick-link-text"><strong>Training</strong><span>Your current training plan</span></span>
          <ChevronRight size={20} aria-hidden="true" />
        </Link>
        <Link className="quick-link" to="/profile">
          <span className="quick-link-icon"><User size={22} aria-hidden="true" /></span>
          <span className="quick-link-text"><strong>Profile</strong><span>Your data, goal and training days</span></span>
          <ChevronRight size={20} aria-hidden="true" />
        </Link>
      </nav>

      {busy && !plan && <p role="status" className="dash-status">Loading training plan...</p>}
      <div className="dash-banner">
        <ApiDownBanner missing={plan && !plan.exerciseDataAvailable}
          error={error} busy={busy} retry={reload} />
      </div>

      {plan && (
        <>
          <NextTraining plan={plan} session={next} />
          <WeekCard plan={plan} days={days} />
          <Stats stats={weekStats(plan)} />
          <SessionList sessions={sessions} />
        </>
      )}
      {goal && <GoalCard goal={goal} />}
    </div>
  );
}