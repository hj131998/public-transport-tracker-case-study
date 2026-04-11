import React, { useState, Suspense, lazy } from 'react';
import RouteSearchPanel from '../components/RouteSearchPanel/RouteSearchPanel';
import AlertBanner from '../components/AlertBanner/AlertBanner';
import VehicleList from '../components/VehicleList/VehicleList';
import OfflineModeToggle from '../components/OfflineModeToggle/OfflineModeToggle';
import RouteAlternatives from '../components/RouteAlternatives/RouteAlternatives';
import { useTransitData } from '../hooks/useTransitData';
import { VehiclePosition } from '../types/transit.types';

// Lazy-load heavy map component
const MapView = lazy(() => import('../components/MapView/MapView'));

function SkeletonCard() {
  return (
    <div className="bg-white rounded-2xl border border-gray-100 p-6 animate-pulse">
      <div className="h-4 bg-gray-200 rounded w-1/3 mb-4" />
      <div className="space-y-3">
        {[1, 2, 3].map((i) => (
          <div key={i} className="h-16 bg-gray-100 rounded-xl" />
        ))}
      </div>
    </div>
  );
}

export default function DashboardPage() {
  const { data, loading, error, refetch } = useTransitData();
  const [selectedVehicle, setSelectedVehicle] = useState<VehiclePosition | null>(null);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 space-y-6">

      {/* Page title */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
        <p className="text-sm text-gray-500 mt-0.5">Real-time public transport tracking</p>
      </div>

      {/* Search */}
      <RouteSearchPanel />

      {/* Status bar */}
      {data && (
        <OfflineModeToggle
          dataSource={data.dataSource}
          warning={data.warning}
          cacheAgeSeconds={data.cacheAgeSeconds}
        />
      )}

      {/* Error state */}
      {error && (
        <div className="bg-red-50 border border-red-200 rounded-xl p-4 flex items-center gap-3">
          <span className="text-red-500 text-xl">⚠</span>
          <div className="flex-1">
            <p className="text-sm font-medium text-red-800">{error}</p>
          </div>
          <button
            onClick={refetch}
            className="text-sm text-red-600 hover:text-red-800 font-medium underline"
          >
            Retry
          </button>
        </div>
      )}

      {/* Alerts */}
      {data && data.alerts.length > 0 && <AlertBanner alerts={data.alerts} />}

      {/* Main content grid */}
      {loading && !data ? (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <SkeletonCard />
          <div className="lg:col-span-2"><SkeletonCard /></div>
        </div>
      ) : data ? (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

          {/* Left column: vehicle list + routes */}
          <div className="space-y-6">
            <div className="bg-white rounded-2xl border border-gray-100 p-6">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-base font-semibold text-gray-800">
                  Vehicles
                  <span className="ml-2 text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full">
                    {data.vehicles.length}
                  </span>
                </h2>
                <button
                  onClick={refetch}
                  className="text-xs text-brand-600 hover:text-brand-800 font-medium"
                >
                  ↺ Refresh
                </button>
              </div>
              <VehicleList
                vehicles={data.vehicles}
                selectedId={selectedVehicle?.vehicleId}
                onSelect={setSelectedVehicle}
              />
            </div>

            {data.routePlan && (
              <div className="bg-white rounded-2xl border border-gray-100 p-6">
                <RouteAlternatives routePlan={data.routePlan} />
              </div>
            )}
          </div>

          {/* Right column: map */}
          <div className="lg:col-span-2">
            <Suspense fallback={
              <div className="rounded-2xl bg-gray-100 animate-pulse h-[500px] flex items-center justify-center">
                <span className="text-gray-400 text-sm">Loading map…</span>
              </div>
            }>
              <div className="h-[500px]">
                <MapView
                  vehicles={data.vehicles}
                  selectedVehicle={selectedVehicle}
                  onVehicleClick={setSelectedVehicle}
                />
              </div>
            </Suspense>
          </div>
        </div>
      ) : (
        /* Empty state */
        <div className="text-center py-20 text-gray-400">
          <span className="text-6xl block mb-4">🚌</span>
          <p className="text-lg font-medium text-gray-600">Search for a route to get started</p>
          <p className="text-sm mt-1">Enter a city and route ID above to see live vehicle positions</p>
        </div>
      )}
    </div>
  );
}
