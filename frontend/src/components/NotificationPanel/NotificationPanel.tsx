import React from 'react';
import { useNotifications } from '../../hooks/useNotifications';
import { alertIcon, alertBg } from '../../utils/formatters';
import { formatDistanceToNow } from 'date-fns';

export default function NotificationPanel() {
  const { notifications, unreadCount, markRead, clearAll } = useNotifications();

  return (
    <div className="max-w-lg mx-auto">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Notifications</h1>
          {unreadCount > 0 && (
            <p className="text-sm text-gray-500 mt-0.5">{unreadCount} unread</p>
          )}
        </div>
        {notifications.length > 0 && (
          <button
            onClick={clearAll}
            className="text-sm text-gray-400 hover:text-red-500 transition-colors"
          >
            Clear all
          </button>
        )}
      </div>

      {notifications.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          <span className="text-5xl block mb-3">🔔</span>
          <p className="text-sm">No notifications yet</p>
          <p className="text-xs mt-1">Alerts will appear here when you track a route</p>
        </div>
      ) : (
        <div className="space-y-2">
          {notifications.map((n) => (
            <button
              key={n.id}
              onClick={() => markRead(n.id)}
              className={`w-full text-left p-4 rounded-xl border-l-4 transition-all ${alertBg(n.type)} ${
                !n.read ? 'opacity-100' : 'opacity-60'
              }`}
            >
              <div className="flex items-start gap-3">
                <span className="text-xl flex-shrink-0">{alertIcon(n.type)}</span>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-gray-800">{n.message}</p>
                  <p className="text-xs text-gray-400 mt-0.5">
                    {formatDistanceToNow(n.timestamp, { addSuffix: true })}
                  </p>
                </div>
                {!n.read && (
                  <span className="w-2 h-2 bg-brand-500 rounded-full flex-shrink-0 mt-1.5" />
                )}
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
