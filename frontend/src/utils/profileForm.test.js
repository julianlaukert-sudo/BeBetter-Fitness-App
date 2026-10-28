import test from 'node:test';
import assert from 'node:assert/strict';
import { profileForm } from './profileForm.js';
import { profileBody, profileErrors } from './validation.js';

test('server profile becomes editable strings without sharing day array', () => {
  const data = {
    personal: { age: 23, gender: 'male', height: 180, weight: 78.5 },
    training: { goal: 'health', fitnessLevel: 'beginner', trainingDays: ['MON', 'WED', 'FRI'] },
  };
  const form = profileForm(data);
  assert.equal(form.weight, '78.5');
  assert.deepEqual(profileErrors(form), {});
  assert.deepEqual(profileBody(form), { personal: data.personal, training: data.training });
  form.trainingDays.push('SUN');
  assert.equal(data.training.trainingDays.length, 3);
});