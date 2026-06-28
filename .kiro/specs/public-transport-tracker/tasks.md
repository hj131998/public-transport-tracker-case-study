# Implementation Plan: Public Transport Tracker

## Overview

This plan focuses on fixing/completing the existing partially-implemented application. Tasks are ordered by build priority: compilation first, then integration, then tests, then deployment. The GtfsRealtimeParser and MtaFeedRegistry are confirmed working and must not be modified.

## Tasks

- [x] 1. Fix backend compilation
  - [x] 1.1 Verify pom.xml dependencies and resolve any missing imports
    - Run `mvn clean compile` in the `backend/transport-tracker-bff` directory
    - Fix any missing model classes (enums: DataSource, AlertType, Severity, CrowdingLevel)
    - Ensure Lombok annotation processing is configured correctly
    - Verify all Spring Boot starters resolve without version conflicts
    - _Requirements: 12.1_

  - [x] 1.2 Fix any missing or broken model/enum classes
    - Ensure `com.tracker.model.enums.DataSource` enum exists with values: LIVE, CACHED, STALE, MOCK
    - Ensure `com.tracker.model.enums.AlertType` enum exists with values: DELAY, DISRUPTION, CROWDING, WEATHER
    - Ensure `com.tracker.model.enums.Severity` enum exists with values: LOW, MEDIUM, HIGH
    - Ensure `com.tracker.model.enums.CrowdingLevel` enum exists with values: LOW, MEDIUM, HIGH
    - _Requirements: 1.1, 2.1_

  - [x] 1.3 Ensure TransitDataService compiles with correct provider interface
    - Verify `TransitDataProvider` interface has `supports(String city)` and `fetchVehicles(String city, String routeId)` methods
    - Verify `TransitLandClient` implements `TransitDataProvider` (or remove if unused)
    - Confirm `MockDataProvider` is a standalone `@Component` (not implementing TransitDataProvider)
    - _Requirements: 2.1, 2.2_

  - [x] 1.4 Ensure the Spring Boot application class exists and compiles
    - Verify `@SpringBootApplication` annotated main class exists
    - Verify `@EnableScheduling` is present (required for cache eviction)
    - Confirm `@EnableConfigurationProperties` binds AppConfig correctly
    - _Requirements: 12.1_

- [x] 2. Fix frontend compilation
  - [x] 2.1 Verify all TypeScript types align with backend JSON output
    - Confirm `transit.types.ts` field names match Jackson serialized field names from Java models
    - Ensure `cacheAgeSeconds` (not `cacheAge`) matches backend `TransitResponse.cacheAgeSeconds`
    - Ensure `delayMinutes` (not `delay`) matches backend `VehiclePosition.delayMinutes`
    - Ensure Alert.generatedAt is typed as `string` (ISO instant from backend)
    - _Requirements: 7.3_

  - [x] 2.2 Fix missing utility functions and imports
    - Ensure `src/utils/formatters.ts` exports: `crowdingLabel`, `crowdingColor`, `crowdingBg`, `formatDelay`, `alertBg`, `alertIcon`, `severityBadge`
    - Verify all component imports resolve correctly
    - Fix any unused variable/parameter TypeScript errors (strict mode enabled)
    - _Requirements: 12.2_

  - [x] 2.3 Ensure all page and component files compile without TypeScript errors
    - Run `npx tsc --noEmit` to identify type errors
    - Fix any missing props, incorrect types, or unused imports
    - Verify RouteSearchPanel, OfflineModeToggle, RouteAlternatives, NotificationPanel components exist and export correctly
    - _Requirements: 12.2_

  - [x] 2.4 Create frontend test setup file if missing
    - Create `src/test/setup.ts` with `@testing-library/jest-dom` imports
    - Ensure `vitest.config` or `vite.config.ts` test section references this setup file
    - _Requirements: 14.2_

  - [x] 2.5 Verify Vite build succeeds
    - Run `npm run build` (which runs `tsc && vite build`)
    - Fix any remaining TypeScript compilation issues
    - Verify output in `dist/` folder
    - _Requirements: 12.2_

- [x] 3. Checkpoint - Backend and frontend compile
  - Ensure `mvn clean compile` and `npm run build` both succeed, ask the user if questions arise.

- [x] 4. Frontend-backend integration
  - [x] 4.1 Verify Vite proxy configuration
    - Confirm `vite.config.ts` has proxy entry: `/api` → `http://localhost:8080`
    - Ensure `changeOrigin: true` is set on the proxy
    - Verify the proxy does not rewrite paths (backend expects `/api/v1/transit`)
    - _Requirements: 7.1_

  - [x] 4.2 Verify CORS configuration in WebConfig
    - Confirm `WebConfig.java` maps `/api/**` pattern
    - Confirm allowed origins includes `http://localhost:3000`
    - Confirm allowed methods include GET, POST, OPTIONS
    - Confirm `maxAge(3600)` is set for preflight caching
    - _Requirements: 13.1, 13.2, 13.3_

  - [x] 4.3 Verify TransitResponse JSON structure matches frontend types
    - Confirm Jackson serialization of `TransitResponse` produces keys matching TypeScript interface
    - Verify `@JsonInclude(NON_NULL)` causes null `warning` and `routePlan` to be omitted (matching frontend optional fields)
    - Verify enum serialization: DataSource serializes as "LIVE"/"CACHED"/"STALE"/"MOCK" (uppercase strings)
    - _Requirements: 7.3_

  - [x] 4.4 Verify MockDataProvider returns structurally correct data
    - Ensure `getMockVehicles()` returns VehiclePosition objects with all required fields populated
    - Ensure `getMockAlerts()` returns Alert objects with valid enum values
    - Ensure `getMockRoutePlan()` returns a RoutePlan with primaryRoute containing stops
    - _Requirements: 2.4, 2.5_

- [x] 5. Checkpoint - Integration verified
  - Ensure backend starts on port 8080 with `DATA_SOURCE_MODE=MOCK`, frontend proxy reaches backend, ask the user if questions arise.

- [x] 6. Backend unit tests
  - [x] 6.1 Write/fix AlertService unit tests
    - Test delay > 15 min generates DELAY alert with HIGH severity
    - Test disrupted vehicle generates DISRUPTION alert
    - Test HIGH crowding generates CROWDING alert
    - Test weather flag generates WEATHER alert
    - Test no conditions returns empty list
    - Test multiple conditions generate multiple alerts
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 14.1_

  - [x] 6.2 Write/fix InMemoryCacheService unit tests
    - Test put/get within TTL returns value
    - Test get after TTL returns empty
    - Test getStale between TTL and staleTTL returns value
    - Test getStale after staleTTL returns empty
    - Test LRU eviction when size exceeds maxSize
    - Test evict removes entry
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 14.1_

  - [x] 6.3 Write/fix TransitDataService unit tests
    - Mock CacheService, TransitDataProvider, MockDataProvider, AppConfig
    - Test LIVE mode with fresh cache hit → returns CACHED
    - Test LIVE mode with cache miss + API success → returns LIVE
    - Test LIVE mode with API failure + stale cache → returns STALE
    - Test LIVE mode with API failure + no cache → returns MOCK
    - Test MOCK mode → returns mock without calling provider
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 14.1, 14.3_

  - [x] 6.4 Write/fix TransitAggregatorService unit tests
    - Verify it orchestrates TransitDataService + AlertService + RoutePlannerService
    - Verify HATEOAS links are built correctly
    - Verify offline mode uses mock alerts
    - _Requirements: 1.1, 14.1_

  - [x] 6.5 Write/fix RoutePlannerService unit tests
    - Test plan returns non-null primaryRoute with stops
    - Test alternatives list has 2 entries normally, 3 when disrupted
    - Test estimatedMinutes increases with vehicle delays
    - _Requirements: 10.1, 10.2, 10.3, 14.1_

  - [ ]* 6.6 Write property tests for AlertService (jqwik)
    - Add jqwik dependency to pom.xml
    - **Property 4: Alert generation correctness**
    - Generate random lists of VehiclePosition with varying delay, crowding, disrupted values
    - Assert DELAY alert present ↔ max delay > 15
    - Assert DISRUPTION alert present ↔ any disrupted == true
    - Assert CROWDING alert present ↔ any crowding == HIGH
    - Assert empty alerts when no conditions met
    - **Validates: Requirements 5.1, 5.2, 5.3, 5.5**

  - [ ]* 6.7 Write property tests for InMemoryCacheService (jqwik)
    - **Property 2: Cache TTL window behavior**
    - Generate random values and test get/getStale behavior at various simulated ages
    - **Property 3: Cache size invariant**
    - Generate N > maxSize random put operations, assert size() <= maxSize
    - **Validates: Requirements 3.1, 3.2, 3.3, 3.4**

  - [ ]* 6.8 Write property tests for RoutePlannerService (jqwik)
    - **Property 7: Route planner structure invariant**
    - Generate random from/to strings and vehicle lists
    - Assert primaryRoute non-null with >= 2 stops, alternatives.size() <= 3
    - **Property 8: Delay increases estimated journey time**
    - Generate vehicle lists with avg delay > 0, assert estimatedMinutes > 15
    - **Validates: Requirements 10.1, 10.2, 10.3**

- [x] 7. Frontend unit tests
  - [x] 7.1 Write/fix TransitContext reducer tests
    - Test FETCH_START sets loading=true, error=null
    - Test FETCH_SUCCESS sets data, loading=false
    - Test FETCH_ERROR sets error message, loading=false
    - Test ADD_NOTIFICATION adds notification, caps at 20
    - Test MARK_READ updates notification read flag
    - Test TOGGLE_OFFLINE flips offlineMode
    - _Requirements: 8.1, 8.4, 14.2_

  - [x] 7.2 Write/fix VehicleList component tests
    - Test renders vehicle cards with correct data
    - Test empty state message shown when no vehicles
    - Test sort buttons change order
    - _Requirements: 9.1, 14.2_

  - [x] 7.3 Write/fix AlertBanner component tests
    - Test renders nothing when alerts array is empty
    - Test renders alert messages with correct styling per type
    - _Requirements: 11.3, 14.2_

  - [x] 7.4 Write/fix formatters utility tests
    - Test crowdingLabel, formatDelay, alertBg, alertIcon, severityBadge
    - _Requirements: 14.2_

  - [ ]* 7.5 Write property test for notification cap (fast-check)
    - Add fast-check dependency to package.json
    - **Property 6: Notification list capped at 20**
    - Generate arbitrary sequences of ADD_NOTIFICATION actions
    - Assert notifications.length never exceeds 20
    - **Validates: Requirements 8.4**

- [x] 8. Checkpoint - All tests pass
  - Ensure `mvn test` and `npm run test` both pass, ask the user if questions arise.

- [x] 9. Docker and deployment configuration
  - [x] 9.1 Verify backend Dockerfile builds and runs
    - Confirm multi-stage build: Maven build stage → JRE runtime stage
    - Ensure the final image exposes port 8080
    - Ensure `DATA_SOURCE_MODE` can be passed as env var
    - _Requirements: 12.1, 12.3, 12.4_

  - [x] 9.2 Verify frontend Docker configuration or static serving
    - Ensure frontend builds into `dist/` with `npm run build`
    - Verify nginx config serves static files and proxies `/api` to backend
    - _Requirements: 12.2, 12.3, 12.4_

  - [x] 9.3 Verify docker-compose.yml wires services correctly
    - Confirm backend service exposes 8080
    - Confirm frontend/nginx service exposes 3000 (or 80)
    - Confirm frontend depends_on backend
    - Confirm environment variables are passed through
    - _Requirements: 12.3, 12.4_

- [x] 10. End-to-end smoke test
  - [x] 10.1 Verify backend starts and serves mock data
    - Start backend with `DATA_SOURCE_MODE=MOCK`
    - Curl `GET /api/v1/transit?city=NYC&route=L` and verify JSON response with vehicles and alerts
    - Verify response contains all required fields: routeId, city, dataSource, vehicles, alerts, links
    - _Requirements: 1.1, 2.5, 12.1_

  - [x] 10.2 Verify frontend dev server starts and proxies to backend
    - Start Vite dev server on port 3000
    - Verify the page loads and the route search panel renders
    - Submit a search query and verify data appears (vehicles on map, vehicle list populated)
    - _Requirements: 7.1, 12.2_

- [x] 11. Final checkpoint - Full application working
  - Ensure both `mvn clean compile test` and `npm run build && npm run test` pass, docker-compose builds, and the application serves mock data end-to-end. Ask the user if questions arise.

## Task Dependency Graph

```json
{
  "waves": [
    { "tasks": ["1", "2"], "description": "Fix compilation for backend and frontend" },
    { "tasks": ["3"], "description": "Checkpoint - both compile successfully" },
    { "tasks": ["4"], "description": "Verify frontend-backend integration" },
    { "tasks": ["5"], "description": "Checkpoint - integration verified" },
    { "tasks": ["6", "7"], "description": "Write and fix unit tests for backend and frontend" },
    { "tasks": ["8"], "description": "Checkpoint - all tests pass" },
    { "tasks": ["9"], "description": "Docker and deployment configuration" },
    { "tasks": ["10"], "description": "End-to-end smoke test" },
    { "tasks": ["11"], "description": "Final checkpoint - full application working" }
  ]
}
```

## Notes

- Tasks marked with `*` are optional property-based tests that can be skipped for faster MVP
- GtfsRealtimeParser.java and MtaFeedRegistry.java must NOT be modified
- The backend uses `DATA_SOURCE_MODE=MOCK` for local development without MTA API access
- Build tasks (1-3) should be completed first since all other work depends on successful compilation
- Property tests use jqwik (Java) and fast-check (TypeScript) with minimum 100 iterations each
