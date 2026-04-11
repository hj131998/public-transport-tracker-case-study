# Public Transport Tracker — Sequence Diagrams

All diagrams are in **Mermaid** format. Paste them at https://mermaid.live to render.

---

## 1. Real-Time Transit Data Fetch (Happy Path)

```mermaid
sequenceDiagram
    actor User
    participant UI as React Frontend
    participant BFF as BFF Gateway :8080
    participant Cache as InMemoryCache
    participant TS as Transit Service :8081
    participant EXT as External API (MTA/Transit.land)

    User->>UI: Enter city=NYC, route=M15
    UI->>BFF: GET /api/v1/transit?city=NYC&route=M15
    BFF->>Cache: get("transit:NYC:M15")
    Cache-->>BFF: MISS (expired or absent)
    BFF->>TS: GET /vehicles?city=NYC&route=M15
    TS->>EXT: HTTP GET (with API key)
    EXT-->>TS: 200 OK - vehicle positions JSON
    TS-->>BFF: Normalized VehicleList
    BFF->>Cache: put("transit:NYC:M15", data, TTL=30s)
    BFF-->>UI: 200 OK - TransitResponse (dataSource=LIVE)
    UI-->>User: Render map + vehicle list + ETAs
```

---

## 2. Cache Hit Flow

```mermaid
sequenceDiagram
    actor User
    participant UI as React Frontend
    participant BFF as BFF Gateway :8080
    participant Cache as InMemoryCache

    User->>UI: Refresh route M15
    UI->>BFF: GET /api/v1/transit?city=NYC&route=M15
    BFF->>Cache: get("transit:NYC:M15")
    Cache-->>BFF: HIT (age=12s, TTL=30s)
    BFF-->>UI: 200 OK - TransitResponse (dataSource=CACHED, cacheAge=12)
    UI-->>User: Render data with "Cached" indicator
```

---

## 3. API Failure — Stale Cache Fallback

```mermaid
sequenceDiagram
    actor User
    participant UI as React Frontend
    participant BFF as BFF Gateway :8080
    participant Cache as InMemoryCache
    participant CB as CircuitBreaker
    participant TS as Transit Service :8081
    participant EXT as External API

    User->>UI: Request route M15
    UI->>BFF: GET /api/v1/transit?city=NYC&route=M15
    BFF->>Cache: get("transit:NYC:M15")
    Cache-->>BFF: MISS (TTL expired)
    BFF->>CB: isOpen?
    CB-->>BFF: CLOSED (allow request)
    BFF->>TS: GET /vehicles
    TS->>EXT: HTTP GET
    EXT-->>TS: 503 Service Unavailable
    TS-->>BFF: Exception thrown
    CB->>CB: Record failure (3/5)
    BFF->>Cache: getStale("transit:NYC:M15")
    Cache-->>BFF: Stale data (age=4min, within 5min stale TTL)
    BFF-->>UI: 200 OK - TransitResponse (dataSource=STALE, warning="Data may be outdated")
    UI-->>User: Render stale data with warning banner
```

---

## 4. Full Offline / Mock Fallback (Circuit Open)

```mermaid
sequenceDiagram
    actor User
    participant UI as React Frontend
    participant BFF as BFF Gateway :8080
    participant CB as CircuitBreaker
    participant Cache as InMemoryCache
    participant Mock as MockDataProvider

    User->>UI: Request route M15
    UI->>BFF: GET /api/v1/transit?city=NYC&route=M15
    BFF->>CB: isOpen?
    CB-->>BFF: OPEN (5 failures in 10s)
    BFF->>Cache: getStale("transit:NYC:M15")
    Cache-->>BFF: MISS (no stale data available)
    BFF->>Mock: getMockData("NYC", "M15")
    Mock-->>BFF: Mock vehicle positions + ETAs
    BFF-->>UI: 200 OK - TransitResponse (dataSource=MOCK, offline=true)
    UI-->>User: Render mock data with "Offline Mode" banner
```

---

## 5. Alert Evaluation Flow

```mermaid
sequenceDiagram
    participant BFF as BFF Gateway :8080
    participant AS as Alert Service :8082
    participant TS as Transit Service :8081

    BFF->>TS: Get transit data for route
    TS-->>BFF: VehicleList with delay, crowding, disruption fields
    BFF->>AS: POST /alerts/evaluate (TransitData payload)
    AS->>AS: Rule: delay > 15min?
    AS->>AS: Rule: disruption == true?
    AS->>AS: Rule: crowding == HIGH?
    AS->>AS: Rule: weatherImpact == true?
    AS-->>BFF: List<Alert> with type, severity, message
    BFF->>BFF: Merge alerts into TransitResponse
```

---

## 6. Route Planning Flow

```mermaid
sequenceDiagram
    actor User
    participant UI as React Frontend
    participant BFF as BFF Gateway :8080
    participant RP as Route Planner :8083

    User->>UI: Enter origin="Times Sq", destination="Brooklyn Bridge"
    UI->>BFF: GET /api/v1/routes/plan?from=TimesSq&to=BrooklynBridge
    BFF->>RP: GET /plan?from=TimesSq&to=BrooklynBridge
    RP->>RP: Calculate primary route (shortest path)
    RP->>RP: Calculate up to 3 alternatives
    RP->>RP: Flag disrupted segments
    RP-->>BFF: RoutePlan { primary, alternatives[], estimatedTime }
    BFF-->>UI: 200 OK with RoutePlan + HATEOAS links
    UI-->>User: Display route options with ETAs and disruption flags
```

---

## 7. CI/CD Pipeline Sequence

```mermaid
sequenceDiagram
    participant Dev as Developer
    participant GH as GitHub
    participant JK as Jenkins
    participant DH as Docker Registry
    participant ENV as Target Environment

    Dev->>GH: git push (feature branch)
    GH->>JK: Webhook trigger
    JK->>JK: Stage 1 - Checkout code
    JK->>JK: Stage 2 - mvn clean install / npm build
    JK->>JK: Stage 3 - JUnit + Jest tests
    JK->>JK: Stage 4 - SonarQube scan
    JK->>JK: Stage 5 - OWASP dependency check
    JK->>JK: Stage 6 - docker build -t transport-tracker:SHA
    JK->>DH: docker push transport-tracker:SHA
    JK->>ENV: Deploy to DEV (docker-compose up)
    JK->>JK: Stage 7 - Smoke tests
    JK-->>Dev: Build result notification
    Dev->>JK: Manual approval for Staging
    JK->>ENV: Deploy to Staging
    Dev->>JK: Manual approval for Prod
    JK->>ENV: Deploy to Prod
```

---

## 8. Frontend Component Interaction (React)

```mermaid
sequenceDiagram
    participant User
    participant RSP as RouteSearchPanel
    participant Hook as useTransitData (hook)
    participant API as apiService.ts
    participant MV as MapView
    participant AB as AlertBanner
    participant VL as VehicleList

    User->>RSP: Submit city + route
    RSP->>Hook: setQuery({city, route})
    Hook->>API: fetchTransitData(city, route)
    API-->>Hook: TransitResponse
    Hook->>Hook: setState(vehicles, alerts, routes)
    Hook-->>MV: vehicles[]
    Hook-->>AB: alerts[]
    Hook-->>VL: vehicles[]
    MV-->>User: Render map markers
    AB-->>User: Render alert banners
    VL-->>User: Render vehicle list with ETAs
```
