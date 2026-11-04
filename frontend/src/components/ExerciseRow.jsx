import { useEffect, useRef, useState } from 'react';
import { Dumbbell, Info } from 'lucide-react';
import ExerciseModal from './ExerciseModal.jsx';
import MarkForm from './MarkForm.jsx';
import { exerciseTarget, markSummary } from '../utils/planSummary.js';

const THUMB = 160; // Zeichenfläche des Standbilds (2x, damit es auf Retina-Displays scharf bleibt)

// KI-Log Nr. 27
const PREVIEW = 'frame';

function Thumb({ url, label, onOpen }) {
  const button = useRef(null);
  const canvas = useRef(null);
  const [near, setNear] = useState(false); // Vorschau liegt (fast) im sichtbaren Bereich
  const [view, setView] = useState('icon'); // 'icon' | 'frame' | 'gif'

  // Bilder erst laden, wenn die Zeile in die Nähe des sichtbaren Bereichs kommt
  useEffect(() => {
    setNear(false);
    if (PREVIEW === 'icon' || !url) return undefined;
    const el = button.current;
    if (!el || typeof IntersectionObserver === 'undefined') {
      setNear(true);
      return undefined;
    }
    const observer = new IntersectionObserver(entries => {
      if (entries.some(e => e.isIntersecting)) {
        setNear(true);
        observer.disconnect();
      }
    }, { rootMargin: '300px' });
    observer.observe(el);
    return () => observer.disconnect();
  }, [url]);

  useEffect(() => {
    setView('icon');
    if (PREVIEW === 'icon' || !url || !near) return undefined;
    if (PREVIEW === 'gif') {
      setView('gif');
      return undefined;
    }
    let active = true;
    const img = new Image();
    img.onload = () => {
      if (!active) return;
      if (!img.naturalWidth || !canvas.current) { setView('gif'); return; }
      try {
        const ctx = canvas.current.getContext('2d');
        const scale = Math.min(THUMB / img.naturalWidth, THUMB / img.naturalHeight);
        const w = img.naturalWidth * scale;
        const h = img.naturalHeight * scale;
        ctx.fillStyle = '#fff';
        ctx.fillRect(0, 0, THUMB, THUMB);
        ctx.drawImage(img, (THUMB - w) / 2, (THUMB - h) / 2, w, h);
        setView('frame');
      } catch {
        setView('gif'); // Zeichnen nicht möglich: Bild direkt anzeigen
      }
    };
    img.onerror = () => { if (active) setView('icon'); };
    img.src = url;
    return () => { active = false; img.onload = null; img.onerror = null; };
  }, [url, near]);

  return (
    <button ref={button} type="button" tabIndex={-1} onClick={onOpen}
      className={`exercise-thumb${view === 'icon' ? ' exercise-thumb--empty' : ''}`}
      aria-label={`Show details for ${label}`}>
      {PREVIEW === 'frame' && <canvas ref={canvas} width={THUMB} height={THUMB} hidden={view !== 'frame'} />}
      {view === 'gif' && <img src={url} alt="" decoding="async" onError={() => setView('icon')} />}
      {view === 'icon' && <Dumbbell size={26} aria-hidden="true" className="icon-dumbbell" />}
    </button>
  );
}

export default function ExerciseRow({ exercise, sessionId, available }) {
  const [open, setOpen] = useState(false);
  // key: Formular baut sich nach neuem Laden mit den gespeicherten Werten neu auf
  const version = JSON.stringify(exercise.mark);
  const { name } = exerciseTarget(exercise);
  const state = markSummary(exercise.mark);
  return (
    <li className="exercise-row">
      <div className="exercise-info">
        <Thumb url={exercise.imageUrl} label={name} onOpen={() => setOpen(true)} />
        <div className="exercise-text">
          <h3 className="exercise-name">{name}</h3>
          <p className="exercise-meta">{exercise.sets} sets · {exercise.reps} reps</p>
          <p className="exercise-meta">
            {exercise.bodyweight
              ? 'Bodyweight: no additional weight'
              : `Training target: ${exercise.weight} kg`}
            {!exercise.bodyweight && exercise.equipment === 'dumbbell' && ' per hand'}
          </p>
          <p className={`mark-state mark-state--${state.key}`}>{state.text}</p>
          <button type="button" className="link-action" onClick={() => setOpen(true)}>
            <Info size={16} aria-hidden="true" />
            Exercise details
          </button>
        </div>
      </div>
      {open && <ExerciseModal exercise={exercise} close={() => setOpen(false)} />}
      <MarkForm key={version} sessionId={sessionId} exercise={exercise} available={available} />
    </li>
  );
}