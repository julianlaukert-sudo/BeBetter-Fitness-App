import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import FormErrors from '../components/FormErrors.jsx'
import { useAuth } from '../context/AuthContext.jsx'
import { validateEmail } from '../utils/validation.js'
import { useSearchParams } from 'react-router-dom';
import { openAdmin } from '../api/adminSso.js';

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
        <section className="card">
            <h2>Log in</h2>
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
                    <label>
                        Email
                        <input
                            type="email"
                            value={form.email}
                            onChange={update('email')}
                            autoComplete="email"
                            aria-invalid={Boolean(fields.email)}
                        />
                    </label>

                    <label>
                        Password
                        <input
                            type="password"
                            value={form.password}
                            onChange={update('password')}
                            autoComplete="current-password"
                            aria-invalid={Boolean(fields.password)}
                        />
                    </label>

                    <button type="submit">
                        {busy ? 'Logging in …' : 'Log in'}
                    </button>
                </fieldset>
            </form>

            <p>
                No account yet? <Link to="/register">Sign up</Link>
            </p>
        </section>
    )
}