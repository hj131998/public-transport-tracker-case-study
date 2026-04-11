import { useTransitContext } from '../context/TransitContext';

export function useOfflineMode() {
  const { state, dispatch } = useTransitContext();
  return {
    offlineMode: state.offlineMode,
    toggle: () => dispatch({ type: 'TOGGLE_OFFLINE' }),
  };
}
