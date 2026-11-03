// KI-Log Nr. 17
const DAYS = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'];
const DAY_LABELS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];

export const FIELD_LABELS = {
  age: 'Age', gender: 'Gender', height: 'Height', weight: 'Weight',
  goal: 'Goal', fitnessLevel: 'Fitness level', trainingDays: 'Training days',
};

export default function ProfileFields({ form, change }) {
  function select(name, label, options) {
    return (
      <label>
        {label}
        <select value={form[name]} onChange={e => change(name, e.target.value)}>
          {options.map(([value, text]) => <option key={value} value={value}>{text}</option>)}
        </select>
      </label>
    );
  }

  return (
    <>
      {['age', 'height', 'weight'].map(name => (
        <label key={name}>
          {{ age: 'Age (years)', height: 'Height (cm)', weight: 'Weight (kg)' }[name]}
          <input
            inputMode={name === 'weight' ? 'decimal' : 'numeric'}
            value={form[name]}
            onChange={e => change(name, e.target.value)}
          />
        </label>
      ))}
      {select('gender', 'Gender', [['male', 'Male'], ['female', 'Female'], ['diverse', 'Diverse']])}
      {select('goal', 'Goal', [['muscle_gain', 'Muscle gain'], ['fat_loss', 'Fat loss'],
        ['health', 'Health']])}
      {select('fitnessLevel', 'Fitness level', [['beginner', 'Beginner'], ['advanced', 'Advanced']])}
      <p>Select exactly three training days with at least one rest day in between.</p>
      <div className="day-options">
        {DAYS.map((day, i) => (
          <label key={day}>
            <input
              type="checkbox"
              checked={form.trainingDays.includes(day)}
              onChange={e => change('trainingDays', e.target.checked
                ? [...form.trainingDays, day]
                : form.trainingDays.filter(d => d !== day))}
            />
            {DAY_LABELS[i]}
          </label>
        ))}
      </div>
    </>
  );
}