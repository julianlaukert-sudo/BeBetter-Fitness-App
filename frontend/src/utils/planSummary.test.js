import test from 'node:test';
import assert from 'node:assert/strict';
import {
    addDays, availability, exerciseTarget, markSummary, nextSession, sessionProgress, sessionState,
    shortDate, statusLabel, timing, weekDays, weekStats, weekdayName,
} from './planSummary.js';

const mark = done => ({ id: 1, date: '2026-11-02', done, weight: 10, reps: 8, automatic: false });
const exercise = m => ({ planExerciseId: 1, mark: m });
const session = (label, date, status, exercises = [], extra = {}) => (
    { sessionId: label.charCodeAt(0), label, date, status, exercises, ...extra }
);

const plan = {
    weekStart: '2026-11-02',
    weekEnd: '2026-11-08',
    today: '2026-11-04',
    sessions: [
        session('C', '2026-11-06', 'OPEN', [exercise(null), exercise(null)]),
        session('A', '2026-11-02', 'DONE', [exercise(mark(true)), exercise(mark(true))]),
        session('B', '2026-11-04', 'IN_PROGRESS', [exercise(mark(false)), exercise(null)]),
    ],
};

const withStatus = (statuses) => ({
    ...plan,
    sessions: plan.sessions.map(s => ({ ...s, status: statuses[s.label] ?? s.status })),
});

test('nextSession: die Einheit von heute hat Vorrang, auch wenn eine frühere noch offen ist', () => {
    assert.equal(nextSession(withStatus({ A: 'OPEN' })).label, 'B');
    assert.equal(nextSession(plan).label, 'B');
});

test('nextSession: ohne offene Einheit heute kommt die früheste noch offene frühere Einheit vor der kommenden', () => {
    // A (Mo) offen, B (heute) abgeschlossen, C (Fr) offen
    assert.equal(nextSession(withStatus({ A: 'OPEN', B: 'DONE' })).label, 'A');
});

test('nextSession: sind nur kommende Einheiten offen, gilt die früheste davon', () => {
    assert.equal(nextSession(withStatus({ B: 'PARTIAL' })).label, 'C');
});

test('nextSession: unabhängig von der Reihenfolge der Liste, mehrere frühere Einheiten nach Datum', () => {
    const many = {
        today: '2026-11-07',
        sessions: [
            session('C', '2026-11-06', 'OPEN'),
            session('B', '2026-11-04', 'IN_PROGRESS'),
            session('A', '2026-11-02', 'DONE'),
        ],
    };
    assert.equal(nextSession(many).label, 'B');
});

test('nextSession: das Merkmal carried ändert die Reihenfolge nicht', () => {
    const carried = withStatus({ A: 'OPEN', B: 'DONE' });
    carried.sessions = carried.sessions.map(s => (s.label === 'C' ? { ...s, carried: true } : s));
    assert.equal(nextSession(carried).label, 'A');
});

test('nextSession: null, wenn alles markiert ist oder kein Plan vorliegt', () => {
    assert.equal(nextSession(withStatus({ B: 'PARTIAL', C: 'FAILED' })), null);
    assert.equal(nextSession(null), null);
    assert.equal(nextSession({ ...plan, sessions: [] }), null);
});

test('nextSession: ohne "today" gilt einfach die früheste offene Einheit', () => {
    const { today, ...noToday } = withStatus({ A: 'OPEN' });
    assert.equal(nextSession(noToday).label, 'A');
});

test('timing: heute, früher, später', () => {
    assert.equal(timing({ date: '2026-11-04' }, '2026-11-04'), 'today');
    assert.equal(timing({ date: '2026-11-02' }, '2026-11-04'), 'earlier');
    assert.equal(timing({ date: '2026-11-06' }, '2026-11-04'), 'later');
});

test('weekDays: sieben Tage ab Montag mit Zustand und heutigem Tag', () => {
    const days = weekDays(plan);
    assert.equal(days.length, 7);
    assert.deepEqual(days.map(d => d.label), ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']);
    assert.deepEqual(days.map(d => d.day), [2, 3, 4, 5, 6, 7, 8]);
    assert.deepEqual(days.map(d => d.state), ['done', 'none', 'started', 'none', 'open', 'none', 'none']);
    assert.deepEqual(days.map(d => d.isToday), [false, false, true, false, false, false, false]);
    assert.equal(days[2].session.label, 'B');
    assert.deepEqual(weekDays(null), []);
});

test('weekDays: Wochenwechsel über Monatsgrenze', () => {
    const days = weekDays({ weekStart: '2026-10-26', today: '2026-10-29', sessions: [] });
    assert.deepEqual(days.map(d => d.day), [26, 27, 28, 29, 30, 31, 1]);
    assert.equal(days[6].date, '2026-11-01');
});

test('weekStats: erfasste (recorded) und erfolgreich abgeschlossene (completed) Übungen werden getrennt gezählt', () => {
    // 6 Übungen: A zweimal Done, B einmal Not done, B einmal ohne Ergebnis, C zweimal ohne Ergebnis
    assert.deepEqual(weekStats(plan), {
        sessionsTotal: 3,
        sessionsCompleted: 1,
        exercisesTotal: 6,
        exercisesRecorded: 3,
        exercisesCompleted: 2,
        recordedPercent: 50,
    });
});

test('weekStats: "Not done" zählt als erfasst, aber nicht als abgeschlossen', () => {
    const onlyFailed = { sessions: [session('A', '2026-11-02', 'FAILED', [exercise(mark(false)), exercise(mark(false))])] };
    const stats = weekStats(onlyFailed);
    assert.equal(stats.exercisesRecorded, 2);
    assert.equal(stats.exercisesCompleted, 0);
    assert.equal(stats.recordedPercent, 100);
    assert.equal(stats.sessionsCompleted, 0);
});

test('weekStats: ohne Plan oder ohne Übungen alles null', () => {
    const empty = {
        sessionsTotal: 0, sessionsCompleted: 0, exercisesTotal: 0,
        exercisesRecorded: 0, exercisesCompleted: 0, recordedPercent: 0,
    };
    assert.deepEqual(weekStats(null), empty);
    assert.deepEqual(weekStats({ sessions: [] }), empty);
});

test('Datumshilfen', () => {
    assert.equal(addDays('2026-10-30', 3), '2026-11-02');
    assert.equal(weekdayName('2026-11-03'), 'Tuesday');
    assert.equal(weekdayName('2026-11-08'), 'Sunday');
    assert.equal(shortDate('2026-11-06'), '06/11');
});

test('sessionProgress: zählt gespeicherte Ergebnisse, auch "Not done"', () => {
    const [c, a, b] = plan.sessions;
    assert.deepEqual(sessionProgress(a), { total: 2, recorded: 2, percent: 100 });
    assert.deepEqual(sessionProgress(b), { total: 2, recorded: 1, percent: 50 });
    assert.deepEqual(sessionProgress(c), { total: 2, recorded: 0, percent: 0 });
});

test('sessionProgress: rundet den Prozentwert und erkennt automatische Markierungen als erfasst', () => {
    const auto = { id: 2, date: '2026-11-08', done: false, weight: null, reps: 0, automatic: true };
    const s = session('A', '2026-11-02', 'IN_PROGRESS', [exercise(mark(true)), exercise(auto), exercise(null)]);
    assert.deepEqual(sessionProgress(s), { total: 3, recorded: 2, percent: 67 });
});

test('sessionProgress: ohne Einheit oder Übungen alles null', () => {
    const empty = { total: 0, recorded: 0, percent: 0 };
    assert.deepEqual(sessionProgress(null), empty);
    assert.deepEqual(sessionProgress(session('A', '2026-11-02', 'OPEN')), empty);
    assert.deepEqual(sessionProgress({ status: 'OPEN' }), empty);
});

test('sessionState: noch nicht erfasste Einheit nach Lage zu heute', () => {
    const today = '2026-11-04';
    assert.deepEqual(sessionState(session('B', '2026-11-04', 'OPEN'), today), { key: 'today', label: 'Today' });
    assert.deepEqual(sessionState(session('A', '2026-11-02', 'OPEN'), today), { key: 'open', label: 'Open' });
    assert.deepEqual(sessionState(session('C', '2026-11-06', 'OPEN'), today), { key: 'planned', label: 'Planned' });
});

test('sessionState: Ergebnisstatus haben Vorrang vor dem Datum', () => {
    const today = '2026-11-04';
    assert.deepEqual(sessionState(session('B', '2026-11-04', 'IN_PROGRESS'), today), { key: 'started', label: 'Started' });
    assert.deepEqual(sessionState(session('A', '2026-11-02', 'DONE'), today), { key: 'done', label: 'Done' });
    assert.deepEqual(sessionState(session('A', '2026-11-02', 'PARTIAL'), today), { key: 'partial', label: 'Partially done' });
    assert.deepEqual(sessionState(session('A', '2026-11-02', 'FAILED'), today), { key: 'failed', label: 'Not done' });
});

test('sessionState: ohne "today" wird eine offene Einheit als Open angezeigt', () => {
    assert.deepEqual(sessionState(session('A', '2026-11-02', 'OPEN')), { key: 'open', label: 'Open' });
});

test('sessionState: unbekannter oder fehlender Status wird nie als Open angezeigt', () => {
    const unknown = { key: 'unknown', label: 'Status unknown' };
    for (const status of ['ARCHIVED', 'open', '', undefined, null, 'constructor']) {
        assert.deepEqual(sessionState(session('A', '2026-11-04', status), '2026-11-04'), unknown, String(status));
    }
});

test('weekDays: unbekannter Status wird nicht als offen dargestellt', () => {
    const days = weekDays({ weekStart: '2026-11-02', today: '2026-11-04', sessions: [session('A', '2026-11-02', 'ARCHIVED')] });
    assert.equal(days[0].state, 'unknown');
    assert.equal(days[1].state, 'none');
});

test('availability: bearbeitbare Einheit, auch mit Ergebnis (Korrektur bis Sonntag)', () => {
    const open = { open: true, text: 'Recording open until Sunday' };
    assert.deepEqual(availability(session('B', '2026-11-04', 'OPEN', [], { available: true }), '2026-11-04'), open);
    assert.deepEqual(availability(session('A', '2026-11-02', 'DONE', [], { available: true }), '2026-11-04'), open);
});

test('availability: kommende gesperrte Einheit nennt Tag und Datum', () => {
    const c = session('C', '2026-11-06', 'OPEN', [], { available: false });
    assert.deepEqual(availability(c, '2026-11-04'), { open: false, text: 'Opens Friday, 06/11' });
});

test('availability: gesperrt ohne kommenden Tag oder ohne "today" zeigt keinen Öffnungstermin', () => {
    const closed = { open: false, text: 'Recording closed' };
    assert.deepEqual(availability(session('A', '2026-11-02', 'OPEN', [], { available: false }), '2026-11-04'), closed);
    assert.deepEqual(availability(session('C', '2026-11-06', 'OPEN', [], { available: false })), closed);
    assert.deepEqual(availability(session('C', '2026-11-06', 'OPEN')), closed);
});

test('exerciseTarget: Kurzhantel pro Hand, Gerät ohne Zusatz, Gewicht als Zahl', () => {
    const base = { exerciseId: '1', name: 'dumbbell bench press', sets: 3, reps: '8-12', bodyweight: false };
    assert.deepEqual(exerciseTarget({ ...base, weight: 11.0, equipment: 'dumbbell' }),
        { name: 'dumbbell bench press', scheme: '3 × 8–12', load: '11 kg per hand' });
    assert.equal(exerciseTarget({ ...base, weight: '4.5', equipment: 'dumbbell' }).load, '4.5 kg per hand');
    assert.equal(exerciseTarget({ ...base, weight: 25, equipment: 'leverage machine' }).load, '25 kg');
    assert.equal(exerciseTarget({ ...base, weight: 25, equipment: null }).load, '25 kg');
});

test('exerciseTarget: Körpergewicht ohne Zusatzgewicht, fehlendes Gewicht ohne Angabe', () => {
    const base = { exerciseId: '2', name: 'plank', sets: 2, reps: '12-15' };
    assert.equal(exerciseTarget({ ...base, bodyweight: true, weight: null }).load, 'Bodyweight');
    assert.equal(exerciseTarget({ ...base, bodyweight: true, weight: 5 }).load, 'Bodyweight');
    assert.equal(exerciseTarget({ ...base, bodyweight: false, weight: null }).load, '');
    assert.equal(exerciseTarget({ ...base, bodyweight: false, weight: 'abc' }).load, '');
});

test('exerciseTarget: Name-Fallback bei fehlenden Übungsdaten und Satzschema', () => {
    const noName = exerciseTarget({ exerciseId: '0099', name: null, sets: 2, reps: '12-15', weight: 4, bodyweight: false });
    assert.equal(noName.name, 'Exercise 0099');
    assert.equal(noName.scheme, '2 × 12–15');
    assert.equal(exerciseTarget({ exerciseId: '3', name: 'x', sets: 3, reps: '12', bodyweight: true }).scheme, '3 × 12');
});

test('statusLabel: bekannte Status und neutraler Fallback', () => {
    assert.equal(statusLabel('OPEN'), 'Open');
    assert.equal(statusLabel('IN_PROGRESS'), 'Started');
    assert.equal(statusLabel('DONE'), 'Done');
    assert.equal(statusLabel('FAILED'), 'Not done');
    assert.equal(statusLabel('PARTIAL'), 'Partially done');
    for (const status of ['ARCHIVED', 'open', '', undefined, null, 'constructor']) {
        assert.equal(statusLabel(status), 'Status unknown', String(status));
    }
});

// KI-Log Nr. 27
test('markSummary: ohne Markierung', () => {
    assert.deepEqual(markSummary(null), { key: 'none', text: 'Not marked yet' });
    assert.deepEqual(markSummary(undefined), { key: 'none', text: 'Not marked yet' });
});

test('markSummary: automatische Markierung hat Vorrang', () => {
    const auto = { id: 2, date: '2026-11-08', done: false, weight: null, reps: 0, automatic: true };
    assert.deepEqual(markSummary(auto), { key: 'automatic', text: 'Automatically not done' });
    assert.equal(markSummary({ ...auto, done: true }).key, 'automatic');
});

test('markSummary: nur Done oder Not done, keine Istwerte', () => {
    assert.deepEqual(markSummary(mark(true)), { key: 'done', text: 'Done' });
    assert.deepEqual(markSummary(mark(false)), { key: 'failed', text: 'Not done' });
});