import { createContext, useCallback, useContext, useRef, useState } from 'react';
import { useAuth } from './AuthContext.jsx';
import { loadPlan, createPlan } from '../api/plan.js';

const Context = createContext(null);

export function PlanProvider({ children }) {
  const { authorized } = useAuth();
  // Ref hält refresh stabil, auch wenn authorized bei jedem Rendern neu entsteht
  const authorizedRef = useRef(authorized);
  authorizedRef.current = authorized;

  const [plan, setPlan] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const running = useRef(null);

  // afterSave = true: auf laufende Anfrage warten und danach sicher NEU laden
  const refresh = useCallback(async (afterSave = false) => {
    while (running.current) {
      if (!afterSave) return running.current;
      try { await running.current; } catch { /* danach folgt ein neuer Ladeversuch */ }
    }
    const work = async () => {
      setBusy(true);
      setError('');
      try {
        const call = authorizedRef.current;
        let data;
        try {
          data = await loadPlan(call);
        } catch (e) {
          if (e.message !== 'No plan found.') throw e;
          await createPlan(call);
          data = await loadPlan(call);
        }
        setPlan(data);
        return data;
      } catch (e) {
        setError(e.message);
        throw e; // Aufrufer (z. B. MarkForm) erfährt vom Fehler
      } finally {
        setBusy(false);
      }
    };
    running.current = work();
    try { return await running.current; } finally { running.current = null; }
  }, []);

  return (
    <Context.Provider value={{ plan, busy, error, refresh }}>
      {children}
    </Context.Provider>
  );
}

export function usePlan() {
  return useContext(Context);
}