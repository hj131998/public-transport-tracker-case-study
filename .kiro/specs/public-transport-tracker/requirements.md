# Requirements Document

## Introduction

The Public Transport Tracker is a full-stack application that displays real-time NYC subway vehicle positions, alerts, and route plans. The system uses a BFF (Backend for Frontend) architecture where a React SPA communicates with a Spring Boot gateway that fetches GTFS-Realtime protobuf data from the MTA free API. The application implements graceful degradation (LIVE → STALE → MOCK), a circuit breaker on external calls, in-memory caching with TTL, and conditional alert generation based on delay, disruption, crowding, and weather rules.

## Glossary

- **BFF**: Backend for Frontend — a Spring Boot gateway service that aggregates data for the React frontend
- **Transit_Controller**: The REST controller exposing API endpoints at /api/v1
- **Transit_Data_Service**: The service implementing the LIVE → STALE → MOCK fallback chain
- **Alert_Service**: The service evaluating business rules to generate conditional alerts
- **Route_Planner_Service**: The service calculating primary and alternative routes with ETAs
- **Cache_Service**: The in-memory LRU cache with 30s fresh TTL and 300s stale TTL
- **MTA_API_Client**: The WebClient-based HTTP client fetching GTFS-RT protobuf feeds from MTA
- **GTFS_Parser**: The protobuf decoder converting binary MTA feed data into domain VehiclePosition objects
- **Frontend_App**: The React SPA with Leaflet maps, context state, and auto-refresh polling
- **Circuit_Breaker**: Resilience4j circuit breaker protecting external API calls
- **DataSource**: An enum indicating data origin — LIVE, CACHED, STALE, or MOCK
- **VehiclePosition**: A domain object with vehicleId, lat, lon, nextStop, eta, crowding, delayMinutes, and disrupted flag
- **Alert**: A domain object with type (DELAY, DISRUPTION, CROWDING, WEATHER), severity, and message

## Requirements

### Requirement 1: Backend API Endpoints

**User Story:** As a frontend developer, I want the BFF to expose well-structured REST endpoints, so that the React app can fetch transit data, alerts, and route plans reliably.

#### Acceptance Criteria

1. WHEN a GET request is made to /api/v1/transit with valid city and route parameters, THE Transit_Controller SHALL return a JSON response containing routeId, city, dataSource, offline flag, vehicles list, alerts list, routePlan, and HATEOAS links
2. WHEN a GET request is made to /api/v1/transit/{routeId}/vehicles with valid city parameter, THE Transit_Controller SHALL return a JSON array of VehiclePosition objects
3. WHEN a GET request is made to /api/v1/transit/{routeId}/alerts with valid city parameter, THE Transit_Controller SHALL return a JSON array of Alert objects
4. WHEN a GET request is made to /api/v1/routes/plan with valid city, from, and to parameters, THE Transit_Controller SHALL return a JSON RoutePlan object with primaryRoute, alternatives, and estimatedMinutes
5. WHEN a GET request is made to /api/v1/routes/{routeId}/alternatives with valid city parameter, THE Transit_Controller SHALL return a JSON RoutePlan object with alternative routes
6. WHEN a request is missing required parameters, THE Transit_Controller SHALL return HTTP 400 with a descriptive error message

### Requirement 2: Graceful Degradation Chain

**User Story:** As a user, I want the application to always display transit data even when external APIs are unavailable, so that I can still plan my commute during outages.

#### Acceptance Criteria

1. WHILE the data source mode is LIVE and the cache contains fresh data (age less than 30 seconds), THE Transit_Data_Service SHALL return cached data with DataSource set to CACHED
2. WHILE the data source mode is LIVE and the cache is empty, THE Transit_Data_Service SHALL call the MTA_API_Client and cache the result with DataSource set to LIVE
3. IF the MTA API call fails and stale cache exists (age less than 300 seconds), THEN THE Transit_Data_Service SHALL return stale cached data with DataSource set to STALE and include a warning message
4. IF the MTA API call fails and no cache exists, THEN THE Transit_Data_Service SHALL return mock data with DataSource set to MOCK and offline flag set to true
5. WHILE the data source mode is MOCK, THE Transit_Data_Service SHALL return mock data without calling external APIs
6. WHILE the data source mode is CACHED, THE Transit_Data_Service SHALL serve only from fresh cache and fall back to mock if cache is empty

### Requirement 3: In-Memory Cache

**User Story:** As a system operator, I want transit data to be cached with configurable TTL, so that repeated requests are fast and external API load is minimized.

#### Acceptance Criteria

1. WHEN a new value is cached, THE Cache_Service SHALL store it with a timestamp and evict the least-recently-used entry if the store exceeds 1000 entries
2. WHEN a cached entry is younger than 30 seconds, THE Cache_Service SHALL return it as fresh
3. WHEN a cached entry is older than 30 seconds but younger than 300 seconds, THE Cache_Service SHALL return it only via the getStale method
4. WHEN a cached entry is older than 300 seconds, THE Cache_Service SHALL treat it as expired and not return it from any method
5. THE Cache_Service SHALL use a ReadWriteLock to ensure thread-safe concurrent access

### Requirement 4: Circuit Breaker and Retry

**User Story:** As a system operator, I want external API calls protected by a circuit breaker, so that cascading failures are prevented and the system remains responsive during upstream outages.

#### Acceptance Criteria

1. WHEN the MTA API failure rate exceeds 50 percent over a sliding window of 10 calls, THE Circuit_Breaker SHALL transition to OPEN state and reject subsequent calls for 30 seconds
2. WHILE the Circuit_Breaker is in OPEN state, THE MTA_API_Client SHALL not make outbound HTTP requests and the fallback chain SHALL be invoked
3. WHEN the wait duration expires, THE Circuit_Breaker SHALL transition to HALF_OPEN and permit 2 probe requests
4. WHEN the MTA API call fails due to an IOException or WebClientRequestException, THE MTA_API_Client SHALL retry up to 3 times with 500ms wait between attempts

### Requirement 5: Alert Generation

**User Story:** As a commuter, I want to see relevant alerts about delays, disruptions, crowding, and weather, so that I can make informed travel decisions.

#### Acceptance Criteria

1. WHEN any vehicle on a route has a delay exceeding 15 minutes, THE Alert_Service SHALL generate a DELAY alert with HIGH severity and message "Significant delays - Plan accordingly"
2. WHEN any vehicle on a route has the disrupted flag set to true, THE Alert_Service SHALL generate a DISRUPTION alert with HIGH severity and message "Service alert - Check alternative routes"
3. WHEN any vehicle on a route has crowding level HIGH, THE Alert_Service SHALL generate a CROWDING alert with MEDIUM severity and message "Vehicle at capacity - Consider next service"
4. WHEN a weather impact flag is provided as true, THE Alert_Service SHALL generate a WEATHER alert with MEDIUM severity and message "Weather impact on schedule"
5. WHEN no alert conditions are met, THE Alert_Service SHALL return an empty alert list

### Requirement 6: MTA GTFS-RT Data Fetching

**User Story:** As a user, I want real-time subway vehicle positions from the MTA API, so that I can see where trains are right now.

#### Acceptance Criteria

1. WHEN fetching vehicle data for a subway route, THE MTA_API_Client SHALL resolve the correct feed URL from the MtaFeedRegistry and make a GET request to the MTA GTFS-RT endpoint
2. WHEN the MTA API returns a protobuf response, THE GTFS_Parser SHALL decode the binary data into a list of VehiclePosition objects filtered by the requested routeId
3. IF the requested route is not a recognized subway route, THEN THE MTA_API_Client SHALL return an empty list without making an API call
4. IF the MTA API returns an HTTP error, THEN THE MTA_API_Client SHALL throw a RuntimeException that triggers the fallback chain
5. THE MTA_API_Client SHALL set a request timeout of 10 seconds on external HTTP calls

### Requirement 7: Frontend-Backend Integration

**User Story:** As a user, I want the frontend to seamlessly communicate with the backend, so that transit data loads correctly in the browser.

#### Acceptance Criteria

1. WHEN the frontend makes API requests during development, THE Vite dev server SHALL proxy requests matching /api to localhost:8080
2. THE Frontend_App SHALL use axios with a base URL of /api/v1 and a 10-second timeout for all API calls
3. WHEN an API response is received, THE Frontend_App SHALL map the JSON response to TypeScript types matching VehiclePosition, Alert, RoutePlan, and TransitResponse interfaces
4. WHEN an API call fails, THE Frontend_App SHALL normalize the error into a descriptive message string and propagate it to the error state
5. THE Frontend_App SHALL attach a unique X-Trace-Id header to every outgoing request

### Requirement 8: Frontend State Management and Auto-Refresh

**User Story:** As a user, I want the transit dashboard to automatically refresh data every 30 seconds, so that I see up-to-date vehicle positions without manual intervention.

#### Acceptance Criteria

1. THE Frontend_App SHALL use React Context with useReducer to manage transit query, data, loading, error, offline mode, and notifications state
2. WHEN a transit query is active, THE Frontend_App SHALL poll the /api/v1/transit endpoint every 30 seconds
3. WHEN the component unmounts or the query changes, THE Frontend_App SHALL cancel the active polling interval to prevent memory leaks
4. WHEN new data arrives with alerts, THE Frontend_App SHALL add those alerts to the notification list capped at 20 entries
5. WHEN offline mode is toggled, THE Frontend_App SHALL include an offline parameter in subsequent API requests

### Requirement 9: Map Visualization

**User Story:** As a user, I want to see vehicle positions on an interactive map, so that I can visually track subway train locations.

#### Acceptance Criteria

1. WHEN vehicle data is available, THE Frontend_App SHALL render a Leaflet map centered on NYC with markers for each vehicle position at the correct lat/lon coordinates
2. WHEN a vehicle marker is clicked, THE Frontend_App SHALL display a popup with vehicle details including nextStop, eta, crowding level, and delay
3. WHEN new data arrives via auto-refresh, THE Frontend_App SHALL update marker positions on the map without a full page reload

### Requirement 10: Route Planning

**User Story:** As a commuter, I want to plan routes between stops and see alternatives, so that I can choose the best path for my journey.

#### Acceptance Criteria

1. WHEN a user provides origin and destination stops, THE Route_Planner_Service SHALL return a primary route with stops, duration, and disruption flag
2. THE Route_Planner_Service SHALL return up to 3 alternative routes alongside the primary route
3. WHEN vehicle data indicates delays on a route segment, THE Route_Planner_Service SHALL factor delay information into estimated journey times

### Requirement 11: Error Handling and Resilience UI

**User Story:** As a user, I want clear error messages and graceful recovery when something goes wrong, so that the application remains usable even during failures.

#### Acceptance Criteria

1. WHEN a React component throws an error during rendering, THE Frontend_App SHALL catch it with an Error Boundary and display a "Something went wrong" message with a retry button
2. WHEN the backend returns an error, THE Frontend_App SHALL display a user-friendly error state with the error message and allow the user to retry
3. WHEN data is served from STALE or MOCK sources, THE Frontend_App SHALL display a banner indicating the data source and any warning message

### Requirement 12: Build and Deployment

**User Story:** As a developer, I want both frontend and backend to build and run successfully in development and Docker environments, so that the application is deployable.

#### Acceptance Criteria

1. WHEN the Maven build is executed, THE backend project SHALL compile without errors and produce a runnable JAR
2. WHEN npm run build is executed, THE frontend project SHALL compile without errors and produce a dist folder with optimized assets
3. WHEN docker-compose up is executed, THE deployment SHALL start both frontend and backend containers and make the application accessible
4. THE backend container SHALL expose port 8080 and the frontend container SHALL expose port 3000

### Requirement 13: CORS Configuration

**User Story:** As a developer, I want CORS properly configured, so that the frontend on port 3000 can call the backend on port 8080 without browser security errors.

#### Acceptance Criteria

1. THE BFF SHALL allow CORS requests from the origin http://localhost:3000
2. WHEN a cross-origin request arrives from an allowed origin, THE BFF SHALL include the correct Access-Control-Allow-Origin, Access-Control-Allow-Methods, and Access-Control-Allow-Headers response headers
3. IF a cross-origin request arrives from a disallowed origin, THEN THE BFF SHALL reject it without processing the request body

### Requirement 14: Testing

**User Story:** As a developer, I want comprehensive test coverage, so that I can refactor and extend the application with confidence.

#### Acceptance Criteria

1. WHEN JUnit tests are executed, THE backend test suite SHALL pass with all unit tests covering service logic, cache behavior, and alert generation
2. WHEN Vitest tests are executed, THE frontend test suite SHALL pass with tests covering component rendering, context state transitions, and API service mocking
3. THE test suites SHALL validate the graceful degradation chain by testing LIVE, STALE, and MOCK transitions
