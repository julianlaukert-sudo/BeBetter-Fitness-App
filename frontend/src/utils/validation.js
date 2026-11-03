// KI-Log Nr. 9, 11
const EMAIL_PATTERN = /^[^@\s]+@[^@\s]+\.[^@\s.]{2,}$/
export const NAME_MIN_LENGTH = 2
export const NAME_MAX_LENGTH = 50
export const EMAIL_MAX_LENGTH = 255

export function validateName(name) {
  const trimmed = name.trim()
  if (!trimmed) return 'Please enter your name.'
  if (trimmed.length < NAME_MIN_LENGTH || trimmed.length > NAME_MAX_LENGTH) {
    return `Name must be ${NAME_MIN_LENGTH} to ${NAME_MAX_LENGTH} characters long.`
  }
  return null
}

export function validateEmail(email) {
  const trimmed = email.trim()
  if (!trimmed) return 'Please enter your email address.'
  if (trimmed.length > EMAIL_MAX_LENGTH || !EMAIL_PATTERN.test(trimmed)) {
    return 'Please enter a valid email address.'
  }
  return null
}

export function passwordChecks(password) {
  const bytes = new TextEncoder().encode(password).length
  return [
    { label: '8 to 64 characters', ok: password.length >= 8 && password.length <= 64 },
    { label: 'at least one uppercase letter', ok: /\p{Lu}/u.test(password) },
    { label: 'at least one number', ok: /[0-9]/.test(password) },
    { label: 'at least one special character', ok: /[^\p{L}\p{N}\s]/u.test(password) },
    { label: 'not too long (umlauts and emojis count as several bytes)', ok: bytes <= 72 },
  ]
}

export function validatePassword(password) {
  if (!password) return 'Please enter a password.'
  return passwordChecks(password).every((check) => check.ok)
    ? null
    : 'Your password does not meet all requirements yet.'
}

export function validatePasswordConfirm(password, confirm) {
  return password === confirm ? null : 'The passwords do not match.'
}

// Alle Fehler der Registrierung als Objekt, z. B. { email: '...' }; leer = alles ok
export function registrationErrors(form) {
  const errors = {}
  const add = (field, message) => {
    if (message) errors[field] = message
  }
  add('name', validateName(form.name))
  add('email', validateEmail(form.email))
  add('password', validatePassword(form.password))
  add('confirm', validatePasswordConfirm(form.password, form.confirm))
  return errors
}

const WEEK_DAYS = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'];

export function profileErrors(form) {
  const e = {};
  if (!Number.isInteger(+form.age) || +form.age < 16 || +form.age > 99)
    e.age = 'Age: whole number from 16 to 99.';
  if (!['male', 'female', 'diverse'].includes(form.gender)) e.gender = 'Please select a gender.';
  if (!Number.isInteger(+form.height) || +form.height < 120 || +form.height > 230)
    e.height = 'Height: whole number from 120 to 230 cm.';
  const weight = String(form.weight).replace(',', '.');
  if (!/^\d+(\.\d)?$/.test(weight) || +weight < 30 || +weight > 250)
    e.weight = 'Weight: 30 to 250 kg, at most one decimal place.';
  if (!['muscle_gain', 'fat_loss', 'health'].includes(form.goal)) e.goal = 'Please select a goal.';
  if (!['beginner', 'advanced'].includes(form.fitnessLevel))
    e.fitnessLevel = 'Please select a fitness level.';
  const selected = form.trainingDays;
  if (selected.length !== 3 || new Set(selected).size !== 3
      || selected.some(d => !WEEK_DAYS.includes(d))) {
    e.trainingDays = 'Please select exactly three days.';
  } else if (selected.some(d => selected.includes(WEEK_DAYS[(WEEK_DAYS.indexOf(d) + 1) % 7]))) {
    e.trainingDays = 'Leave at least one rest day, also between Sunday and Monday.';
  }
  return e;
}

export function profileBody(form) {
  return {
    personal: {
      age: +form.age, gender: form.gender, height: +form.height,
      weight: +String(form.weight).replace(',', '.'),
    },
    training: {
      goal: form.goal, fitnessLevel: form.fitnessLevel, trainingDays: form.trainingDays,
    },
  };
}