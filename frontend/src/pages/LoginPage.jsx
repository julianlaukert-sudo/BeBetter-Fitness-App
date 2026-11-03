import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import FormErrors from '../components/FormErrors.jsx'
import { useAuth } from '../context/AuthContext.jsx'
import { validateEmail } from '../utils/validation.js'
import { useSearchParams } from 'react-router-dom';
import { openAdmin } from '../api/adminSso.js';
import { Lock, Mail } from 'lucide-react';
import AuthField from '../components/AuthField.jsx';

export default function LoginPage() {
    const { login } = useAuth()
    const navigate = useNavigate()
    const location = useLocation()
    const justRegistered = location.state?.registered === true

    const [form, setForm] = useState({ email: '', password: '' })
    const [fields, setFields] = useState({})
    const [message, setMessage] = useState('')
    const [busy, setBusy] = useState(false)
    const [params] = useSearchParams();

    function update(field) {
        return (event) =>
            setForm({ ...form, [field]: event.target.value })
    }

    async function handleSubmit(event) {
        event.preventDefault()
        setMessage('')

        const errors = {}
        const emailError = validateEmail(form.email)

        if (emailError) errors.email = emailError

        if (!form.password) {
            errors.password = 'Please enter your password.'
        }

        setFields(errors)

        if (Object.keys(errors).length > 0) return

        setBusy(true)

        try {
            const session = await login(form.email, form.password);

            if (session.role === 'ADMIN') {
                openAdmin(session.token);
                return;
            }

            navigate(session.profileCompleted ? '/home' : '/onboarding');
        } catch (error) {
            setMessage(error.message);
        } finally {
            setBusy(false);
        }
    }

    return (
        <section className="card auth-card">
            <header className="auth-card-header">
                <h2>Log in</h2>
                <p>Welcome back. Log in to continue with your plan.</p>
            </header>

            {params.get('loggedOut') === '1' && <p role="status">Admin logged out.</p>}
            {params.get('adminError') === '1' && (<p role="alert">Admin login failed. Please log in again.</p>)}

            {justRegistered && (
                <p className="success">
                    Your account has been created. Please log in.
                </p>
            )}

            <FormErrors message={message} fields={fields} />

            <form onSubmit={handleSubmit} noValidate>
                <fieldset disabled={busy}>
                    <AuthField
                        label="Email"
                        icon={Mail}
                        type="email"
                        value={form.email}
                        onChange={update('email')}
                        autoComplete="email"
                        placeholder="you@example.com"
                        aria-invalid={Boolean(fields.email)}
                    />

                    <AuthField
                        label="Password"
                        icon={Lock}
                        type="password"
                        reveal
                        value={form.password}
                        onChange={update('password')}
                        autoComplete="current-password"
                        placeholder="Your password"
                        aria-invalid={Boolean(fields.password)}
                    />

                    <button type="submit" className="btn-primary auth-submit">
                        {busy ? 'Logging in …' : 'Log in'}
                    </button>
                </fieldset>
            </form>

            <p className="auth-switch">
                No account yet? <Link to="/register">Sign up</Link>
            </p>
        </section>
    )
}