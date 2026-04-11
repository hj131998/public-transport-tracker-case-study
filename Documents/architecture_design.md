# Public Transport Tracker — Architecture Design Document

## 1. Problem Summary

Build a **production-ready Public Transport Tracker** full-stack application that:
- Accepts city/route identifiers as input
- Fetches real-time transit data from external APIs (MTA, Transit.land)
- Returns vehicle locations, ETAs, disruptions, route plans, and crowding levels
- Handles API failures gracefully with offline/cache fallback
- Displays conditional alerts based on delay, disruption, crowding, and weather
- Supports CI/CD, Docker containerization, and observability

**Chosen Option: Option A – Resilience & Offline Mode**
> Rationale: Option A directly addresses the most critical NFR for a real-time tracker — availability under upstream API failures. In-memory caching with TTL and graceful degradation ensures users always get a response. Option B (Observability) is partially included as a bonus. Option C (Data Reasoning) is partially covered via ETA logic in the service layer.

---

## 2. Architecture Style

**Microservice Architecture** with a **BFF (Backend for Frontend)** pattern.

- The React frontend talks to a single BFF Gateway (Spring Boot) which orchestrates calls to downstream microservices.
- Each microservice owns a single bounded context.
- Services communicate via REST (sync) internally; async events via an in-memory event bus (no DB required).

---

## 3. System Context Diagram (C4 Level 1)

```
+------------------+        HTTPS         +-------------------------+
|   End User       |  ─────────────────>  |   React Frontend (SPA)  |
| (Web Browser)    |  <─────────────────  |   Port: 3000            |
+------------------+                      +-------------------------+
                                                      |
                                                 REST/JSON
                                                      |
                                          +-------------------------+
                                          |   BFF Gateway Service   |
                                          |   Spring Boot :8080     |
                                          +-------------------------+
                                          /          |           \
                                REST    /            |            \  REST
                                       /             |             \
                          +-----------+   +----------+    +----------+
                          | Transit   |   | Alert    |    | Route    |
                          | Service   |   | Service  |    | Planner  |
                          | :8081     |   | :8082    |    | :8083    |
                          +-----------+   +----------+    +----------+
                               |
                          External APIs
                          +-----------+    +-------------+
                          | MTA API   |    | Transit.land|
                          | (NYC)     |    | API         |
                          +-----------+    +-------------+
```

---

## 4. Component Architecture (C4 Level 2)

### 4.1 Frontend (React SPA)

| Component | Responsibility |
|---|---|
| RouteSearchPanel | Input: city, route ID, date/time |
| MapView | Renders real-time vehicle positions on map |
| VehicleList | Shows list of vehicles with ETA and crowding |
| AlertBanner | Displays conditional alerts (delay/disruption/crowding/weather) |
| OfflineModeToggle | Switches between live and cached/mock data |
| RouteAlternatives | Shows alternative route options |
| NotificationPanel | Push/in-app notifications for subscribed routes |

**State Management:** React Context API + useReducer (no Redux needed for this scope)

**Key React Patterns Used:**
- Container/Presentational component split
- Custom hooks (`useTransitData`, `useAlerts`, `useOfflineMode`)
- Error Boundary for graceful UI degradation
- Lazy loading for MapView (heavy component)

---

### 4.2 BFF Gateway Service (Spring Boot :8080)

Aggregates responses from downstream services into a single response for the frontend.

| Layer | Class/Component | Responsibility |
|---|---|---|
| Controller | `TransitController` | REST endpoints, input validation |
| Service | `TransitAggregatorService` | Orchestrates calls to downstream services |
| Cache | `InMemoryCacheService` | TTL-based cache, stale-data fallback |
| Client | `TransitApiClient` | HTTP client to external APIs |
| Resilience | `CircuitBreakerService` | Wraps external calls, tracks failure state |
| Model | `TransitResponse`, `VehiclePosition`, `Alert` | Domain models |
| Config | `AppConfig`, `SecurityConfig` | API key management, CORS, security |

---

### 4.3 Transit Data Service (Spring Boot :8081)

Owns all real-time data fetching from external APIs.

- Fetches vehicle positions, ETAs, disruptions from MTA / Transit.land
- Applies TTL cache (default: 30 seconds for real-time data)
- Returns stale cache on upstream failure
- Normalizes data from different API formats into a unified model

---

### 4.4 Alert Service (Spring Boot :8082)

Evaluates business rules and generates alerts.

| Rule | Alert Message |
|---|---|
| delay > 15 min | "Significant delays - Plan accordingly" |
| service disruption | "Service alert - Check alternative routes" |
| crowding = HIGH | "Vehicle at capacity - Consider next service" |
| weather impact | "Weather impact on schedule" |

---

### 4.5 Route Planner Service (Spring Boot :8083)

- Accepts origin, destination, preferences
- Returns primary route + up to 3 alternatives
- Calculates estimated journey time
- Flags disrupted segments

---

## 5. Data Flow Diagram

```
User enters Route/City
        |
        v
React Frontend
        |
        | GET /api/v1/transit?city=NYC&route=M15
        v
BFF Gateway (:8080)
        |
        |-- Check InMemoryCache (TTL: 30s) ──> [HIT] Return cached response
        |
        |-- [MISS] Call Transit Service (:8081)
        |           |
        |           |-- Call MTA API / Transit.land
        |           |   [SUCCESS] Normalize + Cache + Return
        |           |   [FAILURE] Return stale cache or mock data
        |
        |-- Call Alert Service (:8082)
        |           |
        |           |-- Evaluate delay, disruption, crowding, weather rules
        |           |-- Return list of active alerts
        |
        |-- Call Route Planner (:8083)
                    |
                    |-- Calculate primary + alternative routes
                    |-- Return route options with ETAs
        |
        v
Aggregate all responses → TransitResponse
        |
        v
React Frontend renders:
  - Map with vehicle positions
  - ETA list
  - Alert banners
  - Route alternatives
```

---

## 6. API Design (REST / HATEOAS)

### Base URL: `http://localhost:8080/api/v1`

| Method | Endpoint | Description |
|---|---|---|
| GET | `/transit` | Get real-time transit data for city/route |
| GET | `/transit/{routeId}/vehicles` | Get vehicle positions for a route |
| GET | `/transit/{routeId}/alerts` | Get active alerts for a route |
| GET | `/routes/plan` | Plan a route between two stops |
| GET | `/routes/{routeId}/alternatives` | Get alternative routes |
| GET | `/health` | Health check endpoint |
| GET | `/actuator/metrics` | Metrics endpoint (Micrometer) |

### Sample Request/Response

```
GET /api/v1/transit?city=NYC&route=M15

Response 200:
{
  "routeId": "M15",
  "city": "NYC",
  "dataSource": "LIVE | CACHED | MOCK",
  "cacheAge": 12,
  "vehicles": [
    {
      "vehicleId": "V001",
      "lat": 40.7128,
      "lon": -74.0060,
      "nextStop": "34th St",
      "eta": "3 min",
      "crowding": "MEDIUM",
      "delay": 5
    }
  ],
  "alerts": [
    {
      "type": "DELAY",
      "severity": "HIGH",
      "message": "Significant delays - Plan accordingly"
    }
  ],
  "_links": {
    "self": { "href": "/api/v1/transit?city=NYC&route=M15" },
    "alternatives": { "href": "/api/v1/routes/M15/alternatives" },
    "alerts": { "href": "/api/v1/transit/M15/alerts" }
  }
}
```

> `_links` section implements **HATEOAS** — the client discovers next actions from the response itself.

---

## 7. Resilience & Offline Mode Strategy (Option A)

### 7.1 In-Memory Cache with TTL

```
Cache Key: "transit:{city}:{routeId}"
TTL: 30 seconds (configurable)
Max Size: 1000 entries (LRU eviction)
Stale TTL: 5 minutes (serve stale on failure)
```

### 7.2 Degradation Levels

| Level | Condition | Behavior |
|---|---|---|
| LIVE | API healthy, cache fresh | Return live data |
| STALE | API failed, cache < 5 min old | Return stale + warn header |
| MOCK | API failed, no cache | Return mock/seed data + offline flag |
| ERROR | All failed | Return 503 with retry-after header |

### 7.3 Circuit Breaker States

```
CLOSED ──(failures > threshold)──> OPEN ──(timeout)──> HALF_OPEN
  ^                                                          |
  └──────────────(success)──────────────────────────────────┘
```

- Threshold: 5 failures in 10 seconds → OPEN
- Open duration: 30 seconds
- Half-open: 1 probe request

---

## 8. Security Design

| Concern | Solution |
|---|---|
| API Key Protection | Stored in environment variables / Spring `@Value`, never in code |
| CORS | Configured to allow only frontend origin |
| Input Validation | `@Valid` + Bean Validation on all request params |
| Rate Limiting | Token bucket per IP (Spring filter) |
| HTTPS | TLS termination at load balancer / reverse proxy |
| Secrets in CI/CD | Jenkins credentials store / GitHub Secrets |

---

## 9. CI/CD Pipeline Design

```
Developer Push
      |
      v
GitHub Repository
      |
      v
Jenkins Pipeline (Jenkinsfile)
      |
      |── Stage 1: Checkout
      |── Stage 2: Build (Maven / npm)
      |── Stage 3: Unit Tests (JUnit / Jest)
      |── Stage 4: Integration Tests
      |── Stage 5: Code Quality (SonarQube)
      |── Stage 6: Security Scan (OWASP)
      |── Stage 7: Docker Build & Tag
      |── Stage 8: Push to Registry (ECR / DockerHub)
      |── Stage 9: Deploy to Dev
      |── Stage 10: Smoke Tests
      |── Stage 11: Deploy to Staging (manual gate)
      |── Stage 12: Deploy to Prod (manual gate)
      |
      v
Docker Containers running on port 8080
```

### Multi-Environment Config

| Environment | Config Source | API Keys |
|---|---|---|
| dev | `application-dev.yml` | Mock keys / sandbox |
| staging | `application-staging.yml` | Staging keys |
| prod | `application-prod.yml` | Prod keys from Vault/Secrets Manager |

---

## 10. Deployment Architecture

```
                    ┌─────────────────────────────────────┐
                    │           Docker Compose             │
                    │                                      │
                    │  ┌──────────┐   ┌────────────────┐  │
                    │  │  nginx   │   │  React Frontend│  │
                    │  │ :80/:443 │──>│  :3000         │  │
                    │  └──────────┘   └────────────────┘  │
                    │       |                              │
                    │  ┌────▼───────────────────────────┐  │
                    │  │   BFF Gateway  :8080           │  │
                    │  └────────────────────────────────┘  │
                    │   /           |            \         │
                    │  ┌──────┐  ┌──────┐  ┌──────┐       │
                    │  │:8081 │  │:8082 │  │:8083 │       │
                    │  └──────┘  └──────┘  └──────┘       │
                    └─────────────────────────────────────┘
```

---

## 11. Non-Functional Requirements Coverage

| NFR | Implementation |
|---|---|
| SOLID Principles | Each service/class has single responsibility; interfaces for all clients |
| 12-Factor App | Config via env vars, stateless services, logs to stdout, one codebase |
| HATEOAS | `_links` in all API responses |
| Performance | In-memory cache, async parallel calls to downstream services |
| Security | API key encryption, input validation, CORS, rate limiting |
| TDD/BDD | JUnit 5 + Mockito for unit tests; Cucumber for BDD scenarios |
| Production Quality | Structured logging (SLF4J/Logback), health endpoints, metrics |

---

## 12. Technology Stack Summary

| Layer | Technology | Reason |
|---|---|---|
| Frontend | React 18 + TypeScript | Component model, hooks, strong typing |
| UI Library | Tailwind CSS | Utility-first, fast styling |
| Map | Leaflet.js | Lightweight, open-source map |
| Backend | Java 17 + Spring Boot 3 | Mature, production-ready, rich ecosystem |
| HTTP Client | Spring WebClient (reactive) | Non-blocking external API calls |
| Cache | ConcurrentHashMap + TTL | No DB required per spec |
| Circuit Breaker | Resilience4j | Lightweight, Spring Boot integration |
| Testing | JUnit 5, Mockito, Jest, Cucumber | TDD + BDD coverage |
| CI/CD | Jenkins + Jenkinsfile | Per spec requirement |
| Containerization | Docker + Docker Compose | Per spec requirement |
| API Docs | SpringDoc OpenAPI (Swagger UI) | Per spec requirement |
| Logging | SLF4J + Logback (JSON format) | Structured logging |
| Metrics | Micrometer + Actuator | Latency, failure rate, cache hit rate |
