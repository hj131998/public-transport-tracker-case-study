import React from 'react';
import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import AlertBanner from '../../components/AlertBanner/AlertBanner';
import { Alert } from '../../types/transit.types';

const makeAlert = (overrides: Partial<Alert> = {}): Alert => ({
  type: 'DELAY',
  severity: 'HIGH',
  message: 'Significant delays - Plan accordingly',
  generatedAt: new Date().toISOString(),
  ...overrides,
});

describe('AlertBanner', () => {
  it('renders nothing when alerts array is empty', () => {
    const { container } = render(<AlertBanner alerts={[]} />);
    expect(container.firstChild).toBeNull();
  });

  it('renders a delay alert with correct message', () => {
    render(<AlertBanner alerts={[makeAlert()]} />);
    expect(screen.getByText('Significant delays - Plan accordingly')).toBeInTheDocument();
  });

  it('renders severity badge', () => {
    render(<AlertBanner alerts={[makeAlert({ severity: 'HIGH' })]} />);
    expect(screen.getByText('HIGH')).toBeInTheDocument();
  });

  it('renders multiple alerts', () => {
    const alerts: Alert[] = [
      makeAlert({ type: 'DELAY', message: 'Delay alert' }),
      makeAlert({ type: 'CROWDING', message: 'Crowding alert', severity: 'MEDIUM' }),
    ];
    render(<AlertBanner alerts={alerts} />);
    expect(screen.getByText('Delay alert')).toBeInTheDocument();
    expect(screen.getByText('Crowding alert')).toBeInTheDocument();
  });

  it('renders disruption alert with correct icon', () => {
    render(<AlertBanner alerts={[makeAlert({ type: 'DISRUPTION', message: 'Service disrupted' })]} />);
    expect(screen.getByText('🚨')).toBeInTheDocument();
  });

  it('renders weather alert', () => {
    render(<AlertBanner alerts={[makeAlert({ type: 'WEATHER', message: 'Weather impact on schedule' })]} />);
    expect(screen.getByText('Weather impact on schedule')).toBeInTheDocument();
  });
});
