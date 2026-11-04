// Prüft die Formularwerte und baut den Body für POST /marks.
// Wirft einen Error , wenn etwas nicht passt.
// KI-Log Nr. 16
export function markPayload(sessionId, exercise, values) {
  if (!['true', 'false'].includes(values.done))
    throw new Error('Please select done or not done.');
  const number = (value, label) => {
    const text = String(value).trim().replace(',', '.');
    if (!/^\d+(\.\d+)?$/.test(text)) throw new Error(`${label}: please enter a number.`);
    return Number(text);
  };
  const reps = number(values.reps, 'Reps');
  if (!Number.isInteger(reps) || reps < 0 || reps > 200)
    throw new Error('Reps: whole number from 0 to 200.');
  const weight = exercise.bodyweight ? null : number(values.weight, 'Weight');
  if (weight !== null && (weight > 500 || Math.abs(weight * 10 - Math.round(weight * 10)) > 1e-8))
    throw new Error('Weight: 0 to 500 kg with at most one decimal place.');
  return {
    sessionId,
    planExerciseId: exercise.planExerciseId,
    done: values.done === 'true',
    weight,
    reps,
  };
}