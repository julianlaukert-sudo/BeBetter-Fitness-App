import test from 'node:test';
import assert from 'node:assert/strict';
import { registrationErrors, profileErrors, profileBody } from './validation.js';
import { loadDraft, saveDraft, clearDraft } from './onboardingDraft.js';

const account = {
    name: 'Test', email: 'test@example.org', password: 'Secure123!', confirm: 'Secure123!',
};
const profile = {
    age: '23', gender: 'male', height: '180', weight: '78,5',
    goal: 'health', fitnessLevel: 'beginner', trainingDays: ['MON', 'WED', 'FRI'],
};

test('valid registration and profile', () => {
    assert.deepEqual(registrationErrors(account), {});
    assert.deepEqual(profileErrors(profile), {});
    assert.equal(profileBody(profile).personal.weight, 78.5);
});

test('password confirmation and UTF-8 length', () => {
    assert.ok(registrationErrors({ ...account, confirm: 'different' }).confirm);
    const password = 'Ä'.repeat(36) + '1!'; // 38 Zeichen, aber 74 Byte
    assert.ok(registrationErrors({ ...account, password, confirm: password }).password);
});

test('missing uppercase, number or special character', () => {
    for (const password of ['secure123!', 'SecureABC!', 'Secure1234'])
        assert.ok(registrationErrors({ ...account, password, confirm: password }).password);
});

test('profile boundaries', () => {
    for (const age of ['15', '100', '16.5']) assert.ok(profileErrors({ ...profile, age }).age);
    for (const age of ['16', '99']) assert.equal(profileErrors({ ...profile, age }).age, undefined);
    assert.ok(profileErrors({ ...profile, weight: '78.55' }).weight);
    assert.ok(profileErrors({ ...profile, height: '231' }).height);
});

test('three separate days and week boundary', () => {
    for (const trainingDays of [['MON', 'WED'], ['MON', 'MON', 'FRI'],
        ['MON', 'TUE', 'FRI'], ['MON', 'WED', 'SUN']])
        assert.ok(profileErrors({ ...profile, trainingDays }).trainingDays);
});

test('drafts are isolated per user and cleared after saving', () => {
    const store = new Map();
    globalThis.localStorage = {
        getItem: k => store.get(k) || null,
        setItem: (k, v) => store.set(k, v),
        removeItem: k => store.delete(k),
    };
    saveDraft(1, profile);
    assert.equal(loadDraft(1).age, '23');
    assert.equal(loadDraft(2).age, '');
    clearDraft(1);
    assert.equal(loadDraft(1).age, '');
    store.set('bebetter.onboarding.1', 'broken');
    assert.equal(loadDraft(1).age, '');
});

test('blocked browser storage does not crash', () => {
    globalThis.localStorage = { getItem() { throw Error(); }, setItem() { throw Error(); } };
    assert.equal(loadDraft(1).age, '');
    assert.equal(saveDraft(1, profile), false);
});