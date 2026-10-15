import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { validateEmail } from '../utils/validation.js'

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const justRegistered = location.state?.registered === true

  const [form, setForm] = useState({ email: '', password: '' })
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
      password: form.password ? null : 'Please enter your password.',
    }
    setErrors(newErrors)
    if (Object.values(newErrors).some(Boolean)) return

    setSubmitting(true)
    try {
      await login(form.email.trim(), form.password)
      navigate('/', { replace: true })
    } catch (error) {
      setServerError(
        error.status === 401 ? 'Email or password is incorrect.' : error.message,
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section>
      <h2>Log in</h2>

      {justRegistered && (
        <p style={{ color: 'green' }}>Your account has been created. Please log in.</p>
      )}

      <form onSubmit={handleSubmit} noValidate>
        <label>
          Email
          <input type="email" value={form.email} onChange={update('email')} autoComplete="email" />
        </label>
        {errors.email && <p className="error">{errors.email}</p>}

        <label>
          Password
          <input
            type="password"
            value={form.password}
            onChange={update('password')}
            autoComplete="current-password"
          />
        </label>
        {errors.password && <p className="error">{errors.password}</p>}

        {serverError && <p role="alert" className="error">{serverError}</p>}

        <button type="submit" disabled={submitting}>
          {submitting ? 'Logging in …' : 'Log in'}
        </button>
      </form>

      <p>
        No account yet? <Link to="/register">Sign up</Link>
      </p>
    </section>
  )
}