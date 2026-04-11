import React, { createContext, useContext, useReducer, ReactNode } from 'react';
import { TransitResponse, TransitQuery, Notification, Alert } from '../types/transit.types';

interface TransitState {
  query: TransitQuery | null;
  data: TransitResponse | null;
  loading: boolean;
  error: string | null;
  offlineMode: boolean;
  notifications: Notification[];
}

type TransitAction =
  | { type: 'SET_QUERY'; payload: TransitQuery }
  | { type: 'FETCH_START' }
  | { type: 'FETCH_SUCCESS'; payload: TransitResponse }
  | { type: 'FETCH_ERROR'; payload: string }
  | { type: 'TOGGLE_OFFLINE' }
  | { type: 'ADD_NOTIFICATION'; payload: Alert }
  | { type: 'MARK_READ'; payload: string }
  | { type: 'CLEAR_NOTIFICATIONS' };

const initialState: TransitState = {
  query: null,
  data: null,
  loading: false,
  error: null,
  offlineMode: false,
  notifications: [],
};

function transitReducer(state: TransitState, action: TransitAction): TransitState {
  switch (action.type) {
    case 'SET_QUERY':
      return { ...state, query: action.payload };
    case 'FETCH_START':
      return { ...state, loading: true, error: null };
    case 'FETCH_SUCCESS':
      return { ...state, loading: false, data: action.payload, error: null };
    case 'FETCH_ERROR':
      return { ...state, loading: false, error: action.payload };
    case 'TOGGLE_OFFLINE':
      return { ...state, offlineMode: !state.offlineMode };
    case 'ADD_NOTIFICATION': {
      const notification: Notification = {
        id: crypto.randomUUID(),
        message: action.payload.message,
        type: action.payload.type,
        timestamp: new Date(),
        read: false,
      };
      return { ...state, notifications: [notification, ...state.notifications].slice(0, 20) };
    }
    case 'MARK_READ':
      return {
        ...state,
        notifications: state.notifications.map((n) =>
          n.id === action.payload ? { ...n, read: true } : n
        ),
      };
    case 'CLEAR_NOTIFICATIONS':
      return { ...state, notifications: [] };
    default:
      return state;
  }
}

interface TransitContextValue {
  state: TransitState;
  dispatch: React.Dispatch<TransitAction>;
}

const TransitContext = createContext<TransitContextValue | null>(null);

export function TransitProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(transitReducer, initialState);
  return (
    <TransitContext.Provider value={{ state, dispatch }}>
      {children}
    </TransitContext.Provider>
  );
}

export function useTransitContext(): TransitContextValue {
  const ctx = useContext(TransitContext);
  if (!ctx) throw new Error('useTransitContext must be used within TransitProvider');
  return ctx;
}
