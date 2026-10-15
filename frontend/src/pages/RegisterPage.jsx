import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { registerUser } from '../api/auth.js'
import {
  NAME_MAX_LENGTH,
  passwordChecks,
  validateEmail,
  validateName,
  validatePassword,
  validatePasswordConfirm,
} from '../utils/validation.js'

export default function RegisterPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ name: '', email: '', password: '', confirm: '' })
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
      name: validateName(form.name),
      email: validateEmail(form.email),
      password: validatePassword(form.password),
      confirm: validatePasswordConfirm(form.password, form.confirm),
    }
    setErrors(newErrors)
    if (Object.values(newErrors).some(Boolean)) return

    setSubmitting(true)
    try {
      await registerUser({
        name: form.name.trim(),
        email: form.email.trim(),
        password: form.password,
      })
      navigate('/login', { state: { registered: true } })
    } catch (error) {
      setServerError(
        error.status === 409
          ? 'An account with this email address already exists.'
          : error.message,
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section>
      <h2>Sign up</h2>
      <form onSubmit={handleSubmit} noValidate>
        <label>
          Name
          <input
            type="text"
            value={form.name}
            onChange={update('name')}
            maxLength={NAME_MAX_LENGTH}
            autoComplete="name"
          />
        </label>
        {errors.name && <p className="error">{errors.name}</p>}

        <label>
          Email
          <input type="email" value={form.email} onChange={update('email')} autoComplete="email" />
        </label>
        {errors.email && <p className="error">{errors.email}</p>}

        <label>
          Password
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
          Confirm password
          <input type="password" value={form.confirm} onChange={update('confirm')} autoComplete="new-password" />
        </label>
        {errors.confirm && <p className="error">{errors.confirm}</p>}

        {serverError && <p role="alert" className="error">{serverError}</p>}

        <button type="submit" disabled={submitting}>
          {submitting ? 'Sending …' : 'Create account'}
        </button>
      </form>
    </section>
  )
}