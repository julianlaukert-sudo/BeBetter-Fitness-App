// Baut den Body für POST /marks. Seit KI-Log Nr. 32 nur noch geschafft / nicht geschafft.
// KI-Log Nr. 16, 32
export function markPayload(sessionId, exercise, done) {
    if (typeof done !== 'boolean') throw new Error('Please choose done or not done.');
    return { sessionId, planExerciseId: exercise.planExerciseId, done };
}