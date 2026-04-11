import React, { useState } from 'react';
import { RoutePlan, Route } from '../../types/transit.types';
import { formatDuration } from '../../utils/formatters';

interface Props {
  routePlan: RoutePlan;
}

function RouteCard({ route, label, recommended }: { route: Route; label: string; recommended?: boolean }) {
  const [expanded, setExpanded] = useState(false);

  return (
    <div className={`rounded-xl border p-4 transition-all ${
      recommended ? 'border-brand-400 bg-brand-50' : 'border-gray-100 bg-white'
    }`}>
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <span className="text-sm font-semibold text-gray-700">{label}</span>
          {recommended && (
            <span className="text-xs bg-brand-600 text-white px-2 py-0.5 rounded-full">Recommended</span>
          )}
          {route.hasDisruption && (
            <span className="text-xs bg-red-100 text-red-700 px-2 py-0.5 rounded-full">⚠ Disrupted</span>
          )}
        </div>
        <div className="flex items-center gap-3">
          <span className="text-sm font-bold text-brand-700">{formatDuration(route.durationMinutes)}</span>
          <button
            onClick={() => setExpanded(!expanded)}
            className="text-gray-400 hover:text-gray-600 transition-colors"
          >
            <svg className={`w-4 h-4 transition-transform ${expanded ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
            </svg>
          </button>
        </div>
      </div>

      {expanded && route.stops.length > 0 && (
        <div className="mt-3 space-y-1 animate-fade-in">
          {route.stops.map((stop, i) => (
            <div key={stop.stopId} className="flex items-center gap-2 text-xs text-gray-600">
              <div className="flex flex-col items-center">
                <div className={`w-2 h-2 rounded-full ${i === 0 ? 'bg-green-500' : i === route.stops.length - 1 ? 'bg-red-500' : 'bg-gray-300'}`} />
                {i < route.stops.length - 1 && <div className="w-0.5 h-3 bg-gray-200" />}
              </div>
              <span className="flex-1">{stop.name}</span>
              <span className="text-brand-600 font-medium">{stop.eta}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default function RouteAlternatives({ routePlan }: Props) {
  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between mb-1">
        <h3 className="text-sm font-semibold text-gray-700">Route Options</h3>
        <span className="text-xs text-gray-400">Est. {formatDuration(routePlan.estimatedMinutes)}</span>
      </div>

      <RouteCard route={routePlan.primaryRoute} label="Primary Route" recommended />

      {routePlan.alternatives.map((alt, i) => (
        <RouteCard key={alt.routeId} route={alt} label={`Alternative ${i + 1}`} />
      ))}
    </div>
  );
}
