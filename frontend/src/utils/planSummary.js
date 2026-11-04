// Reine Hilfsfunktionen für das Dashboard. Sie rechnen nur mit den Daten aus

const WEEKDAYS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];

export const STATUS_LABELS = {
  OPEN: 'Open',
  IN_PROGRESS: 'Started',
  DONE: 'Done',
  FAILED: 'Not done',
  PARTIAL: 'Partially done',
};

// Anzeigezustand pro Wochentag
const DAY_STATES = {
  OPEN: 'open',
  IN_PROGRESS: 'started',
  DONE: 'done',
  PARTIAL: 'partial',
  FAILED: 'failed',
};

// Mittags, damit Zeitzonen und Zeitumstellung das Datum nicht verschieben
const parseDate = iso => new Date(`${iso}T12:00:00`);

function isoOf(date) {
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

export function addDays(iso, days) {
  const date = parseDate(iso);
  date.setDate(date.getDate() + days);
  return isoOf(date);
}

export function weekdayName(iso) {
  return WEEKDAYS[(parseDate(iso).getDay() + 6) % 7];
}

export function shortDate(iso) {
  return `${iso.slice(8, 10)}/${iso.slice(5, 7)}`;
}

function sortedSessions(plan) {
  return [...(plan?.sessions ?? [])].sort((a, b) => a.date.localeCompare(b.date));
}

export function timing(session, today) {
  if (session.date === today) return 'today';
  return session.date < today ? 'earlier' : 'later';
}

const TIMING_RANK = { today: 0, earlier: 1, later: 2 };

export function nextSession(plan) {
  return sortedSessions(plan)
    .filter(s => s.status === 'OPEN' || s.status === 'IN_PROGRESS')
    .sort((a, b) => TIMING_RANK[timing(a, plan.today)] - TIMING_RANK[timing(b, plan.today)])[0]
    ?? null;
}

// Sieben Tage ab weekStart mit der Einheit des Tages, falls es eine gibt
export function weekDays(plan) {
  if (!plan) return [];
  const sessions = plan.sessions ?? [];
  return WEEKDAYS.map((name, i) => {
    const date = addDays(plan.weekStart, i);
    const session = sessions.find(s => s.date === date) ?? null;
    return {
      date,
      label: name.slice(0, 3),
      day: Number(date.slice(8, 10)),
      isToday: date === plan.today,
      session,
      state: session ? (Object.hasOwn(DAY_STATES, session.status) ? DAY_STATES[session.status] : 'unknown') : 'none',
    };
  });
}

// Kennzahlen der Woche, ausschließlich aus Status und Markierungen des Plans.
// recorded: Ergebnis gespeichert .completed: als Done gespeichert.
export function weekStats(plan) {
  const sessions = plan?.sessions ?? [];
  const exercises = sessions.flatMap(s => s.exercises ?? []);
  const recorded = exercises.filter(e => e.mark).length;
  return {
    sessionsTotal: sessions.length,
    sessionsCompleted: sessions.filter(s => s.status === 'DONE').length,
    exercisesTotal: exercises.length,
    exercisesRecorded: recorded,
    exercisesCompleted: exercises.filter(e => e.mark?.done).length,
    recordedPercent: exercises.length ? Math.round((recorded / exercises.length) * 100) : 0,
  };
}

// Fortschritt einer Einheit: erfasst = Ergebnis gespeichert (Done oder Not done)
export function sessionProgress(session) {
  const exercises = session?.exercises ?? [];
  const recorded = exercises.filter(e => e.mark).length;
  return {
    total: exercises.length,
    recorded,
    percent: exercises.length ? Math.round((recorded / exercises.length) * 100) : 0,
  };
}

// Anzeigestatus einer Einheit.zeigen zusätzlich die Lage zum heutigen Datum
const UNKNOWN_STATUS = 'Status unknown';

export function sessionState(session, today) {
  const status = session.status;
  if (!Object.hasOwn(STATUS_LABELS, status)) return { key: 'unknown', label: UNKNOWN_STATUS };
  if (status !== 'OPEN') return { key: DAY_STATES[status], label: STATUS_LABELS[status] };
  if (!today) return { key: 'open', label: STATUS_LABELS.OPEN };
  const when = timing(session, today);
  if (when === 'today') return { key: 'today', label: 'Today' };
  if (when === 'later') return { key: 'planned', label: 'Planned' };
  return { key: 'open', label: STATUS_LABELS.OPEN };
}

// Bearbeitbarkeit laut Backend. Gesperrte kommende Einheiten öffnen an ihrem Tag.
export function availability(session, today) {
  if (session.available) return { open: true, text: 'Recording open until Sunday' };
  if (today && timing(session, today) === 'later') {
    return { open: false, text: `Opens ${weekdayName(session.date)}, ${shortDate(session.date)}` };
  }
  return { open: false, text: 'Recording closed' };
}

// Anzeige einer Übung in der Trainingsübersicht: Name, Satzschema und Zielgewicht
export function exerciseTarget(exercise) {
  const weight = Number(exercise.weight);
  let load = '';
  if (exercise.bodyweight) {
    load = 'Bodyweight';
  } else if (exercise.weight != null && Number.isFinite(weight)) {
    load = `${weight} kg`;
    if (exercise.equipment === 'dumbbell') load += ' per hand';
  }
  return {
    name: exercise.name || `Exercise ${exercise.exerciseId}`,
    scheme: `${exercise.sets} × ${String(exercise.reps).replace('-', '–')}`,
    load,
  };
}

// Statustext für bekannte und unbekannte Statuswerte
export function statusLabel(status) {
  return Object.hasOwn(STATUS_LABELS, status)
    ? STATUS_LABELS[status]
    : 'Status unknown';
}

// KI-Log Nr. 27
// Ergebnistext einer Übung (bisher markText in ExerciseRow) mit Schlüssel für die Anzeige
export function markSummary(mark) {
  if (!mark) return { key: 'none', text: 'Not marked yet' };
  if (mark.automatic) return { key: 'automatic', text: 'Automatically not done; no measured values.' };
  return {
    key: mark.done ? 'done' : 'failed',
    text: `${mark.done ? 'Done' : 'Not done'}: ${mark.reps} reps` + (mark.weight == null ? '' : `, ${mark.weight} kg`),
  };
}