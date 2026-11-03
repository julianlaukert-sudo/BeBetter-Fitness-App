import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { profileErrors, profileBody } from '../utils/validation.js';
import { loadDraft, saveDraft, clearDraft } from '../utils/onboardingDraft.js';
import { Check, Dumbbell, Flame, HeartPulse } from 'lucide-react';
import FormErrors from '../components/FormErrors.jsx';

const DAYS = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'];
const DAY_LABELS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
const GENDERS = [['male', 'Male'], ['female', 'Female'], ['diverse', 'Diverse']];
const GOALS = [['muscle_gain', 'Muscle gain'], ['fat_loss', 'Fat loss'], ['health', 'Health']];
const LEVELS = [['beginner', 'Beginner'], ['advanced', 'Advanced']];
const GOAL_ICONS = { muscle_gain: Dumbbell, fat_loss: Flame, health: HeartPulse };
const STEPS = ['Personal details', 'Training', 'Review'];

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

// Nur für die Anzeige: Tage in Wochenreihenfolge und ausgeschrieben
const dayText = days =>
    DAYS.filter(d => days.includes(d)).map(d => DAY_LABELS[DAYS.indexOf(d)]).join(', ');

// Übersicht der Profildaten (Schritt 3 und nach dem Speichern)
function Summary({ personal, training }) {
    const rows = [
        ['Age', `${personal.age} years`],
        ['Height', `${personal.height} cm`],
        ['Weight', `${personal.weight} kg`],
        ['Gender', text(GENDERS, personal.gender)],
        ['Goal', text(GOALS, training.goal)],
        ['Fitness level', text(LEVELS, training.fitnessLevel)],
        ['Training days', dayText(training.trainingDays)],
    ];
    return (
        <dl className="summary">
            {rows.map(([term, value]) => (
                <div key={term}>
                    <dt>{term}</dt>
                    <dd>{value}</dd>
                </div>
            ))}
        </dl>
    );
}

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

    // Auswahlkarten (Radio-Buttons): gleiche Werte wie die bisherigen Auswahllisten
    function choices(name, label, values, icons = {}) {
        return (
            <fieldset className="choice-group">
                <legend>{label}</legend>
                <div className="choice-grid">
                    {values.map(([value, t]) => {
                        const Icon = icons[value];
                        return (
                            <label key={value} className="choice">
                                <input
                                    type="radio"
                                    name={name}
                                    value={value}
                                    checked={form[name] === value}
                                    onChange={() => change(name, value)}
                                />
                                {Icon && (
                                    <Icon
                                        size={22}
                                        aria-hidden="true"
                                        className={Icon === Dumbbell ? 'icon-dumbbell' : undefined}
                                    />
                                )}
                                <span>{t}</span>
                            </label>
                        );
                    })}
                </div>
            </fieldset>
        );
    }

    if (saved) {
        return (
            <section className="card onboarding">
                <h2>Profile saved</h2>

                <Summary personal={saved.personal} training={saved.training} />

                {/*Link zur Trainingsseite */}
                <div className="form-actions">
                    <Link className="link-button" to="/profile">Edit profile</Link>
                    <Link className="link-button btn-primary" to="/training">Go to training plan</Link>
                </div>
            </section>
        );
    }

    return (
        <section className="card onboarding">
            <header className="onboarding-header">
                <h2>Personal profile</h2>
                <ol className="steps" aria-label={`Step ${step} of 3`}>
                    {STEPS.map((label, i) => {
                        const number = i + 1;
                        return (
                            <li
                                key={label}
                                className={number < step ? 'done' : undefined}
                                aria-current={number === step ? 'step' : undefined}
                            >
                                <span className="step-number">
                                    {number < step ? <Check size={14} aria-hidden="true" /> : number}
                                </span>
                                <span className="step-label">{label}</span>
                            </li>
                        );
                    })}
                </ol>
            </header>

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
                        <div className="form-grid">
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
                        </div>
                    )}

                    {step === 2 && (
                        <>
                            {choices('goal', 'Goal', GOALS, GOAL_ICONS)}
                            {choices('fitnessLevel', 'Fitness level', LEVELS)}

                            <fieldset className="choice-group">
                                <legend>Training days</legend>
                                <p className="hint">
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
                            </fieldset>
                        </>
                    )}

                    {step === 3 && (
                        <>
                            <p>Please check your data:</p>
                            <Summary
                                personal={form}
                                training={form}
                            />
                        </>
                    )}

                    <div className="form-actions">
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

                        {/* key: getrennte Buttons, damit der Klick auf "Next" nie als "Save profile" gilt */}
                        {step < 3 ? (
                            <button key="next" type="button" className="btn-primary" onClick={next}>
                                Next
                            </button>
                        ) : (
                            <button key="save" type="submit" className="btn-primary">
                                {busy ? 'Saving...' : 'Save profile'}
                            </button>
                        )}
                    </div>
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