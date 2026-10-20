import { Link } from 'react-router-dom';

const TEXTS = {
  'Exercises could not be loaded.':
    'Exercises are currently unavailable. Your profile is saved. Please try again later.',
  'Profile not found.': 'Please complete your profile first.',
  'Week change follows in Sprint 4.':
    'This plan belongs to last week. The week change follows in Sprint 4.',
};

export default function ApiDownBanner({ missing, error, busy, retry }) {
  if (!missing && !error) return null;
  const text = TEXTS[error] || error
    || 'Some exercise details are missing. Your training instructions stay visible.';
  return (
    <aside role="alert" className="form-errors">
      <p>{text}</p>
      <button type="button" disabled={busy} onClick={retry}>Try again</button>
      {error === 'Profile not found.' && <> {' '}<Link to="/onboarding">Go to profile</Link></>}
    </aside>
  );
}