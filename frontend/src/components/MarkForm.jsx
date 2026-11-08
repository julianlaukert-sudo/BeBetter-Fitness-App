import { useState } from 'react';
import { Check } from 'lucide-react';
import { useAuth } from '../context/AuthContext.jsx';
import { usePlan } from '../context/PlanContext.jsx';
import { saveMark } from '../api/marks.js';
import { markPayload } from '../utils/markValidation.js';

// KI-Log Nr. 27, 32: Ein Schalter je Übung. Häkchen = geschafft, kein Häkchen = nicht geschafft.
export default function MarkForm({ sessionId, exercise, available }) {
    const { authorized } = useAuth();
    const { refresh } = usePlan();
    const done = exercise.mark?.done === true;
    const [pending, setPending] = useState(false);
    const [message, setMessage] = useState('');

    async function toggle() {
        if (pending || !available) return;
        setPending(true);
        setMessage('');
        let saved = false;
        try {
            await saveMark(authorized, markPayload(sessionId, exercise, !done));
            saved = true;
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
        <div className="mark-form">
            <button
                type="button"
                className={`mark-toggle${done ? ' is-done' : ''}`}
                aria-pressed={done}
                disabled={pending || !available}
                onClick={toggle}
            >
        <span className="mark-check" aria-hidden="true">
          {done && <Check size={16} strokeWidth={3} />}
        </span>
                {pending ? 'Saving...' : 'Done'}
            </button>
            {message && <p role="status" className="mark-message">{message}</p>}
        </div>
    );
}