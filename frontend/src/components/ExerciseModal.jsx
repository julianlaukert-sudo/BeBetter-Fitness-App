import { useEffect, useRef, useState } from 'react';
// KI-Log Nr. 13
// Natives dialog: Schließen per Button oder Escape
export default function ExerciseModal({ exercise, close }) {
  const dialog = useRef(null);
  const [broken, setBroken] = useState(false);

  useEffect(() => {
    const el = dialog.current;
    if (!el.open) el.showModal();
    return () => { if (el.open) el.close(); };
  }, []);

  return (
    <dialog ref={dialog} onCancel={close} aria-labelledby="exercise-title">
      <h2 id="exercise-title">{exercise.name || 'Exercise details not available'}</h2>
      <p>Body part: {exercise.bodyPart}</p>
      <p>Target muscle: {exercise.targetMuscle || 'Not available'}</p>
      <p>Equipment: {exercise.equipment || 'Not available'}</p>
      {exercise.imageUrl && !broken
        ? <img src={exercise.imageUrl} alt={exercise.name || 'Exercise'}
            onError={() => setBroken(true)} width="260" />
        : <p>No image available.</p>}
      <button type="button" onClick={close}>Close</button>
    </dialog>
  );
}