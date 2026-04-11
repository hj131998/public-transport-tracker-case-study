import { useCallback, useEffect, useRef } from 'react';
import { transitApi } from '../services/apiService';
import { useTransitContext } from '../context/TransitContext';

const REFRESH_INTERVAL_MS = 30_000;

export function useTransitData() {
  const { state, dispatch } = useTransitContext();
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const fetch = useCallback(async () => {
    if (!state.query) return;
    dispatch({ type: 'FETCH_START' });
    try {
      const data = await transitApi.getTransitData(state.query.city, state.query.route);
      dispatch({ type: 'FETCH_SUCCESS', payload: data });
      // Push new alerts as notifications
      data.alerts.forEach((alert) => dispatch({ type: 'ADD_NOTIFICATION', payload: alert }));
    } catch (err) {
      dispatch({ type: 'FETCH_ERROR', payload: (err as Error).message });
    }
  }, [state.query, dispatch]);

  // Fetch on query change
  useEffect(() => {
    if (!state.query) return;
    fetch();
  }, [state.query, fetch]);

  // Auto-refresh every 30s unless offline mode is on
  useEffect(() => {
    if (state.offlineMode || !state.query) {
      if (intervalRef.current) clearInterval(intervalRef.current);
      return;
    }
    intervalRef.current = setInterval(fetch, REFRESH_INTERVAL_MS);
    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current);
    };
  }, [state.offlineMode, state.query, fetch]);

  return {
    data: state.data,
    loading: state.loading,
    error: state.error,
    refetch: fetch,
  };
}
