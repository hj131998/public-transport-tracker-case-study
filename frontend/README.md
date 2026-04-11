# Public Transport Tracker — Frontend

React 18 + TypeScript + Tailwind CSS SPA. Real-time transit tracking with live map, vehicle list, alerts, and route planning.

## Quick Start

### Prerequisites
- Node.js 20+
- npm 10+
- Backend running on http://localhost:8080

### Install and run

```bash
cd frontend
cp .env.template .env.local
npm install
npm run dev
```

App starts on http://localhost:3000

### Run tests

```bash
npm test
```

### Build for production

```bash
npm run build
npm run preview   # preview the production build locally
```

## Pages

| Route | Description |
|---|---|
| `/` | Dashboard — search, alerts, vehicle list, map |
| `/map` | Full-screen live map with sidebar |
| `/routes` | Route planner — origin to destination |
| `/notifications` | All service alerts and notifications |

## Architecture

- State: React Context + useReducer (no Redux)
- Data fetching: Custom hooks (`useTransitData`) with 30s auto-refresh
- Map: Leaflet + react-leaflet
- Styling: Tailwind CSS utility classes
- Routing: React Router v6
- HTTP: Axios with request interceptors (trace ID injection)
- Code splitting: React.lazy + Suspense on all pages and MapView
- Error handling: ErrorBoundary component wraps all routes

## Key Features

- Live vehicle positions on interactive map
- Auto-refresh every 30 seconds
- Offline mode toggle — switches to mock/cached data
- Conditional alert banners (delay, disruption, crowding, weather)
- Route planning with primary + alternative routes
- In-app notification panel with unread count
- Data source badge (LIVE / CACHED / STALE / OFFLINE)
