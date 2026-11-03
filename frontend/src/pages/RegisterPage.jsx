import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { registerUser } from '../api/auth.js'
import { Lock, Mail, User } from 'lucide-react'
import AuthField from '../components/AuthField.jsx'
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
    <section className="card auth-card">
      <header className="auth-card-header">
        <h2>Sign up</h2>
        <p>Create your account and get started.</p>
      </header>
      <FormErrors message={message} fields={fields} />
      <form onSubmit={handleSubmit} noValidate>
        <fieldset disabled={busy}>
          <AuthField label="Name" icon={User} type="text" value={form.name} onChange={update('name')}
            maxLength={NAME_MAX_LENGTH} autoComplete="name" placeholder="Your name"
            aria-invalid={Boolean(fields.name)} />
          <AuthField label="Email" icon={Mail} type="email" value={form.email} onChange={update('email')}
            autoComplete="email" placeholder="you@example.com" aria-invalid={Boolean(fields.email)} />
          <AuthField label="Password" icon={Lock} type="password" reveal value={form.password}
            onChange={update('password')} autoComplete="new-password" placeholder="8 to 64 characters"
            aria-invalid={Boolean(fields.password)} />
          <ul className="password-checks">
            {passwordChecks(form.password).map((check) => (
              <li key={check.label} className={check.ok ? 'ok' : ''}>
                {check.ok ? '✓' : '○'} {check.label}
              </li>
            ))}
          </ul>
          <AuthField label="Confirm password" icon={Lock} type="password" reveal value={form.confirm}
            onChange={update('confirm')} autoComplete="new-password" placeholder="Repeat your password"
            aria-invalid={Boolean(fields.confirm)} />
          <button type="submit" className="btn-primary auth-submit">{busy ? 'Sending …' : 'Create account'}</button>
        </fieldset>
      </form>
      <p className="auth-switch">Already have an account? <Link to="/login">Log in</Link></p>
    </section>
  )
}