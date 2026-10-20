// Plananfragen dürfen länger dauern (mehrere ExerciseDB-Listen)
export const loadPlan = authorized => authorized('/plans/current', { timeout: 60000 });
export const createPlan = authorized => authorized('/plans', { method: 'POST', timeout: 60000 });