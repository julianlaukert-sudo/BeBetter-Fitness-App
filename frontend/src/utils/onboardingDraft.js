// Browserentwurf des Onboardings, getrennt pro Nutzer-ID.
// Enthält nur Profildaten – keine Passwörter, keine Tokens.
const key = id => `bebetter.onboarding.${id}`;
const DAYS = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'];

export const empty = {
  age: '', gender: '', height: '', weight: '',
  goal: '', fitnessLevel: '', trainingDays: [],
};

export function loadDraft(id) {
  try {
    const data = JSON.parse(localStorage.getItem(key(id)));
    if (!data || !Array.isArray(data.trainingDays)) return { ...empty };
    return { ...empty, ...data, trainingDays: data.trainingDays.filter(d => DAYS.includes(d)) };
  } catch {
    return { ...empty };
  }
}

export function saveDraft(id, value) {
  try {
    localStorage.setItem(key(id), JSON.stringify(value));
    return true;
  } catch {
    return false;
  }
}

export function clearDraft(id) {
  try {
    localStorage.removeItem(key(id));
  } catch {
    /* Speichern im Backend bleibt trotzdem gültig. */
  }
}