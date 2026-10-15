import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { registerUser } from '../api/auth.js'
import FormErrors from '../components/FormErrors.jsx'
import { NAME_MAX_LENGTH, passwordChecks, registrationErrors } from '../utils/validation.js'

export default function RegisterPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ name: '', email: '', password: '', confirm: '' })
  const [fields, setFields] = useState({})
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)

  function update(field) {
    return (event) => setForm({ ...form, [field]: event.target.value })
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setMessage('')
    const errors = registrationErrors(form)
    setFields(errors)
    if (Object.keys(errors).length > 0) return

    setBusy(true)
    try {
      await registerUser({ name: form.name.trim(), email: form.email.trim(), password: form.password })
      navigate('/login', { state: { registered: true } })
    } catch (error) {
      setMessage(error.message)
      setFields(error.fields ?? {})
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="card">
      <h2>Sign up</h2>
      <FormErrors message={message} fields={fields} />
      <form onSubmit={handleSubmit} noValidate>
        <fieldset disabled={busy}>
          <label>
            Name
            <input type="text" value={form.name} onChange={update('name')} maxLength={NAME_MAX_LENGTH}
              autoComplete="name" aria-invalid={Boolean(fields.name)} />
          </label>
          <label>
            Email
            <input type="email" value={form.email} onChange={update('email')} autoComplete="email"
              aria-invalid={Boolean(fields.email)} />
          </label>
          <label>
            Password
            <input type="password" value={form.password} onChange={update('password')}
              autoComplete="new-password" aria-invalid={Boolean(fields.password)} />
          </label>
          <ul className="password-checks">
            {passwordChecks(form.password).map((check) => (
              <li key={check.label} className={check.ok ? 'ok' : ''}>
                {check.ok ? '✓' : '○'} {check.label}
              </li>
            ))}
          </ul>
          <label>
            Confirm password
            <input type="password" value={form.confirm} onChange={update('confirm')}
              autoComplete="new-password" aria-invalid={Boolean(fields.confirm)} />
          </label>
          <button type="submit">{busy ? 'Sending …' : 'Create account'}</button>
        </fieldset>
      </form>
      <p>Already have an account? <Link to="/login">Log in</Link></p>
    </section>
  )
}