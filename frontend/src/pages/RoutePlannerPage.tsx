import React, { useState, FormEvent } from 'react';
import { transitApi } from '../services/apiService';
import { RoutePlan } from '../types/transit.types';
import RouteAlternatives from '../components/RouteAlternatives/RouteAlternatives';

const CITIES = ['NYC', 'LON', 'CHI', 'LAX', 'SFO'];

export default function RoutePlannerPage() {
  const [city, setCity] = useState('NYC');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [plan, setPlan] = useState<RoutePlan | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!from.trim() || !to.trim()) return;
    setLoading(true);
    setError(null);
    try {
      const result = await transitApi.planRoute(city, from.trim(), to.trim());
      setPlan(result);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  };

  const swap = () => { setFrom(to); setTo(from); setPlan(null); };

  return (
    <div className="max-w-2xl mx-auto px-4 sm:px-6 py-8 space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Route Planner</h1>
        <p className="text-sm text-gray-500 mt-0.5">Find the best route between two stops</p>
      </div>

      {/* Form */}
      <div className="bg-white rounded-2xl border border-gray-100 p-6 shadow-sm">
        <form onSubmit={handleSubmit} className="space-y-4">
          {/* City */}
          <div>
            <label className="block text-xs font-medium text-gray-500 mb-1">City</label>
            <select
              value={city}
              onChange={(e) => setCity(e.target.value)}
              className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 bg-gray-50"
            >
              {CITIES.map((c) => <option key={c}>{c}</option>)}
            </select>
          </div>

          {/* From / To with swap */}
          <div className="relative space-y-3">
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">From</label>
              <div className="relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-green-500">●</span>
                <input
                  value={from}
                  onChange={(e) => setFrom(e.target.value)}
                  placeholder="Origin stop or area"
                  className="w-full border border-gray-200 rounded-lg pl-8 pr-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 bg-gray-50"
                />
              </div>
            </div>

            {/* Swap button */}
            <button
              type="button"
              onClick={swap}
              className="absolute right-0 top-1/2 -translate-y-1/2 -translate-x-1 bg-white border border-gray-200 rounded-full p-1.5 hover:bg-gray-50 shadow-sm transition-colors"
              title="Swap origin and destination"
            >
              <svg className="w-4 h-4 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 16V4m0 0L3 8m4-4l4 4m6 0v12m0 0l4-4m-4 4l-4-4" />
              </svg>
            </button>

            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">To</label>
              <div className="relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-red-500">●</span>
                <input
                  value={to}
                  onChange={(e) => setTo(e.target.value)}
                  placeholder="Destination stop or area"
                  className="w-full border border-gray-200 rounded-lg pl-8 pr-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500 bg-gray-50"
                />
              </div>
            </div>
          </div>

          <button
            type="submit"
            disabled={!from || !to || loading}
            className="w-full bg-brand-600 hover:bg-brand-700 disabled:bg-gray-300 text-white font-semibold py-2.5 rounded-lg transition-colors text-sm"
          >
            {loading ? 'Planning…' : 'Plan Route'}
          </button>
        </form>
      </div>

      {/* Error */}
      {error && (
        <div className="bg-red-50 border border-red-200 rounded-xl p-4 text-sm text-red-700">
          ⚠ {error}
        </div>
      )}

      {/* Results */}
      {plan && (
        <div className="bg-white rounded-2xl border border-gray-100 p-6 shadow-sm animate-fade-in">
          <RouteAlternatives routePlan={plan} />
        </div>
      )}
    </div>
  );
}
