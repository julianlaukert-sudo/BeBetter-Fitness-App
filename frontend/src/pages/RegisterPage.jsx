import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { registerUser } from '../api/auth.js'
import {
  passwordChecks,
  validateEmail,
  validatePassword,
  validatePasswordConfirm,
} from '../utils/validation.js'

export default function RegisterPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ email: '', password: '', confirm: '' })
  const [errors, setErrors] = useState({})
  const [serverError, setServerError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  function update(field) {
    return (event) => setForm({ ...form, [field]: event.target.value })
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setServerError(null)

    const newErrors = {
      email: validateEmail(form.email),
      password: validatePassword(form.password),
      confirm: validatePasswordConfirm(form.password, form.confirm),
    }
    setErrors(newErrors)
    if (Object.values(newErrors).some(Boolean)) return

    setSubmitting(true)
    try {
      await registerUser({ email: form.email.trim(), password: form.password })
      navigate('/login', { state: { registered: true } })
    } catch (error) {
      setServerError(
        error.status === 409
          ? 'Für diese E-Mail-Adresse gibt es schon ein Konto.'
          : error.message,
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section>
      <h2>Registrieren</h2>
      <form onSubmit={handleSubmit} noValidate>
        <label>
          E-Mail
          <input type="email" value={form.email} onChange={update('email')} autoComplete="email" />
        </label>
        {errors.email && <p className="error">{errors.email}</p>}

        <label>
          Passwort
          <input type="password" value={form.password} onChange={update('password')} autoComplete="new-password" />
        </label>
        <ul>
          {passwordChecks(form.password).map((check) => (
            <li key={check.label} style={{ color: check.ok ? 'green' : 'gray' }}>
              {check.ok ? '✓' : '○'} {check.label}
            </li>
          ))}
        </ul>
        {errors.password && <p className="error">{errors.password}</p>}

        <label>
          Passwort wiederholen
          <input type="password" value={form.confirm} onChange={update('confirm')} autoComplete="new-password" />
        </label>
        {errors.confirm && <p className="error">{errors.confirm}</p>}

        {serverError && <p role="alert" className="error">{serverError}</p>}

        <button type="submit" disabled={submitting}>
          {submitting ? 'Wird gesendet …' : 'Konto erstellen'}
        </button>
      </form>
    </section>
  )
}