import React, { useState, FormEvent } from 'react';
import { useTransitContext } from '../../context/TransitContext';

const CITIES = ['NYC', 'LON', 'CHI', 'LAX', 'SFO'];
const POPULAR_ROUTES: Record<string, string[]> = {
  NYC: ['M15', 'M1', 'B63', 'Q58', 'Bx12'],
  LON: ['15', '25', '38', '73', '149'],
  CHI: ['22', '36', '77', '151', '156'],
  LAX: ['2', '4', '20', '720', '733'],
  SFO: ['1', '14', '38', '49', 'N'],
};

export default function RouteSearchPanel() {
  const { state, dispatch } = useTransitContext();
  const [city, setCity] = useState(state.query?.city ?? 'NYC');
  const [route, setRoute] = useState(state.query?.route ?? '');

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!city.trim() || !route.trim()) return;
    dispatch({ type: 'SET_QUERY', payload: { city: city.trim().toUpperCase(), route: route.trim() } });
  };

  const handleQuickRoute = (r: string) => {
    setRoute(r);
    dispatch({ type: 'SET_QUERY', payload: { city, route: r } });
  };

  return (
    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
      <h2 className="text-lg font-semibold text-gray-800 mb-4 flex items-center gap-2">
        <span>🔍</span> Search Route
      </h2>

      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="grid grid-cols-2 gap-3">
          {/* City */}
          <div>
            <label className="block text-xs font-medium text-gray-500 mb-1">City</label>
            <select
              value={city}
              onChange={(e) => { setCity(e.target.value); setRoute(''); }}
              className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 bg-gray-50"
            >
              {CITIES.map((c) => <option key={c} value={c}>{c}</option>)}
            </select>
          </div>

          {/* Route */}
          <div>
            <label className="block text-xs font-medium text-gray-500 mb-1">Route ID</label>
            <input
              type="text"
              value={route}
              onChange={(e) => setRoute(e.target.value)}
              placeholder="e.g. M15"
              className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 bg-gray-50"
            />
          </div>
        </div>

        <button
          type="submit"
          disabled={!city || !route}
          className="w-full bg-brand-600 hover:bg-brand-700 disabled:bg-gray-300 text-white font-semibold py-2.5 rounded-lg transition-colors text-sm"
        >
          Track Route
        </button>
      </form>

      {/* Quick routes */}
      <div className="mt-4">
        <p className="text-xs text-gray-400 mb-2">Popular routes in {city}</p>
        <div className="flex flex-wrap gap-2">
          {(POPULAR_ROUTES[city] ?? []).map((r) => (
            <button
              key={r}
              onClick={() => handleQuickRoute(r)}
              className={`px-3 py-1 rounded-full text-xs font-medium border transition-colors ${
                state.query?.route === r && state.query?.city === city
                  ? 'bg-brand-600 text-white border-brand-600'
                  : 'border-gray-200 text-gray-600 hover:border-brand-400 hover:text-brand-600'
              }`}
            >
              {r}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
