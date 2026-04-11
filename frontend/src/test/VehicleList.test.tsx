import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import VehicleList from '../../components/VehicleList/VehicleList';
import { VehiclePosition } from '../../types/transit.types';

const makeVehicle = (overrides: Partial<VehiclePosition> = {}): VehiclePosition => ({
  vehicleId: 'V001',
  lat: 40.71,
  lon: -74.00,
  nextStop: 'Times Square',
  eta: '3 min',
  crowding: 'LOW',
  delayMinutes: 0,
  disrupted: false,
  ...overrides,
});

describe('VehicleList', () => {
  it('shows empty state when no vehicles', () => {
    render(<VehicleList vehicles={[]} />);
    expect(screen.getByText(/No vehicles found/i)).toBeInTheDocument();
  });

  it('renders vehicle ID and next stop', () => {
    render(<VehicleList vehicles={[makeVehicle()]} />);
    expect(screen.getByText('V001')).toBeInTheDocument();
    expect(screen.getByText(/Times Square/)).toBeInTheDocument();
  });

  it('shows ETA', () => {
    render(<VehicleList vehicles={[makeVehicle({ eta: '5 min' })]} />);
    expect(screen.getByText('5 min')).toBeInTheDocument();
  });

  it('shows On time when delay is 0', () => {
    render(<VehicleList vehicles={[makeVehicle({ delayMinutes: 0 })]} />);
    expect(screen.getByText('On time')).toBeInTheDocument();
  });

  it('shows delay minutes when delayed', () => {
    render(<VehicleList vehicles={[makeVehicle({ delayMinutes: 18 })]} />);
    expect(screen.getByText('18 min delay')).toBeInTheDocument();
  });

  it('shows disrupted badge when vehicle is disrupted', () => {
    render(<VehicleList vehicles={[makeVehicle({ disrupted: true })]} />);
    expect(screen.getByText(/Disrupted/i)).toBeInTheDocument();
  });

  it('calls onSelect when vehicle card is clicked', () => {
    const onSelect = vi.fn();
    const vehicle = makeVehicle();
    render(<VehicleList vehicles={[vehicle]} onSelect={onSelect} />);
    fireEvent.click(screen.getByText('V001').closest('button')!);
    expect(onSelect).toHaveBeenCalledWith(vehicle);
  });

  it('highlights selected vehicle', () => {
    const vehicle = makeVehicle({ vehicleId: 'V001' });
    const { container } = render(
      <VehicleList vehicles={[vehicle]} selectedId="V001" />
    );
    const card = container.querySelector('button');
    expect(card?.className).toContain('border-brand-500');
  });
});
