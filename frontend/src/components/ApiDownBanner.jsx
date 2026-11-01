import { Link } from 'react-router-dom';

const TEXTS = {
  'Exercises could not be loaded.':
    'Exercises are currently unavailable. Your profile is saved. Please try again later.',
  'Profile not found.': 'Please complete your profile first.',
  'Plan week is not current.': 'The plan could not be updated yet. Please try again.',
};

export default function ApiDownBanner({ missing, error, busy, retry }) {
  if (!missing && !error) return null;
  const text = TEXTS[error] || error
    || 'Some exercise details are missing. Your training instructions stay visible.';
  // Sprint 7: Hilfe bei Störungen der Übungsquelle – ohne Wartezeit-Versprechen
  const exerciseProblem = missing || error === 'Exercises could not be loaded.';
  return (
    <aside role="alert" className="form-errors">
      <p>{text}</p>
      {exerciseProblem && (
        <p>Please do not reload several times in a row. If the exercise provider is
          temporarily blocking requests, the waiting time can be longer than a minute.
          Try again once after that.</p>
      )}
      <button type="button" disabled={busy} onClick={retry}>Try again</button>
      {error === 'Profile not found.' && <> {' '}<Link to="/onboarding">Go to profile</Link></>}
    </aside>
  );
}