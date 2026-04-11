import React, { useState } from 'react';
import { VehiclePosition } from '../../types/transit.types';
import { crowdingColor, crowdingBg, crowdingLabel, formatDelay } from '../../utils/formatters';

interface Props {
  vehicles: VehiclePosition[];
  onSelect?: (vehicle: VehiclePosition) => void;
  selectedId?: string;
}

export default function VehicleList({ vehicles, onSelect, selectedId }: Props) {
  const [sortBy, setSortBy] = useState<'eta' | 'delay' | 'crowding'>('eta');

  const sorted = [...vehicles].sort((a, b) => {
    if (sortBy === 'delay') return b.delayMinutes - a.delayMinutes;
    if (sortBy === 'crowding') {
      const order = { HIGH: 0, MEDIUM: 1, LOW: 2 };
      return order[a.crowding] - order[b.crowding];
    }
    return a.eta.localeCompare(b.eta);
  });

  if (vehicles.length === 0) {
    return (
      <div className="text-center py-10 text-gray-400">
        <span className="text-4xl block mb-2">🚌</span>
        <p className="text-sm">No vehicles found for this route</p>
      </div>
    );
  }

  return (
    <div>
      {/* Sort controls */}
      <div className="flex items-center gap-2 mb-3">
        <span className="text-xs text-gray-400">Sort:</span>
        {(['eta', 'delay', 'crowding'] as const).map((s) => (
          <button
            key={s}
            onClick={() => setSortBy(s)}
            className={`text-xs px-2.5 py-1 rounded-full transition-colors capitalize ${
              sortBy === s ? 'bg-brand-600 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
            }`}
          >
            {s}
          </button>
        ))}
      </div>

      <div className="space-y-2">
        {sorted.map((v) => (
          <button
            key={v.vehicleId}
            onClick={() => onSelect?.(v)}
            className={`w-full text-left p-4 rounded-xl border transition-all ${
              selectedId === v.vehicleId
                ? 'border-brand-500 bg-brand-50 shadow-sm'
                : 'border-gray-100 bg-white hover:border-gray-200 hover:shadow-sm'
            }`}
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="text-lg">🚌</span>
                <div>
                  <p className="text-sm font-semibold text-gray-800">{v.vehicleId}</p>
                  <p className="text-xs text-gray-500">→ {v.nextStop}</p>
                </div>
              </div>
              <div className="text-right">
                <p className="text-sm font-bold text-brand-700">{v.eta}</p>
                <p className={`text-xs ${v.delayMinutes > 0 ? 'text-orange-600' : 'text-green-600'}`}>
                  {formatDelay(v.delayMinutes)}
                </p>
              </div>
            </div>

            <div className="flex items-center gap-2 mt-2">
              <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${crowdingBg(v.crowding)} ${crowdingColor(v.crowding)}`}>
                {crowdingLabel(v.crowding)} capacity
              </span>
              {v.disrupted && (
                <span className="text-xs px-2 py-0.5 rounded-full bg-red-100 text-red-700 font-medium">
                  ⚠ Disrupted
                </span>
              )}
            </div>
          </button>
        ))}
      </div>
    </div>
  );
}
