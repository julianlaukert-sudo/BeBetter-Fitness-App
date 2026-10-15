const DEFAULT_LABELS = {
  name: 'Name',
  email: 'Email',
  password: 'Password',
  confirm: 'Confirm password',
}

// Zeigt eine allgemeine Meldung und Feldfehler (vom Frontend oder Backend)
export default function FormErrors({ message, fields = {}, labels = DEFAULT_LABELS }) {
  const entries = Object.entries(fields)
  if (!message && entries.length === 0) return null
  return (
    <div role="alert" className="form-errors">
      {message && <p>{message}</p>}
      {entries.length > 0 && (
        <ul>
          {entries.map(([field, text]) => (
            <li key={field}>{labels[field] ?? field}: {text}</li>
          ))}
        </ul>
      )}
    </div>
  )
}