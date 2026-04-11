package com.tracker.service;

import com.tracker.cache.CacheService;
import com.tracker.client.MockDataProvider;
import com.tracker.client.TransitDataProvider;
import com.tracker.config.AppConfig;
import com.tracker.model.VehiclePosition;
import com.tracker.model.enums.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransitDataService {

    private final List<TransitDataProvider> providers;
    private final CacheService cacheService;
    private final MockDataProvider mockDataProvider;
    private final AppConfig appConfig;

    private static final String CACHE_KEY_PREFIX = "transit:vehicles:";

    @SuppressWarnings("unchecked")
    public FetchResult fetchVehicles(String city, String routeId) {
        String mode = appConfig.getDataSourceMode().toUpperCase();
        String cacheKey = CACHE_KEY_PREFIX + city + ":" + routeId;

        log.debug("Fetching vehicles city={} route={} mode={}", city, routeId, mode);

        return switch (mode) {
            case "MOCK" -> serveMock(city, routeId);
            case "CACHED" -> serveCachedOnly(cacheKey, city, routeId);
            case "STALE" -> serveStaleOnly(cacheKey, city, routeId);
            default -> serveLive(cacheKey, city, routeId); // LIVE or any unrecognised value
        };
    }

    /**
     * LIVE mode — the full fallback chain:
     *   1. Fresh cache (< TTL)
     *   2. External API call → cache the result
     *   3. Stale cache (< stale TTL)
     *   4. Mock data (last resort)
     */
    @SuppressWarnings("unchecked")
    private FetchResult serveLive(String cacheKey, String city, String routeId) {
        // 1. Try fresh cache
        var cached = cacheService.get(cacheKey, List.class);
        if (cached.isPresent()) {
            log.info("Cache HIT city={} route={}", city, routeId);
            return new FetchResult((List<VehiclePosition>) cached.get(), DataSource.CACHED, false, null);
        }

        // 2. Call external API
        try {
            TransitDataProvider provider = resolveProvider(city);
            List<VehiclePosition> vehicles = provider.fetchVehicles(city, routeId);
            cacheService.put(cacheKey, vehicles);
            log.info("LIVE API success city={} route={} vehicles={}", city, routeId, vehicles.size());
            return new FetchResult(vehicles, DataSource.LIVE, false, null);
        } catch (Exception ex) {
            log.warn("LIVE API failed city={} route={}: {}", city, routeId, ex.getMessage());
        }

        // 3. Try stale cache
        var stale = cacheService.getStale(cacheKey, List.class);
        if (stale.isPresent()) {
            log.warn("Serving STALE cache city={} route={}", city, routeId);
            return new FetchResult(
                    (List<VehiclePosition>) stale.get(),
                    DataSource.STALE,
                    false,
                    "Data may be outdated - live feed unavailable"
            );
        }

        // 4. Final fallback: mock
        log.error("All sources exhausted, serving MOCK city={} route={}", city, routeId);
        return serveMock(city, routeId);
    }

    /**
     * CACHED mode — serve from fresh cache only, never call external API.
     * Falls back to mock if cache is empty.
     */
    @SuppressWarnings("unchecked")
    private FetchResult serveCachedOnly(String cacheKey, String city, String routeId) {
        var cached = cacheService.get(cacheKey, List.class);
        if (cached.isPresent()) {
            log.info("CACHED mode: serving fresh cache city={} route={}", city, routeId);
            return new FetchResult((List<VehiclePosition>) cached.get(), DataSource.CACHED, false, null);
        }
        log.warn("CACHED mode: no fresh cache available, falling back to mock city={} route={}", city, routeId);
        return serveMock(city, routeId);
    }

    /**
     * STALE mode — serve from stale cache only (for testing degradation).
     * Falls back to mock if no stale data exists.
     */
    @SuppressWarnings("unchecked")
    private FetchResult serveStaleOnly(String cacheKey, String city, String routeId) {
        var stale = cacheService.getStale(cacheKey, List.class);
        if (stale.isPresent()) {
            log.info("STALE mode: serving stale cache city={} route={}", city, routeId);
            return new FetchResult(
                    (List<VehiclePosition>) stale.get(),
                    DataSource.STALE,
                    false,
                    "Stale mode - showing previously cached data"
            );
        }
        log.warn("STALE mode: no stale cache available, falling back to mock city={} route={}", city, routeId);
        return serveMock(city, routeId);
    }

    /**
     * MOCK mode — always return deterministic mock data, skip API and cache.
     */
    private FetchResult serveMock(String city, String routeId) {
        log.info("MOCK mode: serving mock data city={} route={}", city, routeId);
        return new FetchResult(
                mockDataProvider.getMockVehicles(city, routeId),
                DataSource.MOCK,
                true,
                "Mock mode - showing sample data"
        );
    }

    private TransitDataProvider resolveProvider(String city) {
        return providers.stream()
                .filter(p -> p.supports(city))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No provider found for city: " + city));
    }

    public record FetchResult(
            List<VehiclePosition> vehicles,
            DataSource dataSource,
            boolean offline,
            String warning
    ) {}
}
