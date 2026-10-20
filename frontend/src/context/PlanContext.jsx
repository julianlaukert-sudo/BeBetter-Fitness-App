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

  const refresh = useCallback(async () => {
    // Gleichzeitige Ladeversuche teilen sich eine laufende Anfragefolge
    if (running.current) return running.current;
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
          await createPlan(call);       // idempotent im Backend
          data = await loadPlan(call);
        }
        setPlan(data);
      } catch (e) {
        setError(e.message);
      } finally {
        setBusy(false);
      }
    };
    running.current = work();
    try { await running.current; } finally { running.current = null; }
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