import { useState } from 'react';
import ExerciseModal from './ExerciseModal.jsx';
import MarkForm from './MarkForm.jsx';

function markText(mark) {
  if (!mark) return 'Not marked yet';
  if (mark.automatic) return 'Automatically not done; no measured values.';
  return `${mark.done ? 'Done' : 'Not done'}: ${mark.reps} reps`
    + (mark.weight == null ? '' : `, ${mark.weight} kg`);
}

export default function ExerciseRow({ exercise, sessionId, available }) {
  const [open, setOpen] = useState(false);
  // key: Formular baut sich nach neuem Laden mit den gespeicherten Werten neu auf
  const version = JSON.stringify(exercise.mark);
  return (
    <li>
      <strong>{exercise.name || `Exercise ${exercise.exerciseId}`}</strong>
      <p>{exercise.sets} sets · {exercise.reps} reps</p>
      <p>
        {exercise.bodyweight
          ? 'Bodyweight: no additional weight'
          : `Training target: ${exercise.weight} kg`}
        {!exercise.bodyweight && exercise.equipment === 'dumbbell' && ' per hand'}
      </p>
      <p>{markText(exercise.mark)}</p>
      <button type="button" onClick={() => setOpen(true)}>Exercise details</button>
      {open && <ExerciseModal exercise={exercise} close={() => setOpen(false)} />}
      <MarkForm key={version} sessionId={sessionId} exercise={exercise} available={available} />
    </li>
  );
}