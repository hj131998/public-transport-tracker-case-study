import { CrowdingLevel, AlertType, Severity, DataSource } from '../types/transit.types';

export function crowdingColor(level: CrowdingLevel): string {
  return { LOW: 'text-green-600', MEDIUM: 'text-yellow-600', HIGH: 'text-red-600' }[level];
}

export function crowdingBg(level: CrowdingLevel): string {
  return { LOW: 'bg-green-100', MEDIUM: 'bg-yellow-100', HIGH: 'bg-red-100' }[level];
}

export function crowdingLabel(level: CrowdingLevel): string {
  return { LOW: 'Low', MEDIUM: 'Moderate', HIGH: 'Full' }[level];
}

export function alertBg(type: AlertType): string {
  return {
    DELAY: 'bg-orange-50 border-orange-400',
    DISRUPTION: 'bg-red-50 border-red-400',
    CROWDING: 'bg-yellow-50 border-yellow-400',
    WEATHER: 'bg-blue-50 border-blue-400',
  }[type];
}

export function alertIcon(type: AlertType): string {
  return { DELAY: '⏱', DISRUPTION: '🚨', CROWDING: '👥', WEATHER: '🌧' }[type];
}

export function severityBadge(severity: Severity): string {
  return {
    LOW: 'bg-gray-100 text-gray-700',
    MEDIUM: 'bg-yellow-100 text-yellow-800',
    HIGH: 'bg-red-100 text-red-800',
  }[severity];
}

export function dataSourceBadge(source: DataSource): { label: string; className: string } {
  return {
    LIVE:   { label: '● LIVE',   className: 'bg-green-100 text-green-800' },
    CACHED: { label: '◎ CACHED', className: 'bg-blue-100 text-blue-800' },
    STALE:  { label: '⚠ STALE',  className: 'bg-yellow-100 text-yellow-800' },
    MOCK:   { label: '○ OFFLINE', className: 'bg-gray-100 text-gray-600' },
  }[source];
}

export function formatDelay(minutes: number): string {
  if (minutes === 0) return 'On time';
  return `${minutes} min delay`;
}

export function formatDuration(minutes: number): string {
  if (minutes < 60) return `${minutes} min`;
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return m > 0 ? `${h}h ${m}m` : `${h}h`;
}
