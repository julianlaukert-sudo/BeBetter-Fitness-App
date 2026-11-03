import test from 'node:test';
import assert from 'node:assert/strict';
import { markPayload } from './markValidation.js';
// KI-Log Nr. 16
const ex = { planExerciseId: 42, bodyweight: false };
const values = { done: 'false', weight: '3,5', reps: '0' };

test('false and zero remain explicit values; decimal comma is supported', () => {
  assert.deepEqual(markPayload(7, ex, values), {
    sessionId: 7, planExerciseId: 42, done: false, weight: 3.5, reps: 0,
  });
});

test('missing result, blank values and fractional reps are rejected', () => {
  for (const patch of [{ done: '' }, { reps: '' }, { weight: '' },
    { reps: '2.5' }, { reps: '201' }, { weight: '-1' },
    { weight: '500.1' }, { weight: '2.55' }, { weight: '1e2' }])
    assert.throws(() => markPayload(7, ex, { ...values, ...patch }));
});

test('bodyweight sends null and does not require a weight input', () => {
  assert.equal(markPayload(7, { ...ex, bodyweight: true },
    { ...values, weight: '' }).weight, null);
});

test('boundary values and explicit success are accepted', () => {
  const result = markPayload(7, ex, { done: 'true', weight: '500', reps: '200' });
  assert.equal(result.done, true);
  assert.equal(result.weight, 500);
  assert.equal(result.reps, 200);
});