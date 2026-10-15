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