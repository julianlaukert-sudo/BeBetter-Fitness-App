// Wandelt die Serverantwort in editierbare Formularwerte
export function profileForm(profile) {
  return {
    age: String(profile.personal.age),
    gender: profile.personal.gender,
    height: String(profile.personal.height),
    weight: String(profile.personal.weight),
    goal: profile.training.goal,
    fitnessLevel: profile.training.fitnessLevel,
    trainingDays: [...profile.training.trainingDays],
  };
}