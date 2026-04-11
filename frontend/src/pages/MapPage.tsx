import React, { useState, Suspense, lazy } from 'react';
import VehicleList from '../components/VehicleList/VehicleList';
import AlertBanner from '../components/AlertBanner/AlertBanner';
import OfflineModeToggle from '../components/OfflineModeToggle/OfflineModeToggle';
import RouteSearchPanel from '../components/RouteSearchPanel/RouteSearchPanel';
import { useTransitData } from '../hooks/useTransitData';
import { VehiclePosition } from '../types/transit.types';

const MapView = lazy(() => import('../components/MapView/MapView'));

export default function MapPage() {
  const { data, loading, refetch } = useTransitData();
  const [selectedVehicle, setSelectedVehicle] = useState<VehiclePosition | null>(null);
  const [sidebarOpen, setSidebarOpen] = useState(true);

  return (
    <div className="flex h-[calc(100vh-64px)] overflow-hidden">

      {/* Sidebar */}
      <div className={`flex-shrink-0 bg-white border-r border-gray-100 overflow-y-auto transition-all duration-300 ${
        sidebarOpen ? 'w-80' : 'w-0'
      }`}>
        <div className="p-4 space-y-4 min-w-[320px]">
          <RouteSearchPanel />

          {data && (
            <OfflineModeToggle
              dataSource={data.dataSource}
              warning={data.warning}
              cacheAgeSeconds={data.cacheAgeSeconds}
            />
          )}

          {data?.alerts && data.alerts.length > 0 && (
            <AlertBanner alerts={data.alerts} />
          )}

          {data && (
            <div>
              <div className="flex items-center justify-between mb-3">
                <h2 className="text-sm font-semibold text-gray-700">
                  Vehicles ({data.vehicles.length})
                </h2>
                <button onClick={refetch} className="text-xs text-brand-600 hover:text-brand-800">
                  ↺ Refresh
                </button>
              </div>
              <VehicleList
                vehicles={data.vehicles}
                selectedId={selectedVehicle?.vehicleId}
                onSelect={setSelectedVehicle}
              />
            </div>
          )}

          {loading && !data && (
            <div className="space-y-2">
              {[1, 2, 3].map((i) => (
                <div key={i} className="h-16 bg-gray-100 rounded-xl animate-pulse" />
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Map area */}
      <div className="flex-1 relative">
        {/* Toggle sidebar button */}
        <button
          onClick={() => setSidebarOpen(!sidebarOpen)}
          className="absolute top-4 left-4 z-[1000] bg-white shadow-md rounded-lg p-2 hover:bg-gray-50 transition-colors"
          title={sidebarOpen ? 'Hide sidebar' : 'Show sidebar'}
        >
          <svg className="w-5 h-5 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
              d={sidebarOpen ? 'M11 19l-7-7 7-7m8 14l-7-7 7-7' : 'M13 5l7 7-7 7M5 5l7 7-7 7'} />
          </svg>
        </button>

        <Suspense fallback={
          <div className="h-full bg-gray-100 animate-pulse flex items-center justify-center">
            <span className="text-gray-400">Loading map…</span>
          </div>
        }>
          <MapView
            vehicles={data?.vehicles ?? []}
            selectedVehicle={selectedVehicle}
            onVehicleClick={setSelectedVehicle}
          />
        </Suspense>
      </div>
    </div>
  );
}
