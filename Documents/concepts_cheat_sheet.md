# Concepts Cheat Sheet — Public Transport Tracker

Quick-reference for all concepts used in this design. Use this during your review.

---

## ARCHITECTURE PATTERNS

### Microservices Architecture
Split application into small, independently deployable services, each owning a single business capability. Services communicate over HTTP/REST or messaging.
- Used here: BFF Gateway, Transit Service, Alert Service, Route Planner as separate services.

### BFF — Backend for Frontend
A dedicated backend service tailored for a specific frontend (e.g., React SPA). It aggregates data from multiple downstream services into one response, reducing frontend complexity.
- Used here: BFF Gateway (:8080) aggregates Transit + Alert + Route data for the React UI.

### C4 Model
A hierarchical way to document software architecture at 4 levels: Context → Container → Component → Code. Helps communicate architecture to different audiences.
- Used here: System Context (Level 1) and Component diagrams (Level 2).

### 12-Factor App
A methodology for building production-ready, cloud-native apps. Key factors relevant here:
- Config via environment variables (not hardcoded)
- Stateless processes
- Logs as event streams (stdout)
- One codebase, multiple deploys
- Port binding (app exposes its own port)

---

## DESIGN PRINCIPLES

### SOLID Principles
- **S** — Single Responsibility: Each class does one thing (e.g., `AlertService` only evaluates alerts)
- **O** — Open/Closed: Add new alert rules without modifying existing ones (strategy pattern)
- **L** — Liskov Substitution: `MtaApiClient` and `TransitLandClient` are interchangeable via `TransitDataProvider` interface
- **I** — Interface Segregation: Separate interfaces for `CacheService`, `AlertEvaluator`, `ResilienceStrategy`
- **D** — Dependency Inversion: `TransitAggregatorService` depends on `TransitDataProvider` interface, not concrete clients

### HATEOAS
Hypermedia As The Engine Of Application State. REST responses include `_links` so clients can discover available actions without hardcoding URLs.
- Used here: Every API response includes `_links` with `self`, `alternatives`, `alerts` URLs.

### DRY / KISS / YAGNI
- DRY: Don't Repeat Yourself — shared models, reusable cache logic
- KISS: Keep It Simple — in-memory cache instead of Redis (no DB required)
- YAGNI: You Aren't Gonna Need It — don't build features not in scope

---

## RESILIENCE PATTERNS

### Circuit Breaker
Prevents cascading failures by stopping calls to a failing service. Has 3 states:
- CLOSED: Normal operation
- OPEN: Calls blocked, fallback used (after N failures)
- HALF-OPEN: One probe request allowed to test recovery
- Used here: Wraps all external API calls.

### Cache-Aside Pattern
Application code manages the cache: check cache first, on miss fetch from source, then populate cache.
- Used here: `InMemoryCacheService` with TTL=30s.

### Graceful Degradation
System continues to function at reduced capability when dependencies fail. Levels: LIVE → STALE → MOCK → ERROR.
- Used here: Offline mode with stale cache and mock data fallback.

### Stale-While-Revalidate
Serve stale (expired) cached data while fetching fresh data in the background. Improves perceived performance.
- Used here: Stale TTL of 5 minutes — serve old data rather than error.

### Retry with Backoff
Retry failed requests with increasing wait times (exponential backoff) to avoid overwhelming a recovering service.
- Used here: WebClient retry config on transient failures.

---

## CONCURRENCY & CACHING

### TTL (Time To Live)
A duration after which a cache entry is considered expired and must be refreshed.
- Used here: 30s TTL for real-time data, 5min stale TTL.

### ConcurrentHashMap
Thread-safe Java Map implementation. Allows concurrent reads without locking the entire map.
- Used here: Backing store for `InMemoryCacheService`.

### LRU Eviction (Least Recently Used)
Cache eviction policy — when cache is full, remove the entry that was accessed least recently.
- Used here: Max 1000 entries with LRU eviction.

---

## API DESIGN

### REST (Representational State Transfer)
Architectural style for APIs using HTTP verbs (GET, POST, PUT, DELETE) and resource-based URLs.
- Used here: All service-to-service and frontend-to-BFF communication.

### OpenAPI / Swagger
Specification standard for documenting REST APIs. SpringDoc generates interactive Swagger UI automatically from annotations.
- Used here: `springdoc-openapi` dependency in Spring Boot.

### API Versioning
Prefix URLs with `/api/v1/` to allow future breaking changes without affecting existing clients.
- Used here: All endpoints under `/api/v1/`.

### Rate Limiting
Restrict number of requests per client per time window to prevent abuse.
- Used here: Token bucket filter in Spring Security config.

---

## TESTING

### TDD — Test Driven Development
Write the test first, then write the minimum code to make it pass, then refactor. Red → Green → Refactor cycle.
- Used here: JUnit 5 tests written before service implementation.

### BDD — Behavior Driven Development
Write tests in plain English (Given/When/Then) using Cucumber. Bridges gap between business and technical teams.
- Example: `Given route M15 is requested, When API is down, Then stale data is returned`
- Used here: Cucumber + JUnit 5 for BDD scenarios.

### Unit Testing
Test a single class/method in isolation using mocks for dependencies.
- Used here: Mockito to mock `TransitApiClient` in `TransitAggregatorServiceTest`.

### Integration Testing
Test multiple components working together (e.g., controller + service + cache).
- Used here: Spring Boot `@SpringBootTest` tests.

### Smoke Testing
Quick sanity check after deployment — hit key endpoints to verify the app is alive.
- Used here: Post-deploy smoke test stage in Jenkins pipeline.

---

## FRONTEND PATTERNS

### Container / Presentational Pattern
Separate components into "smart" containers (handle data/state) and "dumb" presentational components (only render UI).
- Used here: `MapPage` (container) vs `MapView` (presentational).

### Custom Hooks
Encapsulate reusable stateful logic in React hooks (functions starting with `use`).
- Used here: `useTransitData`, `useAlerts`, `useOfflineMode`.

### Error Boundary
React component that catches JavaScript errors in child components and renders a fallback UI instead of crashing.
- Used here: Wraps `MapView` (heavy, error-prone component).

### Lazy Loading
Load components only when needed using `React.lazy()` and `Suspense`. Reduces initial bundle size.
- Used here: `MapView` loaded lazily.

### State Management (Context API + useReducer)
React's built-in state management. Context provides global state; useReducer handles complex state transitions.
- Used here: Transit data state shared across components without Redux.

---

## CI/CD

### CI — Continuous Integration
Automatically build and test code on every push. Catches bugs early.
- Used here: Jenkins pipeline triggers on every push.

### CD — Continuous Delivery/Deployment
Automatically deploy to environments after CI passes. Delivery = manual gate to prod; Deployment = fully automated.
- Used here: Auto-deploy to DEV, manual gate for Staging and Prod.

### Jenkinsfile (Pipeline as Code)
Define CI/CD pipeline in a `Jenkinsfile` committed to the repo. Version-controlled, reproducible.
- Used here: Jenkinsfile with stages for build, test, scan, docker, deploy.

### Docker
Containerization platform. Packages app + dependencies into a portable image.
- Used here: Dockerfile for each service, Docker Compose for local orchestration.

### Multi-Stage Docker Build
Use multiple `FROM` stages in Dockerfile — build stage (with JDK/Node) and runtime stage (slim image). Reduces final image size.
- Used here: Builder stage with Maven/Node, runtime stage with JRE/nginx.

### Multi-Environment Config
Separate configuration files per environment (dev/staging/prod). Spring Boot profiles (`application-dev.yml`) activate the right config.
- Used here: Spring profiles + environment variable overrides.

---

## SECURITY

### API Key Management
Never hardcode API keys. Store in environment variables, CI/CD secrets, or a secrets manager (AWS Secrets Manager, HashiCorp Vault).
- Used here: Keys injected via `${MTA_API_KEY}` in `application.yml`.

### CORS — Cross-Origin Resource Sharing
Browser security mechanism. Backend must explicitly allow requests from the frontend's origin.
- Used here: Spring Security CORS config allows `http://localhost:3000`.

### Input Validation
Validate all user inputs at the API boundary using Bean Validation (`@NotBlank`, `@Pattern`).
- Used here: `@Valid` on controller request params.

### OWASP Dependency Check
Scans project dependencies for known CVEs (Common Vulnerabilities and Exposures).
- Used here: Jenkins pipeline stage.

---

## OBSERVABILITY

### Structured Logging
Log in JSON format with consistent fields (timestamp, level, traceId, message). Machine-parseable for log aggregation tools (ELK, Splunk).
- Used here: Logback JSON encoder.

### Health Endpoints
`/health` (liveness) and `/ready` (readiness) endpoints for container orchestrators to check if the app is alive and ready to serve traffic.
- Used here: Spring Boot Actuator `/actuator/health`.

### Metrics
Quantitative measurements: request latency, error rate, cache hit rate, circuit breaker state.
- Used here: Micrometer + Spring Boot Actuator `/actuator/metrics`.

### SLO — Service Level Objective
A target for a service metric. Example: "99% of requests respond in < 500ms". Defines acceptable performance.
- Example SLOs for this app:
  - Availability: 99.5% uptime
  - Latency: p95 < 300ms
  - Cache hit rate: > 70%

### Distributed Tracing
Track a request as it flows through multiple services using a trace ID. Tools: Zipkin, Jaeger.
- Bonus: Add `X-Trace-Id` header propagation across services.

---

## DESIGN PATTERNS (GoF)

### Strategy Pattern
Define a family of algorithms, encapsulate each one, make them interchangeable.
- Used here: `ResilienceStrategy` interface — swap between CircuitBreaker, Retry, etc.

### Factory Pattern
Create objects without specifying the exact class. A factory decides which implementation to instantiate.
- Used here: `TransitDataProviderFactory` selects `MtaApiClient` or `TransitLandClient` based on city.

### Adapter Pattern
Convert the interface of a class into another interface clients expect. Wraps incompatible interfaces.
- Used here: `MtaApiClient` and `TransitLandClient` adapt different API response formats to the unified `VehiclePosition` model.

### Decorator Pattern
Add behavior to an object dynamically without changing its class.
- Used here: Logging decorator wraps `TransitDataProvider` to log all API calls.

### Builder Pattern
Construct complex objects step by step.
- Used here: `TransitResponse.builder()` for constructing the aggregated response.

---

## DATA STRUCTURES

### ConcurrentHashMap
Thread-safe hash map. Used for in-memory cache. O(1) average get/put.

### Priority Queue
Used in route planning to implement Dijkstra's algorithm for shortest path.

### Immutable Value Objects
Domain models (`VehiclePosition`, `Alert`) are immutable — created once, never modified. Thread-safe by design.
- Used here: Lombok `@Value` or Java records.
