// Plananfragen dürfen länger dauern (mehrere ExerciseDB-Listen)
// KI-Log Nr. 13
export const loadPlan = authorized => authorized('/plans/current', { timeout: 60000 });
export const createPlan = authorized => authorized('/plans', { method: 'POST', timeout: 60000 });