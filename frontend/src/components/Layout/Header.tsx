import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useOfflineMode } from '../../hooks/useOfflineMode';
import { useNotifications } from '../../hooks/useNotifications';

export default function Header() {
  const { offlineMode, toggle } = useOfflineMode();
  const { unreadCount } = useNotifications();
  const location = useLocation();

  const navLink = (to: string, label: string) => (
    <Link
      to={to}
      className={`px-3 py-2 rounded-md text-sm font-medium transition-colors ${
        location.pathname === to
          ? 'bg-brand-700 text-white'
          : 'text-blue-100 hover:bg-brand-600 hover:text-white'
      }`}
    >
      {label}
    </Link>
  );

  return (
    <header className="bg-brand-900 shadow-lg">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <Link to="/" className="flex items-center gap-2">
            <span className="text-2xl">🚌</span>
            <span className="text-white font-bold text-lg tracking-tight">
              Transport Tracker
            </span>
          </Link>

          {/* Nav */}
          <nav className="hidden md:flex items-center gap-1">
            {navLink('/', 'Dashboard')}
            {navLink('/map', 'Live Map')}
            {navLink('/routes', 'Route Planner')}
          </nav>

          {/* Controls */}
          <div className="flex items-center gap-3">
            {/* Offline toggle */}
            <button
              onClick={toggle}
              title={offlineMode ? 'Switch to Live Mode' : 'Switch to Offline Mode'}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-semibold transition-all ${
                offlineMode
                  ? 'bg-gray-500 text-white'
                  : 'bg-green-500 text-white hover:bg-green-600'
              }`}
            >
              <span className={`w-2 h-2 rounded-full ${offlineMode ? 'bg-gray-300' : 'bg-white animate-pulse'}`} />
              {offlineMode ? 'Offline' : 'Live'}
            </button>

            {/* Notification bell */}
            <Link to="/notifications" className="relative p-2 text-blue-200 hover:text-white transition-colors">
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                  d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
              </svg>
              {unreadCount > 0 && (
                <span className="absolute top-1 right-1 w-4 h-4 bg-red-500 text-white text-xs rounded-full flex items-center justify-center font-bold">
                  {unreadCount > 9 ? '9+' : unreadCount}
                </span>
              )}
            </Link>
          </div>
        </div>
      </div>
    </header>
  );
}
