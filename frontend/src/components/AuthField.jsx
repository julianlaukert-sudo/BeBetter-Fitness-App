import { useEffect, useId, useRef, useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';

// Eingabefeld für Login und Registrierung: Beschriftung, Icon und bei Passwörtern
// ein Button zum Anzeigen. Alle übrigen Eigenschaften gehen unverändert an das <input>.
export default function AuthField({ label, icon: Icon, type = 'text', reveal = false, ...inputProps }) {
  const [visible, setVisible] = useState(false);
  const inputRef = useRef(null);
  const inputId = useId();

  // Nach dem Absenden das Passwort wieder verbergen
  useEffect(() => {
    const form = inputRef.current?.form;
    if (!reveal || !form) return undefined;
    const hide = () => setVisible(false);
    form.addEventListener('submit', hide);
    return () => form.removeEventListener('submit', hide);
  }, [reveal]);

  return (
    <div className="auth-field">
      <label htmlFor={inputId}>{label}</label>
      <div className={`auth-field-control${reveal ? ' auth-field-control--toggle' : ''}`}>
        {Icon && <Icon className="auth-field-icon" size={18} aria-hidden="true" />}
        <input
          ref={inputRef}
          id={inputId}
          type={reveal && visible ? 'text' : type}
          {...inputProps}
        />
        {reveal && (
          <button
            type="button"
            className="icon-button auth-field-toggle"
            onClick={() => setVisible(v => !v)}
            aria-controls={inputId}
            aria-pressed={visible}
            aria-label={visible ? 'Hide password' : 'Show password'}
            title={visible ? 'Hide password' : 'Show password'}
          >
            {visible ? <EyeOff size={18} aria-hidden="true" /> : <Eye size={18} aria-hidden="true" />}
          </button>
        )}
      </div>
    </div>
  );
}