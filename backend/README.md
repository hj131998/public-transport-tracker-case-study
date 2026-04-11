# Public Transport Tracker — Backend

Spring Boot 3 BFF Gateway service. Aggregates real-time transit data from MTA and Transit.land APIs with full offline resilience.

## Quick Start

### Prerequisites
- Java 17
- Maven 3.9+
- Docker + Docker Compose

### Run locally (without Docker)

```bash
cd backend/transport-tracker-bff
cp ../.env.template ../.env   # fill in your API keys
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Service starts on http://localhost:8080
Swagger UI: http://localhost:8080/swagger-ui.html

### Run with Docker Compose (recommended)

```bash
cd backend
cp .env.template .env         # fill in your API keys
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up --build
```

This starts:
| Container | Port | Description |
|---|---|---|
| transport-tracker-bff | 8080 | Spring Boot BFF |
| transport-tracker-wiremock | 8089 | Simulated transit APIs |
| transport-tracker-nginx | 80 | Reverse proxy |

### Run tests

```bash
cd backend/transport-tracker-bff
mvn test                      # unit + BDD tests
```

## Key Endpoints

| Method | URL | Description |
|---|---|---|
| GET | /api/v1/transit?city=NYC&route=M15 | Aggregated transit data |
| GET | /api/v1/transit/{routeId}/vehicles?city=NYC | Vehicle positions |
| GET | /api/v1/transit/{routeId}/alerts?city=NYC | Active alerts |
| GET | /api/v1/routes/plan?city=NYC&from=A&to=B | Route planning |
| GET | /api/v1/health | Liveness check |
| GET | /api/v1/health/ready | Readiness check |
| GET | /actuator/metrics | Micrometer metrics |
| GET | /actuator/prometheus | Prometheus scrape endpoint |

## Resilience Behaviour

| Condition | Response |
|---|---|
| API healthy | LIVE data, cached for 30s |
| API down, cache < 5min old | STALE data + warning header |
| API down, no cache | MOCK data + offline=true |
| Circuit OPEN | Skips API call, goes straight to cache/mock |

## Environment Variables

See `.env.template` for all configurable values.
API keys are never hardcoded — always injected via environment.
