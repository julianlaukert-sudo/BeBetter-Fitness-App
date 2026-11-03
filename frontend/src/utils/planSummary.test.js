import test from 'node:test';
import assert from 'node:assert/strict';
import {
    addDays, nextSession, shortDate, timing, weekDays, weekStats, weekdayName,
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