import { useState } from 'react';
import { useAuth } from '../context/AuthContext.jsx';
import { usePlan } from '../context/PlanContext.jsx';
import { saveMark } from '../api/marks.js';
import { markPayload } from '../utils/markValidation.js';
// KI-Log Nr. 16
export default function MarkForm({ sessionId, exercise, available }) {
  const { authorized } = useAuth();
  const { refresh } = usePlan();
  const mark = exercise.mark;
  const [values, setValues] = useState({
    done: mark ? String(mark.done) : '',
    weight: mark?.weight ?? exercise.weight ?? '',
    reps: mark?.reps ?? '',
  });
  const [pending, setPending] = useState(false);
  const [message, setMessage] = useState('');

  const field = e => setValues(v => ({ ...v, [e.target.name]: e.target.value }));

  async function submit(e) {
    e.preventDefault();
    if (pending || !available) return;
    setPending(true);
    setMessage('');
    let saved = false;
    try {
      const payload = markPayload(sessionId, exercise, values);
      await saveMark(authorized, payload);
      saved = true;
      setMessage('Saved.');
      await refresh(true);
    } catch (error) {
      // Speichern ok, nur Neuladen fehlgeschlagen -> klar unterscheiden
      setMessage(saved
        ? 'Saved, but the view could not be refreshed. Please reload.'
        : error.message);
    } finally {
      setPending(false);
    }
  }

  return (
    <form onSubmit={submit} noValidate>
      <fieldset disabled={pending || !available}>
        <legend>Your training values</legend>
        <label>
          Result
          <select name="done" value={values.done} onChange={field}>
            <option value="">Please select</option>
            <option value="true">Done</option>
            <option value="false">Not done</option>
          </select>
        </label>
        {!exercise.bodyweight && (
          <label>
            Actual weight in kg
            <input name="weight" inputMode="decimal" value={values.weight} onChange={field} />
          </label>
        )}
        <label>
          Actual reps
          <input name="reps" inputMode="numeric" value={values.reps} onChange={field} />
        </label>
        <button type="submit">{pending ? 'Saving...' : mark ? 'Correct' : 'Save'}</button>
      </fieldset>
      {message && <p role="status">{message}</p>}
    </form>
  );
}