import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { loadProfile, updateProfile } from '../api/profile.js';
import { profileErrors, profileBody } from '../utils/validation.js';
import { profileForm } from '../utils/profileForm.js';
import ProfileFields, { FIELD_LABELS } from '../components/ProfileFields.jsx';
import FormErrors from '../components/FormErrors.jsx';
// KI-Log Nr. 17
export default function ProfilePage() {
  const { user, authorized } = useAuth();
  const [saved, setSaved] = useState(null);
  const [form, setForm] = useState(null);
  const [busy, setBusy] = useState(true);
  const [message, setMessage] = useState('');
  const [fields, setFields] = useState({});
  const [confirmed, setConfirmed] = useState(false);
  const [missing, setMissing] = useState(false);

  useEffect(() => {
    let active = true;
    loadProfile(authorized)
      .then(data => { if (active) { setSaved(data); setForm(profileForm(data)); } })
      .catch(e => {
        if (!active) return;
        setMessage(e.message);
        setMissing(e.message === 'Profile not found.');
      })
      .finally(() => { if (active) setBusy(false); });
    return () => { active = false; };
  }, [user.userId]);

  function change(name, value) {
    setForm(f => ({ ...f, [name]: value }));
    setConfirmed(false);   // nach jeder Änderung neu bestätigen
  }

  async function submit(e) {
    e.preventDefault();
    setMessage('');
    const errors = profileErrors(form);
    setFields(errors);
    if (Object.keys(errors).length) return;
    if (!confirmed) {
      setMessage('Please confirm that the changes apply from next week.');
      return;
    }
    setBusy(true);
    try {
      const data = await updateProfile(authorized, profileBody(form));
      setSaved(data);
      setForm(profileForm(data));
      setConfirmed(false);
      setMessage(data.nextPlanFrom
        ? `Saved. Your new plan starts on ${data.nextPlanFrom}.`
        : 'Profile saved.');
    } catch (error) {
      // Eingaben bleiben stehen
      setMessage(error.message);
      setFields(error.fields || {});
    } finally {
      setBusy(false);
    }
  }

  function discard() {
    setForm(profileForm(saved));
    setConfirmed(false);
    setFields({});
    setMessage('');
  }

  return (
    <section className="card">
      <h2>Edit my profile</h2>
      <FormErrors message={message} fields={fields} labels={FIELD_LABELS} />
      {busy && <p role="status">Please wait...</p>}
      {missing && <Link to="/onboarding">Create your profile first</Link>}
      {!form && !busy && !missing && (
        <button type="button" onClick={() => window.location.reload()}>Reload</button>
      )}
      {form && (
        <form onSubmit={submit} noValidate>
          <fieldset disabled={busy}>
            <ProfileFields form={form} change={change} />
            <p>Profile values are saved right away. Goal, level, training days and body data
              relevant for start weights only affect your plan from next week.
              The current week and its results stay unchanged.</p>
            {saved?.nextPlanFrom && <p>New plan scheduled from: {saved.nextPlanFrom}.</p>}
            <label>
              <input type="checkbox" checked={confirmed}
                onChange={e => setConfirmed(e.target.checked)} />
              I understand when the changes take effect.
            </label>
            <button type="submit">Save changes</button>{' '}
            <button type="button" onClick={discard}>Discard changes</button>
          </fieldset>
        </form>
      )}
    </section>
  );
}