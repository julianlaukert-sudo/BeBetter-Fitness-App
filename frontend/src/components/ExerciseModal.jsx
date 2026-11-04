import { useEffect, useRef, useState } from 'react';
import { Dumbbell, Target, User, X } from 'lucide-react';

// KI-Log Nr. 27
// Natives dialog: Schließen per Button oder Escape
export default function ExerciseModal({ exercise, close }) {
  const dialog = useRef(null);
  const [broken, setBroken] = useState(false);

  useEffect(() => {
    const el = dialog.current;
    const opener = document.activeElement; // Hierhin kehrt der Fokus beim Schließen zurück
    if (!el.open) el.showModal();
    return () => {
      if (el.open) el.close();
      if (opener instanceof HTMLElement && opener.isConnected) opener.focus();
    };
  }, []);

  const hasImage = exercise.imageUrl && !broken;

  return (
    <dialog ref={dialog} className="exercise-modal" onCancel={close} aria-labelledby="exercise-title">
      <div className="modal-head">
        <h2 id="exercise-title">{exercise.name || 'Exercise details not available'}</h2>
        <button type="button" className="icon-button" onClick={close} aria-label="Close">
          <X size={20} aria-hidden="true" />
        </button>
      </div>
      <ul className="modal-facts">
        <li><User size={20} aria-hidden="true" /><span>Body part:</span> {exercise.bodyPart}</li>
        <li><Target size={20} aria-hidden="true" /><span>Target muscle:</span> {exercise.targetMuscle || <span>Not available</span>}</li>
        <li><Dumbbell size={20} aria-hidden="true" className="icon-dumbbell" /><span>Equipment:</span> {exercise.equipment || <span>Not available</span>}</li>
      </ul>
      <div className={`modal-media${hasImage ? '' : ' is-empty'}`}>
        {hasImage
          ? <img src={exercise.imageUrl} alt={exercise.name || 'Exercise'}
              onError={() => setBroken(true)} />
          : <p>No image available.</p>}
      </div>
      <div className="modal-foot">
        <button type="button" className="btn-primary" onClick={close}>Close</button>
      </div>
    </dialog>
  );
}