import { useTransitContext } from '../context/TransitContext';

export function useNotifications() {
  const { state, dispatch } = useTransitContext();
  const unreadCount = state.notifications.filter((n) => !n.read).length;

  return {
    notifications: state.notifications,
    unreadCount,
    markRead: (id: string) => dispatch({ type: 'MARK_READ', payload: id }),
    clearAll: () => dispatch({ type: 'CLEAR_NOTIFICATIONS' }),
  };
}
