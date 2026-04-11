import React from 'react';
import { DataSource } from '../../types/transit.types';
import { dataSourceBadge } from '../../utils/formatters';
import { useOfflineMode } from '../../hooks/useOfflineMode';

interface Props {
  dataSource?: DataSource;
  warning?: string;
  cacheAgeSeconds?: number;
}

export default function OfflineModeToggle({ dataSource, warning, cacheAgeSeconds }: Props) {
  const { offlineMode, toggle } = useOfflineMode();

  const badge = dataSource ? dataSourceBadge(dataSource) : null;

  return (
    <div className="flex flex-wrap items-center gap-3">
      {/* Data source badge */}
      {badge && (
        <span className={`text-xs font-semibold px-3 py-1 rounded-full ${badge.className}`}>
          {badge.label}
        </span>
      )}

      {/* Cache age */}
      {cacheAgeSeconds !== undefined && cacheAgeSeconds > 0 && (
        <span className="text-xs text-gray-400">
          Updated {cacheAgeSeconds}s ago
        </span>
      )}

      {/* Warning */}
      {warning && (
        <span className="text-xs text-yellow-700 bg-yellow-50 border border-yellow-200 px-3 py-1 rounded-full">
          ⚠ {warning}
        </span>
      )}

      {/* Toggle button */}
      <button
        onClick={toggle}
        className={`ml-auto text-xs font-medium px-3 py-1.5 rounded-full border transition-all ${
          offlineMode
            ? 'border-gray-300 text-gray-600 hover:border-brand-400 hover:text-brand-600'
            : 'border-green-300 text-green-700 hover:border-red-300 hover:text-red-600'
        }`}
      >
        {offlineMode ? '↺ Switch to Live' : '⏸ Go Offline'}
      </button>
    </div>
  );
}
