import React from 'react';
import { Alert } from '../../types/transit.types';
import { alertBg, alertIcon, severityBadge } from '../../utils/formatters';

interface Props {
  alerts: Alert[];
}

export default function AlertBanner({ alerts }: Props) {
  if (alerts.length === 0) return null;

  return (
    <div className="space-y-2 animate-slide-down">
      {alerts.map((alert, i) => (
        <div
          key={i}
          className={`flex items-start gap-3 p-4 rounded-xl border-l-4 ${alertBg(alert.type)}`}
        >
          <span className="text-xl flex-shrink-0 mt-0.5">{alertIcon(alert.type)}</span>
          <div className="flex-1 min-w-0">
            <p className="text-sm font-medium text-gray-800">{alert.message}</p>
          </div>
          <span className={`text-xs font-semibold px-2 py-0.5 rounded-full flex-shrink-0 ${severityBadge(alert.severity)}`}>
            {alert.severity}
          </span>
        </div>
      ))}
    </div>
  );
}
