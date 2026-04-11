# Public Transport Tracker — UML & LLD Diagrams

All diagrams use **Mermaid** syntax. Render at https://mermaid.live

---

## 1. Class Diagram — Domain Models

```mermaid
classDiagram
    class TransitResponse {
        +String routeId
        +String city
        +DataSource dataSource
        +int cacheAgeSeconds
        +boolean offline
        +List~VehiclePosition~ vehicles
        +List~Alert~ alerts
        +RoutePlan routePlan
        +Map~String,Link~ _links
    }

    class VehiclePosition {
        +String vehicleId
        +double lat
        +double lon
        +String nextStop
        +String eta
        +CrowdingLevel crowding
        +int delayMinutes
        +boolean disrupted
    }

    class Alert {
        +AlertType type
        +Severity severity
        +String message
        +Instant generatedAt
    }

    class RoutePlan {
        +Route primaryRoute
        +List~Route~ alternatives
        +int estimatedMinutes
    }

    class Route {
        +String routeId
        +List~Stop~ stops
        +int durationMinutes
        +boolean hasDisruption
    }

    class Stop {
        +String stopId
        +String name
        +double lat
        +double lon
        +String eta
    }

    class AlertType {
        <<enumeration>>
        DELAY
        DISRUPTION
        CROWDING
        WEATHER
    }

    class CrowdingLevel {
        <<enumeration>>
        LOW
        MEDIUM
        HIGH
    }

    class DataSource {
        <<enumeration>>
        LIVE
        CACHED
        STALE
        MOCK
    }

    class Severity {
        <<enumeration>>
        LOW
        MEDIUM
        HIGH
    }

    TransitResponse "1" *-- "many" VehiclePosition
    TransitResponse "1" *-- "many" Alert
    TransitResponse "1" *-- "0..1" RoutePlan
    RoutePlan "1" *-- "many" Route
    Route "1" *-- "many" Stop
    Alert --> AlertType
    VehiclePosition --> CrowdingLevel
    TransitResponse --> DataSource
    Alert --> Severity
```

---

## 2. Class Diagram — BFF Gateway Service Layer

```mermaid
classDiagram
    class TransitController {
        -TransitAggregatorService aggregatorService
        +getTransitData(city, route) ResponseEntity
        +getVehicles(routeId) ResponseEntity
        +getAlerts(routeId) ResponseEntity
        +planRoute(from, to) ResponseEntity
    }

    class TransitAggregatorService {
        -TransitApiClient transitClient
        -AlertService alertService
        -RoutePlannerClient routeClient
        -InMemoryCacheService cache
        -CircuitBreakerService circuitBreaker
        +aggregate(city, route) TransitResponse
        -fetchWithFallback(city, route) List~VehiclePosition~
    }

    class InMemoryCacheService {
        -ConcurrentHashMap~String,CacheEntry~ store
        -int ttlSeconds
        -int staleTtlSeconds
        +get(key) Optional~T~
        +getStale(key) Optional~T~
        +put(key, value) void
        +evictExpired() void
    }

    class CacheEntry~T~ {
        +T data
        +Instant createdAt
        +boolean isExpired(ttl)
        +boolean isStale(staleTtl)
    }

    class CircuitBreakerService {
        -CircuitState state
        -int failureCount
        -int threshold
        -Instant openedAt
        -Duration openDuration
        +isOpen() boolean
        +recordSuccess() void
        +recordFailure() void
        -transitionTo(state) void
    }

    class TransitApiClient {
        -WebClient webClient
        -String apiKey
        -String baseUrl
        +fetchVehicles(city, route) List~VehiclePosition~
        +fetchDisruptions(routeId) List~Disruption~
        -normalize(rawResponse) List~VehiclePosition~
    }

    class AlertService {
        +evaluate(vehicles, disruptions) List~Alert~
        -checkDelay(vehicle) Optional~Alert~
        -checkDisruption(disruption) Optional~Alert~
        -checkCrowding(vehicle) Optional~Alert~
        -checkWeather(data) Optional~Alert~
    }

    class MockDataProvider {
        +getMockVehicles(city, route) List~VehiclePosition~
        +getMockAlerts() List~Alert~
    }

    TransitController --> TransitAggregatorService
    TransitAggregatorService --> InMemoryCacheService
    TransitAggregatorService --> CircuitBreakerService
    TransitAggregatorService --> TransitApiClient
    TransitAggregatorService --> AlertService
    TransitAggregatorService --> MockDataProvider
    InMemoryCacheService --> CacheEntry
```

---

## 3. Class Diagram — Interfaces & SOLID Design

```mermaid
classDiagram
    class TransitDataProvider {
        <<interface>>
        +fetchVehicles(city, route) List~VehiclePosition~
        +fetchDisruptions(routeId) List~Disruption~
    }

    class CacheService {
        <<interface>>
        +get(key) Optional~T~
        +put(key, value, ttl) void
        +getStale(key) Optional~T~
    }

    class AlertEvaluator {
        <<interface>>
        +evaluate(data) List~Alert~
    }

    class ResilienceStrategy {
        <<interface>>
        +execute(supplier) T
        +isAvailable() boolean
    }

    class MtaApiClient {
        +fetchVehicles(city, route) List~VehiclePosition~
        +fetchDisruptions(routeId) List~Disruption~
    }

    class TransitLandClient {
        +fetchVehicles(city, route) List~VehiclePosition~
        +fetchDisruptions(routeId) List~Disruption~
    }

    class InMemoryCacheService {
        +get(key) Optional~T~
        +put(key, value, ttl) void
        +getStale(key) Optional~T~
    }

    class RuleBasedAlertEvaluator {
        +evaluate(data) List~Alert~
    }

    class CircuitBreakerStrategy {
        +execute(supplier) T
        +isAvailable() boolean
    }

    TransitDataProvider <|.. MtaApiClient : implements
    TransitDataProvider <|.. TransitLandClient : implements
    CacheService <|.. InMemoryCacheService : implements
    AlertEvaluator <|.. RuleBasedAlertEvaluator : implements
    ResilienceStrategy <|.. CircuitBreakerStrategy : implements
```

> This design follows **Dependency Inversion Principle** — high-level modules depend on abstractions, not concretions.

---

## 4. Component Diagram — Full System

```mermaid
graph TB
    subgraph Browser
        UI[React SPA]
    end

    subgraph Docker Network
        subgraph BFF[BFF Gateway :8080]
            TC[TransitController]
            TAS[TransitAggregatorService]
            CB[CircuitBreaker]
            CACHE[InMemoryCache]
        end

        subgraph TS[Transit Service :8081]
            TVC[VehicleController]
            TAC[TransitApiClient]
        end

        subgraph AS[Alert Service :8082]
            AC[AlertController]
            AE[AlertEvaluator]
        end

        subgraph RP[Route Planner :8083]
            RC[RouteController]
            RPL[RoutePlannerLogic]
        end
    end

    subgraph External
        MTA[MTA API]
        TL[Transit.land API]
    end

    UI -->|REST JSON| TC
    TC --> TAS
    TAS --> CB
    TAS --> CACHE
    TAS -->|REST| TVC
    TAS -->|REST| AC
    TAS -->|REST| RC
    TAC -->|HTTPS + API Key| MTA
    TAC -->|HTTPS + API Key| TL
```

---

## 5. State Diagram — Circuit Breaker

```mermaid
stateDiagram-v2
    [*] --> CLOSED
    CLOSED --> CLOSED : success
    CLOSED --> OPEN : failures >= threshold (5 in 10s)
    OPEN --> HALF_OPEN : timeout elapsed (30s)
    HALF_OPEN --> CLOSED : probe request succeeds
    HALF_OPEN --> OPEN : probe request fails
```

---

## 6. State Diagram — Cache Entry Lifecycle

```mermaid
stateDiagram-v2
    [*] --> FRESH : put(key, data)
    FRESH --> STALE : TTL expired (30s)
    STALE --> EVICTED : Stale TTL expired (5min)
    EVICTED --> [*]
    FRESH --> FRESH : get() returns data
    STALE --> STALE : getStale() returns data with warning
```

---

## 7. Deployment Diagram

```mermaid
graph LR
    subgraph Host Machine
        subgraph docker-compose
            N[nginx :80]
            F[frontend :3000]
            G[bff-gateway :8080]
            T[transit-service :8081]
            A[alert-service :8082]
            R[route-planner :8083]
        end
    end

    Browser -->|HTTP :80| N
    N -->|proxy| F
    N -->|proxy /api| G
    G --> T
    G --> A
    G --> R
```

---

## 8. Entity Relationship — Data Models (Logical, No DB)

```mermaid
erDiagram
    TRANSIT_RESPONSE {
        string routeId
        string city
        string dataSource
        int cacheAgeSeconds
        boolean offline
    }

    VEHICLE_POSITION {
        string vehicleId
        float lat
        float lon
        string nextStop
        string eta
        string crowding
        int delayMinutes
    }

    ALERT {
        string type
        string severity
        string message
        timestamp generatedAt
    }

    ROUTE {
        string routeId
        int durationMinutes
        boolean hasDisruption
    }

    STOP {
        string stopId
        string name
        float lat
        float lon
        string eta
    }

    TRANSIT_RESPONSE ||--o{ VEHICLE_POSITION : contains
    TRANSIT_RESPONSE ||--o{ ALERT : has
    TRANSIT_RESPONSE ||--o{ ROUTE : includes
    ROUTE ||--o{ STOP : has
```

---

## 9. React Component Tree (UI Architecture)

```mermaid
graph TD
    App --> Router
    Router --> HomePage
    Router --> MapPage
    HomePage --> RouteSearchPanel
    HomePage --> AlertBanner
    HomePage --> OfflineModeToggle
    MapPage --> MapView
    MapPage --> VehicleList
    MapPage --> RouteAlternatives
    MapPage --> NotificationPanel
    RouteSearchPanel --> useTransitData
    VehicleList --> useTransitData
    AlertBanner --> useAlerts
    MapView --> useMapMarkers
    OfflineModeToggle --> useOfflineMode
    useTransitData --> apiService
    useAlerts --> apiService
    apiService --> BFF_Gateway
```

---

## 10. Package Structure — Backend (LLD)

```
transport-tracker-bff/
├── src/main/java/com/tracker/
│   ├── controller/
│   │   ├── TransitController.java
│   │   └── HealthController.java
│   ├── service/
│   │   ├── TransitAggregatorService.java
│   │   ├── AlertService.java
│   │   └── MockDataProvider.java
│   ├── client/
│   │   ├── TransitDataProvider.java       ← interface
│   │   ├── MtaApiClient.java
│   │   └── TransitLandClient.java
│   ├── cache/
│   │   ├── CacheService.java              ← interface
│   │   ├── InMemoryCacheService.java
│   │   └── CacheEntry.java
│   ├── resilience/
│   │   ├── ResilienceStrategy.java        ← interface
│   │   └── CircuitBreakerService.java
│   ├── model/
│   │   ├── TransitResponse.java
│   │   ├── VehiclePosition.java
│   │   ├── Alert.java
│   │   ├── RoutePlan.java
│   │   ├── Route.java
│   │   └── Stop.java
│   ├── config/
│   │   ├── AppConfig.java
│   │   ├── SecurityConfig.java
│   │   └── WebClientConfig.java
│   └── TransportTrackerApplication.java
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-staging.yml
│   └── application-prod.yml
├── src/test/java/com/tracker/
│   ├── controller/TransitControllerTest.java
│   ├── service/TransitAggregatorServiceTest.java
│   ├── cache/InMemoryCacheServiceTest.java
│   └── bdd/steps/TransitSteps.java
├── Dockerfile
├── Jenkinsfile
└── pom.xml

transport-tracker-frontend/
├── src/
│   ├── components/
│   │   ├── RouteSearchPanel/
│   │   ├── MapView/
│   │   ├── VehicleList/
│   │   ├── AlertBanner/
│   │   ├── OfflineModeToggle/
│   │   └── RouteAlternatives/
│   ├── hooks/
│   │   ├── useTransitData.ts
│   │   ├── useAlerts.ts
│   │   └── useOfflineMode.ts
│   ├── services/
│   │   └── apiService.ts
│   ├── types/
│   │   └── transit.types.ts
│   └── App.tsx
├── Dockerfile
└── package.json
```
