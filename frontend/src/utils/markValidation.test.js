import test from 'node:test';
import assert from 'node:assert/strict';
import { markPayload } from './markValidation.js';
// KI-Log Nr. 16, 32
const ex = { planExerciseId: 42, bodyweight: false };

test('done and not done are sent without weight or reps', () => {
    assert.deepEqual(markPayload(7, ex, true), { sessionId: 7, planExerciseId: 42, done: true });
    assert.deepEqual(markPayload(7, ex, false), { sessionId: 7, planExerciseId: 42, done: false });
});

test('bodyweight exercises use the same payload', () => {
    assert.deepEqual(markPayload(7, { ...ex, bodyweight: true }, true),
        { sessionId: 7, planExerciseId: 42, done: true });
});

test('missing or non-boolean result is rejected', () => {
    for (const value of [undefined, null, '', 'true', 1])
        assert.throws(() => markPayload(7, ex, value));
});