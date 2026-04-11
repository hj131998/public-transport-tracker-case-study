import React from 'react';
import { render, screen, act } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { TransitProvider, useTransitContext } from '../../context/TransitContext';
import { TransitResponse } from '../../types/transit.types';

function TestConsumer() {
  const { state, dispatch } = useTransitContext();
  return (
    <div>
      <span data-testid="loading">{String(state.loading)}</span>
      <span data-testid="offline">{String(state.offlineMode)}</span>
      <span data-testid="city">{state.query?.city ?? 'none'}</span>
      <span data-testid="notifications">{state.notifications.length}</span>
      <button onClick={() => dispatch({ type: 'FETCH_START' })}>start</button>
      <button onClick={() => dispatch({ type: 'TOGGLE_OFFLINE' })}>toggle</button>
      <button onClick={() => dispatch({ type: 'SET_QUERY', payload: { city: 'NYC', route: 'M15' } })}>
        setQuery
      </button>
      <button onClick={() => dispatch({
        type: 'ADD_NOTIFICATION',
        payload: { type: 'DELAY', severity: 'HIGH', message: 'Test alert', generatedAt: '' }
      })}>
        addNotif
      </button>
    </div>
  );
}

function wrap() {
  return render(<TransitProvider><TestConsumer /></TransitProvider>);
}

describe('TransitContext', () => {
  it('initialises with loading=false', () => {
    wrap();
    expect(screen.getByTestId('loading').textContent).toBe('false');
  });

  it('FETCH_START sets loading=true', () => {
    wrap();
    act(() => screen.getByText('start').click());
    expect(screen.getByTestId('loading').textContent).toBe('true');
  });

  it('TOGGLE_OFFLINE flips offlineMode', () => {
    wrap();
    expect(screen.getByTestId('offline').textContent).toBe('false');
    act(() => screen.getByText('toggle').click());
    expect(screen.getByTestId('offline').textContent).toBe('true');
  });

  it('SET_QUERY updates city', () => {
    wrap();
    act(() => screen.getByText('setQuery').click());
    expect(screen.getByTestId('city').textContent).toBe('NYC');
  });

  it('ADD_NOTIFICATION increments notification count', () => {
    wrap();
    expect(screen.getByTestId('notifications').textContent).toBe('0');
    act(() => screen.getByText('addNotif').click());
    expect(screen.getByTestId('notifications').textContent).toBe('1');
  });
});
