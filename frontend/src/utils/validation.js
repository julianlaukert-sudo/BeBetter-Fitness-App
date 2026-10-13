const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export function validateEmail(email) {
  if (!email.trim()) return 'Bitte gib deine E-Mail-Adresse ein.'
  if (!EMAIL_PATTERN.test(email.trim())) return 'Bitte gib eine gültige E-Mail-Adresse ein.'
  return null
}

export function passwordChecks(password) {
  const bytes = new TextEncoder().encode(password).length
  return [
    { label: '8 bis 64 Zeichen', ok: password.length >= 8 && password.length <= 64 },
    { label: 'mindestens ein Großbuchstabe', ok: /\p{Lu}/u.test(password) },
    { label: 'mindestens eine Zahl', ok: /\d/.test(password) },
    { label: 'mindestens ein Sonderzeichen', ok: /[^\p{L}\p{N}\s]/u.test(password) },
    { label: 'nicht zu lang (Umlaute und Emojis zählen mehrfach)', ok: bytes <= 72 },
  ]
}

export function validatePassword(password) {
  return passwordChecks(password).every((check) => check.ok)
    ? null
    : 'Das Passwort erfüllt noch nicht alle Regeln.'
}

export function validatePasswordConfirm(password, confirm) {
  return password === confirm ? null : 'Die Passwörter stimmen nicht überein.'
}