export type DataSource = 'LIVE' | 'CACHED' | 'STALE' | 'MOCK';
export type CrowdingLevel = 'LOW' | 'MEDIUM' | 'HIGH';
export type AlertType = 'DELAY' | 'DISRUPTION' | 'CROWDING' | 'WEATHER';
export type Severity = 'LOW' | 'MEDIUM' | 'HIGH';

export interface VehiclePosition {
  vehicleId: string;
  lat: number;
  lon: number;
  nextStop: string;
  eta: string;
  crowding: CrowdingLevel;
  delayMinutes: number;
  disrupted: boolean;
}

export interface Alert {
  type: AlertType;
  severity: Severity;
  message: string;
  generatedAt: string;
}

export interface Stop {
  stopId: string;
  name: string;
  lat: number;
  lon: number;
  eta: string;
}

export interface Route {
  routeId: string;
  stops: Stop[];
  durationMinutes: number;
  hasDisruption: boolean;
}

export interface RoutePlan {
  primaryRoute: Route;
  alternatives: Route[];
  estimatedMinutes: number;
}

export interface HateoasLink {
  href: string;
}

export interface TransitResponse {
  routeId: string;
  city: string;
  dataSource: DataSource;
  cacheAgeSeconds: number;
  offline: boolean;
  warning?: string;
  vehicles: VehiclePosition[];
  alerts: Alert[];
  routePlan?: RoutePlan;
  links: Record<string, HateoasLink>;
}

export interface TransitQuery {
  city: string;
  route: string;
}

export interface RoutePlanQuery {
  city: string;
  from: string;
  to: string;
}

export interface Notification {
  id: string;
  message: string;
  type: AlertType;
  timestamp: Date;
  read: boolean;
}
