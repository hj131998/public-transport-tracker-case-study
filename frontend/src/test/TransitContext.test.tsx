import { render, screen, act } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { TransitProvider, useTransitContext } from '../context/TransitContext';
import { TransitResponse, Alert } from '../types/transit.types';

// Helper component that exposes state and dispatch for testing
function TestConsumer() {
  const { state, dispatch } = useTransitContext();
  return (
    <div>
      <span data-testid="loading">{String(state.loading)}</span>
      <span data-testid="error">{state.error ?? 'null'}</span>
      <span data-testid="offline">{String(state.offlineMode)}</span>
      <span data-testid="city">{state.query?.city ?? 'none'}</span>
      <span data-testid="data-source">{state.data?.dataSource ?? 'none'}</span>
      <span data-testid="route-id">{state.data?.routeId ?? 'none'}</span>
      <span data-testid="notifications">{state.notifications.length}</span>
      <span data-testid="notification-ids">
        {state.notifications.map((n) => n.id).join(',')}
      </span>
      <span data-testid="notification-read">
        {state.notifications.map((n) => String(n.read)).join(',')}
      </span>
      <button onClick={() => dispatch({ type: 'FETCH_START' })}>fetchStart</button>
      <button
        onClick={() =>
          dispatch({
            type: 'FETCH_SUCCESS',
            payload: {
              routeId: 'L',
              city: 'NYC',
              dataSource: 'LIVE',
              cacheAgeSeconds: 0,
              offline: false,
              vehicles: [],
              alerts: [],
              links: {},
            } as TransitResponse,
          })
        }
      >
        fetchSuccess
      </button>
      <button
        onClick={() => dispatch({ type: 'FETCH_ERROR', payload: 'Network error' })}
      >
        fetchError
      </button>
      <button onClick={() => dispatch({ type: 'TOGGLE_OFFLINE' })}>toggleOffline</button>
      <button
        onClick={() =>
          dispatch({
            type: 'ADD_NOTIFICATION',
            payload: {
              type: 'DELAY',
              severity: 'HIGH',
              message: 'Test alert',
              generatedAt: new Date().toISOString(),
            } as Alert,
          })
        }
      >
        addNotification
      </button>
      <button onClick={() => dispatch({ type: 'SET_QUERY', payload: { city: 'NYC', route: 'L', mode: 'BUS' } })}>
        setQuery
      </button>
    </div>
  );
}

function renderWithProvider() {
  return render(
    <TransitProvider>
      <TestConsumer />
    </TransitProvider>
  );
}

describe('TransitContext reducer', () => {
  describe('FETCH_START', () => {
    it('sets loading=true and error=null', () => {
      renderWithProvider();

      // First set an error so we can verify it gets cleared
      act(() => screen.getByText('fetchError').click());
      expect(screen.getByTestId('error').textContent).toBe('Network error');

      // Now dispatch FETCH_START
      act(() => screen.getByText('fetchStart').click());
      expect(screen.getByTestId('loading').textContent).toBe('true');
      expect(screen.getByTestId('error').textContent).toBe('null');
    });
  });

  describe('FETCH_SUCCESS', () => {
    it('sets data, loading=false', () => {
      renderWithProvider();

      // Start a fetch first to set loading=true
      act(() => screen.getByText('fetchStart').click());
      expect(screen.getByTestId('loading').textContent).toBe('true');

      // Dispatch success
      act(() => screen.getByText('fetchSuccess').click());
      expect(screen.getByTestId('loading').textContent).toBe('false');
      expect(screen.getByTestId('data-source').textContent).toBe('LIVE');
      expect(screen.getByTestId('route-id').textContent).toBe('L');
      expect(screen.getByTestId('error').textContent).toBe('null');
    });
  });

  describe('FETCH_ERROR', () => {
    it('sets error message, loading=false', () => {
      renderWithProvider();

      // Start a fetch first to set loading=true
      act(() => screen.getByText('fetchStart').click());
      expect(screen.getByTestId('loading').textContent).toBe('true');

      // Dispatch error
      act(() => screen.getByText('fetchError').click());
      expect(screen.getByTestId('loading').textContent).toBe('false');
      expect(screen.getByTestId('error').textContent).toBe('Network error');
    });
  });

  describe('ADD_NOTIFICATION', () => {
    it('adds notification to the list', () => {
      renderWithProvider();

      expect(screen.getByTestId('notifications').textContent).toBe('0');
      act(() => screen.getByText('addNotification').click());
      expect(screen.getByTestId('notifications').textContent).toBe('1');
    });

    it('caps notifications at 20', () => {
      renderWithProvider();

      // Add 25 notifications
      act(() => {
        for (let i = 0; i < 25; i++) {
          screen.getByText('addNotification').click();
        }
      });

      // Should never exceed 20
      expect(Number(screen.getByTestId('notifications').textContent)).toBe(20);
    });
  });

  describe('MARK_READ', () => {
    it('updates notification read flag', () => {
      // Use a custom component that can dispatch MARK_READ with a specific id
      function MarkReadConsumer() {
        const { state, dispatch } = useTransitContext();
        return (
          <div>
            <span data-testid="notif-count">{state.notifications.length}</span>
            <span data-testid="first-read">
              {state.notifications.length > 0 ? String(state.notifications[0].read) : 'none'}
            </span>
            <span data-testid="first-id">
              {state.notifications.length > 0 ? state.notifications[0].id : 'none'}
            </span>
            <button
              onClick={() =>
                dispatch({
                  type: 'ADD_NOTIFICATION',
                  payload: {
                    type: 'DISRUPTION',
                    severity: 'HIGH',
                    message: 'Service alert',
                    generatedAt: new Date().toISOString(),
                  } as Alert,
                })
              }
            >
              add
            </button>
            <button
              onClick={() => {
                const id = state.notifications[0]?.id;
                if (id) dispatch({ type: 'MARK_READ', payload: id });
              }}
            >
              markRead
            </button>
          </div>
        );
      }

      render(
        <TransitProvider>
          <MarkReadConsumer />
        </TransitProvider>
      );

      // Add a notification
      act(() => screen.getByText('add').click());
      expect(screen.getByTestId('first-read').textContent).toBe('false');

      // Mark it as read
      act(() => screen.getByText('markRead').click());
      expect(screen.getByTestId('first-read').textContent).toBe('true');
    });
  });

  describe('TOGGLE_OFFLINE', () => {
    it('flips offlineMode from false to true', () => {
      renderWithProvider();

      expect(screen.getByTestId('offline').textContent).toBe('false');
      act(() => screen.getByText('toggleOffline').click());
      expect(screen.getByTestId('offline').textContent).toBe('true');
    });

    it('flips offlineMode back to false on second toggle', () => {
      renderWithProvider();

      act(() => screen.getByText('toggleOffline').click());
      expect(screen.getByTestId('offline').textContent).toBe('true');
      act(() => screen.getByText('toggleOffline').click());
      expect(screen.getByTestId('offline').textContent).toBe('false');
    });
  });

  describe('SET_QUERY', () => {
    it('updates the query state', () => {
      renderWithProvider();

      expect(screen.getByTestId('city').textContent).toBe('none');
      act(() => screen.getByText('setQuery').click());
      expect(screen.getByTestId('city').textContent).toBe('NYC');
    });
  });
});
