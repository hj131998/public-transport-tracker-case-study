import { useState, FormEvent } from 'react';
import { useTransitContext } from '../../context/TransitContext';
import { TransitMode } from '../../types/transit.types';

const CITIES = ['NYC', 'CHICAGO', 'PORTLAND', 'SF', 'BOSTON'];

const POPULAR_ROUTES: Record<string, Record<TransitMode, string[]>> = {
  NYC: {
    BUS: ['M15', 'M1', 'B63', 'Q58', 'Bx12'],
    SUBWAY: ['L', 'A', 'N', '1', '7'],
  },
  CHICAGO: {
    BUS: ['22', '36', '77', '151', '156'],
    SUBWAY: [],
  },
  PORTLAND: {
    BUS: ['72', '4', '14', '20', '33'],
    SUBWAY: [],
  },
  SF: {
    BUS: ['14', '38', '49', '1', 'N'],
    SUBWAY: [],
  },
  BOSTON: {
    BUS: ['1', '57', '66', '77', '28'],
    SUBWAY: [],
  },
};

export default function RouteSearchPanel() {
  const { state, dispatch } = useTransitContext();
  const [city, setCity] = useState(state.query?.city ?? 'NYC');
  const [route, setRoute] = useState(state.query?.route ?? '');
  const [mode, setMode] = useState<TransitMode>(state.query?.mode ?? 'BUS');

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!city.trim() || !route.trim()) return;
    dispatch({ type: 'SET_QUERY', payload: { city: city.trim().toUpperCase(), route: route.trim(), mode } });
  };

  const handleQuickRoute = (r: string) => {
    setRoute(r);
    dispatch({ type: 'SET_QUERY', payload: { city, route: r, mode } });
  };

  const handleModeChange = (newMode: TransitMode) => {
    setMode(newMode);
    setRoute('');
  };

  const popularRoutes = POPULAR_ROUTES[city]?.[mode] ?? [];

  return (
    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
      <h2 className="text-lg font-semibold text-gray-800 mb-4 flex items-center gap-2">
        <span>🔍</span> Search Route
      </h2>

      <form onSubmit={handleSubmit} className="space-y-4">
        {/* Mode Toggle */}
        <div>
          <label className="block text-xs font-medium text-gray-500 mb-2">Vehicle Type</label>
          <div className="flex rounded-lg border border-gray-200 overflow-hidden">
            <button
              type="button"
              onClick={() => handleModeChange('BUS')}
              className={`flex-1 py-2 px-4 text-sm font-medium transition-colors ${
                mode === 'BUS'
                  ? 'bg-brand-600 text-white'
                  : 'bg-gray-50 text-gray-600 hover:bg-gray-100'
              }`}
            >
              🚌 Bus
            </button>
            <button
              type="button"
              onClick={() => handleModeChange('SUBWAY')}
              className={`flex-1 py-2 px-4 text-sm font-medium transition-colors ${
                mode === 'SUBWAY'
                  ? 'bg-brand-600 text-white'
                  : 'bg-gray-50 text-gray-600 hover:bg-gray-100'
              }`}
            >
              🚇 Subway
            </button>
          </div>
          {mode === 'SUBWAY' && (
            <p className="mt-1 text-xs text-amber-600">
              ⚠️ Subway shows delays and ETAs only — GPS positions not available from MTA
            </p>
          )}
        </div>

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
              placeholder={mode === 'BUS' ? 'e.g. M15' : 'e.g. L'}
              className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 bg-gray-50"
            />
          </div>
        </div>

        <button
          type="submit"
          disabled={!city || !route}
          className="w-full bg-brand-600 hover:bg-brand-700 disabled:bg-gray-300 text-white font-semibold py-2.5 rounded-lg transition-colors text-sm"
        >
          Track {mode === 'BUS' ? 'Bus' : 'Subway'} Route
        </button>
      </form>

      {/* Quick routes */}
      {popularRoutes.length > 0 && (
        <div className="mt-4">
          <p className="text-xs text-gray-400 mb-2">
            Popular {mode === 'BUS' ? 'bus' : 'subway'} routes in {city}
          </p>
          <div className="flex flex-wrap gap-2">
            {popularRoutes.map((r) => (
              <button
                key={r}
                onClick={() => handleQuickRoute(r)}
                className={`px-3 py-1 rounded-full text-xs font-medium border transition-colors ${
                  state.query?.route === r && state.query?.city === city && state.query?.mode === mode
                    ? 'bg-brand-600 text-white border-brand-600'
                    : 'border-gray-200 text-gray-600 hover:border-brand-400 hover:text-brand-600'
                }`}
              >
                {r}
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
