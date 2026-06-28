import { VehiclePosition } from '../../types/transit.types';

interface SubwayRouteViewProps {
  vehicles: VehiclePosition[];
  routeId: string;
}

/**
 * Route diagram / timeline view for subway mode.
 * Since MTA subway feeds don't provide GPS coordinates,
 * we show trains as positioned along a stop sequence
 * with real-time delay and ETA information.
 */
export default function SubwayRouteView({ vehicles, routeId }: SubwayRouteViewProps) {
  if (vehicles.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center h-[500px] bg-gray-50 rounded-2xl border border-gray-100">
        <span className="text-5xl mb-3">🚇</span>
        <p className="text-gray-500 text-sm">No active trains on route {routeId}</p>
        <p className="text-gray-400 text-xs mt-1">Data updates every 30 seconds</p>
      </div>
    );
  }

  // Group vehicles by direction based on [N] / [S] suffix in nextStop
  // Backend appends direction: "Union Sq - 14 St [N]" or "3 Av [S]"
  const northbound = vehicles.filter(v => v.nextStop.endsWith('[N]'));
  const southbound = vehicles.filter(v => v.nextStop.endsWith('[S]'));
  const unknown = vehicles.filter(v => !v.nextStop.endsWith('[N]') && !v.nextStop.endsWith('[S]'));

  const dir1 = northbound.length > 0 ? northbound : unknown.slice(0, Math.ceil(unknown.length / 2));
  const dir2 = southbound.length > 0 ? southbound : unknown.slice(Math.ceil(unknown.length / 2));

  return (
    <div className="bg-white rounded-2xl border border-gray-100 overflow-hidden max-h-[400px] flex flex-col">
      {/* Header */}
      <div className="bg-gradient-to-r from-indigo-600 to-indigo-700 px-5 py-3 flex items-center gap-3">
        <span className="text-white text-lg">🚇</span>
        <div>
          <h3 className="text-white font-semibold text-sm">{routeId} Train — Live Status</h3>
          <p className="text-indigo-200 text-xs">{vehicles.length} active trains</p>
        </div>
      </div>

      {/* Timeline content */}
      <div className="flex-1 overflow-y-auto p-5">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Direction 1: Northbound / Manhattan-bound */}
          <DirectionPanel
            title={northbound.length > 0 ? "Manhattan-bound (N)" : "Direction 1"}
            trains={dir1}
            lineColor="bg-indigo-500"
          />
          {/* Direction 2: Southbound / Brooklyn-bound */}
          <DirectionPanel
            title={southbound.length > 0 ? "Brooklyn-bound (S)" : "Direction 2"}
            trains={dir2}
            lineColor="bg-purple-500"
          />
        </div>
      </div>

      {/* Footer legend */}
      <div className="border-t border-gray-100 px-5 py-2 bg-gray-50 flex items-center gap-4 text-xs text-gray-500">
        <span className="flex items-center gap-1">
          <span className="w-2 h-2 rounded-full bg-green-500" /> On time
        </span>
        <span className="flex items-center gap-1">
          <span className="w-2 h-2 rounded-full bg-amber-500" /> Minor delay
        </span>
        <span className="flex items-center gap-1">
          <span className="w-2 h-2 rounded-full bg-red-500" /> Significant delay
        </span>
        <span className="flex items-center gap-1">
          <span className="w-2 h-2 rounded-full bg-gray-800" /> Disrupted
        </span>
      </div>
    </div>
  );
}

// ─── Sub-components ──────────────────────────────────────────────────────────

interface DirectionPanelProps {
  title: string;
  trains: VehiclePosition[];
  lineColor: string;
}

function DirectionPanel({ title, trains, lineColor }: DirectionPanelProps) {
  if (trains.length === 0) return null;

  return (
    <div>
      <h4 className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-3">
        {title}
      </h4>
      <div className="relative">
        {/* Vertical line */}
        <div className={`absolute left-3 top-2 bottom-2 w-0.5 ${lineColor} opacity-30 rounded-full`} />

        {/* Train entries */}
        <div className="space-y-1">
          {trains.map((train) => (
            <TrainEntry key={train.vehicleId} train={train} />
          ))}
        </div>
      </div>
    </div>
  );
}

interface TrainEntryProps {
  train: VehiclePosition;
}

function TrainEntry({ train }: TrainEntryProps) {
  const statusColor = getStatusColor(train);
  const statusIcon = getStatusIcon(train);
  // Strip direction suffix [N] or [S] for display
  const displayStop = train.nextStop.replace(/\s*\[[NS]\]$/, '');

  return (
    <div className="flex items-start gap-3 pl-1 py-2 group hover:bg-gray-50 rounded-lg transition-colors">
      {/* Dot on the timeline */}
      <div className="relative flex-shrink-0 mt-1">
        <div className={`w-5 h-5 rounded-full border-2 border-white shadow-sm flex items-center justify-center ${statusColor}`}>
          <span className="text-[10px]">{statusIcon}</span>
        </div>
      </div>

      {/* Train info */}
      <div className="flex-1 min-w-0">
        <div className="flex items-center justify-between">
          <span className="text-sm font-medium text-gray-800 truncate">
            {displayStop === 'In transit' ? 'Between stations' : displayStop}
          </span>
          <DelayBadge delayMinutes={train.delayMinutes} />
        </div>
        <div className="flex items-center gap-2 mt-0.5">
          <span className="text-xs text-gray-500">{train.eta}</span>
          <span className="text-xs text-gray-300">•</span>
          <span className="text-xs text-gray-400">ID: {train.vehicleId}</span>
          {train.crowding !== 'LOW' && (
            <>
              <span className="text-xs text-gray-300">•</span>
              <CrowdingIndicator level={train.crowding} />
            </>
          )}
        </div>
      </div>
    </div>
  );
}

function DelayBadge({ delayMinutes }: { delayMinutes: number }) {
  if (delayMinutes === 0) {
    return <span className="text-xs font-medium text-green-600 bg-green-50 px-2 py-0.5 rounded-full">On time</span>;
  }
  if (delayMinutes <= 5) {
    return <span className="text-xs font-medium text-amber-600 bg-amber-50 px-2 py-0.5 rounded-full">+{delayMinutes} min</span>;
  }
  return <span className="text-xs font-medium text-red-600 bg-red-50 px-2 py-0.5 rounded-full">+{delayMinutes} min</span>;
}

function CrowdingIndicator({ level }: { level: string }) {
  const config = {
    MEDIUM: { label: 'Busy', color: 'text-amber-500' },
    HIGH: { label: 'Full', color: 'text-red-500' },
  }[level] ?? { label: '', color: '' };

  if (!config.label) return null;
  return <span className={`text-xs ${config.color}`}>👥 {config.label}</span>;
}

function getStatusColor(train: VehiclePosition): string {
  if (train.disrupted) return 'bg-gray-800';
  if (train.delayMinutes > 15) return 'bg-red-500';
  if (train.delayMinutes > 5) return 'bg-amber-500';
  return 'bg-green-500';
}

function getStatusIcon(train: VehiclePosition): string {
  if (train.disrupted) return '⚠';
  return '🚇';
}
