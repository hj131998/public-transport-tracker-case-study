import React, { Suspense, lazy } from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { TransitProvider } from './context/TransitContext';
import Header from './components/Layout/Header';

// Lazy-load pages for code splitting
const DashboardPage    = lazy(() => import('./pages/DashboardPage'));
const MapPage          = lazy(() => import('./pages/MapPage'));
const RoutePlannerPage = lazy(() => import('./pages/RoutePlannerPage'));
const NotificationsPage = lazy(() => import('./pages/NotificationsPage'));
const NotFoundPage     = lazy(() => import('./pages/NotFoundPage'));

function PageLoader() {
  return (
    <div className="flex items-center justify-center min-h-[60vh]">
      <div className="flex flex-col items-center gap-3 text-gray-400">
        <div className="w-8 h-8 border-4 border-brand-200 border-t-brand-600 rounded-full animate-spin" />
        <span className="text-sm">Loading…</span>
      </div>
    </div>
  );
}

// Error boundary for graceful UI degradation
class ErrorBoundary extends React.Component<
  { children: React.ReactNode },
  { hasError: boolean; message: string }
> {
  constructor(props: { children: React.ReactNode }) {
    super(props);
    this.state = { hasError: false, message: '' };
  }

  static getDerivedStateFromError(error: Error) {
    return { hasError: true, message: error.message };
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="flex flex-col items-center justify-center min-h-[60vh] text-center px-4">
          <span className="text-5xl mb-4">⚠️</span>
          <h2 className="text-xl font-bold text-gray-800 mb-2">Something went wrong</h2>
          <p className="text-sm text-gray-500 mb-6">{this.state.message}</p>
          <button
            onClick={() => this.setState({ hasError: false, message: '' })}
            className="btn-primary"
          >
            Try again
          </button>
        </div>
      );
    }
    return this.props.children;
  }
}

export default function App() {
  return (
    <TransitProvider>
      <BrowserRouter>
        <div className="min-h-screen flex flex-col">
          <Header />
          <main className="flex-1">
            <ErrorBoundary>
              <Suspense fallback={<PageLoader />}>
                <Routes>
                  <Route path="/"              element={<DashboardPage />} />
                  <Route path="/map"           element={<MapPage />} />
                  <Route path="/routes"        element={<RoutePlannerPage />} />
                  <Route path="/notifications" element={<NotificationsPage />} />
                  <Route path="*"              element={<NotFoundPage />} />
                </Routes>
              </Suspense>
            </ErrorBoundary>
          </main>
        </div>
      </BrowserRouter>
    </TransitProvider>
  );
}
