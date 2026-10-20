import { useState } from 'react';
import ExerciseModal from './ExerciseModal.jsx';

export default function ExerciseRow({ exercise }) {
  const [open, setOpen] = useState(false);
  return (
    <li>
      <strong>{exercise.name || `Exercise ${exercise.exerciseId}`}</strong>
      <p>{exercise.sets} sets · {exercise.reps} reps</p>
      <p>
        {exercise.bodyweight
          ? 'Bodyweight: no additional weight'
          : `Calculated start suggestion: ${exercise.weight} kg`}
        {!exercise.bodyweight && exercise.equipment === 'dumbbell' && ' per hand'}
      </p>
      <button type="button" onClick={() => setOpen(true)}>Exercise details</button>
      {open && <ExerciseModal exercise={exercise} close={() => setOpen(false)} />}
    </li>
  );
}