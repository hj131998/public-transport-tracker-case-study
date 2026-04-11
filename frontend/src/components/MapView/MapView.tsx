import React, { useEffect, useRef } from 'react';
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet';
import L from 'leaflet';
import { VehiclePosition } from '../../types/transit.types';
import { crowdingLabel, formatDelay } from '../../utils/formatters';

// Fix Leaflet default icon path issue with Vite
delete (L.Icon.Default.prototype as unknown as Record<string, unknown>)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
});

const busIcon = (disrupted: boolean, selected: boolean) =>
  L.divIcon({
    className: '',
    html: `<div class="flex items-center justify-center w-8 h-8 rounded-full shadow-lg border-2 text-base
      ${selected ? 'border-blue-600 bg-blue-100 scale-125' : disrupted ? 'border-red-500 bg-red-100' : 'border-green-500 bg-white'}
      transition-transform">🚌</div>`,
    iconSize: [32, 32],
    iconAnchor: [16, 16],
  });

function FlyToSelected({ vehicle }: { vehicle: VehiclePosition | null }) {
  const map = useMap();
  const prevRef = useRef<string | null>(null);
  useEffect(() => {
    if (vehicle && vehicle.vehicleId !== prevRef.current) {
      map.flyTo([vehicle.lat, vehicle.lon], 15, { duration: 1 });
      prevRef.current = vehicle.vehicleId;
    }
  }, [vehicle, map]);
  return null;
}

interface Props {
  vehicles: VehiclePosition[];
  selectedVehicle?: VehiclePosition | null;
  onVehicleClick?: (v: VehiclePosition) => void;
}

export default function MapView({ vehicles, selectedVehicle, onVehicleClick }: Props) {
  const center: [number, number] =
    vehicles.length > 0
      ? [vehicles[0].lat, vehicles[0].lon]
      : [40.7128, -74.006]; // Default NYC

  return (
    <div className="rounded-2xl overflow-hidden shadow-sm border border-gray-100 h-full min-h-[400px]">
      <MapContainer
        center={center}
        zoom={13}
        className="h-full w-full"
        style={{ minHeight: '400px' }}
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        <FlyToSelected vehicle={selectedVehicle ?? null} />

        {vehicles.map((v) => (
          <Marker
            key={v.vehicleId}
            position={[v.lat, v.lon]}
            icon={busIcon(v.disrupted, selectedVehicle?.vehicleId === v.vehicleId)}
            eventHandlers={{ click: () => onVehicleClick?.(v) }}
          >
            <Popup>
              <div className="text-sm min-w-[160px]">
                <p className="font-bold text-gray-800 mb-1">🚌 {v.vehicleId}</p>
                <p className="text-gray-600">→ {v.nextStop}</p>
                <p className="text-brand-700 font-semibold">ETA: {v.eta}</p>
                <p className={v.delayMinutes > 0 ? 'text-orange-600' : 'text-green-600'}>
                  {formatDelay(v.delayMinutes)}
                </p>
                <p className="text-gray-500 text-xs mt-1">
                  Capacity: {crowdingLabel(v.crowding)}
                </p>
                {v.disrupted && (
                  <p className="text-red-600 font-semibold text-xs mt-1">⚠ Service disrupted</p>
                )}
              </div>
            </Popup>
          </Marker>
        ))}
      </MapContainer>
    </div>
  );
}
