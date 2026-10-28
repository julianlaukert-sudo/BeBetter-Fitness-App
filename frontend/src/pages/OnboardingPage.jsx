import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { profileErrors, profileBody } from '../utils/validation.js';
import { loadDraft, saveDraft, clearDraft } from '../utils/onboardingDraft.js';
import FormErrors from '../components/FormErrors.jsx';

const DAYS = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'];
const DAY_LABELS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
const GENDERS = [['male', 'Male'], ['female', 'Female'], ['diverse', 'Diverse']];
const GOALS = [['muscle_gain', 'Muscle gain'], ['fat_loss', 'Fat loss'], ['health', 'Health']];
const LEVELS = [['beginner', 'Beginner'], ['advanced', 'Advanced']];

const FIELD_LABELS = {
    age: 'Age',
    gender: 'Gender',
    height: 'Height',
    weight: 'Weight',
    goal: 'Goal',
    fitnessLevel: 'Fitness level',
    trainingDays: 'Training days',
};

const text = (list, value) =>
    (list.find(([v]) => v === value) || [value, value])[1];

export default function OnboardingPage() {
    const { user, authorized, markProfileCompleted } = useAuth();
    const navigate = useNavigate();

    const [form, setForm] = useState(() => loadDraft(user.userId));
    const [step, setStep] = useState(1);
    const [fields, setFields] = useState({});
    const [message, setMessage] = useState('');
    const [warning, setWarning] = useState('');
    const [saved, setSaved] = useState(null);
    const [busy, setBusy] = useState(true);
    const [ready, setReady] = useState(false);

    // Vorhandenes Profil aus der Datenbank laden.
    useEffect(() => {
        let active = true;

        authorized('/profile')
            .then(data => {
                if (active) {
                    setSaved(data);
                    setReady(true);
                }
            })
            .catch(e => {
                if (!active) return;

                if (e.message === 'Profile not found.') {
                    setReady(true);
                } else {
                    setMessage(e.message);
                }
            })
            .finally(() => {
                if (active) setBusy(false);
            });

        return () => {
            active = false;
        };
    }, [user.userId]);

    function change(name, value) {
        const next = { ...form, [name]: value };
        setForm(next);

        if (!saveDraft(user.userId, next)) {
            setWarning(
                'Draft could not be saved in this browser. Please keep this page open.'
            );
        }
    }

    function next() {
        const errors = profileErrors(form);

        const names =
            step === 1
                ? ['age', 'gender', 'height', 'weight']
                : ['goal', 'fitnessLevel', 'trainingDays'];

        const current = Object.fromEntries(
            Object.entries(errors).filter(([k]) => names.includes(k))
        );

        setFields(current);

        if (!Object.keys(current).length) {
            setStep(step + 1);
        }
    }

    async function submit(event) {
        event.preventDefault();

        if (step !== 3) {
            next();
            return;
        }

        const errors = profileErrors(form);
        setFields(errors);
        setMessage('');

        if (Object.keys(errors).length) return;

        setBusy(true);

        try {
            const data = await authorized('/profile', {
                method: 'POST',
                body: profileBody(form),
            });

            // Den Entwurf erst nach erfolgreichem Speichern löschen.
            clearDraft(user.userId);

            // Den Profilstatus im AuthContext aktualisieren.
            markProfileCompleted();

            // Die gespeicherten Profildaten anzeigen.
            setSaved(data);

            // Nach erfolgreichem Speichern zur Trainingsseite wechseln.
            // Dort wird der Trainingsplan erstellt.
            navigate('/training');
        } catch (e) {
            // Bei einem Fehler bleiben die Eingaben erhalten.
            setMessage(e.message);
            setFields(e.fields || {});
        } finally {
            setBusy(false);
        }
    }

    function select(name, label, values) {
        return (
            <label>
                {label}
                <select
                    value={form[name]}
                    onChange={e => change(name, e.target.value)}
                >
                    <option value="">Please select</option>
                    {values.map(([value, t]) => (
                        <option key={value} value={value}>
                            {t}
                        </option>
                    ))}
                </select>
            </label>
        );
    }

    if (saved) {
        return (
            <section className="card">
                <h2>Profile saved</h2>

                <p>
                    {saved.personal.age} years, {text(GENDERS, saved.personal.gender)},{' '}
                    {saved.personal.height} cm, {saved.personal.weight} kg
                </p>

                <p>
                    {text(GOALS, saved.training.goal)},{' '}
                    {text(LEVELS, saved.training.fitnessLevel)},{' '}
                    {saved.training.trainingDays.join(', ')}
                </p>

                {/*Link zur Trainingsseite */}
                <Link to="/training">Go to training plan</Link>
                <p><Link to="/profile">Edit profile</Link></p>

                <p>Editing the profile follows in Sprint 5.</p>
            </section>
        );
    }

    return (
        <section className="card">
            <h2>Personal profile – step {step} of 3</h2>

            <FormErrors
                message={message}
                fields={fields}
                labels={FIELD_LABELS}
            />

            {warning && <p role="alert">{warning}</p>}

            {!ready && !busy && (
                <p>
                    Profile could not be loaded.{' '}
                    <button
                        type="button"
                        onClick={() => window.location.reload()}
                    >
                        Reload
                    </button>
                </p>
            )}

            <form onSubmit={submit} noValidate>
                <fieldset disabled={busy || !ready}>
                    {step === 1 && (
                        <>
                            {['age', 'height', 'weight'].map(name => (
                                <label key={name}>
                                    {{
                                        age: 'Age (years)',
                                        height: 'Height (cm)',
                                        weight: 'Weight (kg)',
                                    }[name]}

                                    <input
                                        inputMode={name === 'weight' ? 'decimal' : 'numeric'}
                                        value={form[name]}
                                        onChange={e => change(name, e.target.value)}
                                    />
                                </label>
                            ))}

                            {select('gender', 'Gender', GENDERS)}
                        </>
                    )}

                    {step === 2 && (
                        <>
                            {select('goal', 'Goal', GOALS)}
                            {select('fitnessLevel', 'Fitness level', LEVELS)}

                            <p>
                                Select three training days with at least one rest day in between.
                            </p>

                            <div className="day-options">
                                {DAYS.map((day, i) => (
                                    <label key={day}>
                                        <input
                                            type="checkbox"
                                            checked={form.trainingDays.includes(day)}
                                            onChange={e =>
                                                change(
                                                    'trainingDays',
                                                    e.target.checked
                                                        ? [...form.trainingDays, day]
                                                        : form.trainingDays.filter(d => d !== day)
                                                )
                                            }
                                        />
                                        {DAY_LABELS[i]}
                                    </label>
                                ))}
                            </div>
                        </>
                    )}

                    {step === 3 && (
                        <>
                            <p>Please check your data:</p>

                            <p>
                                {form.age} years, {form.height} cm, {form.weight} kg,{' '}
                                {text(GENDERS, form.gender)}
                            </p>

                            <p>
                                {text(GOALS, form.goal)},{' '}
                                {text(LEVELS, form.fitnessLevel)},{' '}
                                {form.trainingDays.join(', ')}
                            </p>
                        </>
                    )}

                    {step > 1 && (
                        <button
                            type="button"
                            onClick={() => {
                                setFields({});
                                setStep(step - 1);
                            }}
                        >
                            Back
                        </button>
                    )}

                    {step < 3 ? (
                        <button type="button" onClick={next}>
                            Next
                        </button>
                    ) : (
                        <button type="submit">
                            {busy ? 'Saving...' : 'Save profile'}
                        </button>
                    )}
                </fieldset>
            </form>

            {message === 'Profile already exists.' && (
                <button
                    type="button"
                    onClick={() => window.location.reload()}
                >
                    Load saved profile
                </button>
            )}
        </section>
    );
}