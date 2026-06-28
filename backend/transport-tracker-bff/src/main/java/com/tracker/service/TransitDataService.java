package com.tracker.service;

import com.tracker.cache.CacheService;
import com.tracker.client.MockDataProvider;
import com.tracker.client.MtaApiClient;
import com.tracker.client.TransitLandFeedFetcher;
import com.tracker.config.AppConfig;
import com.tracker.model.VehiclePosition;
import com.tracker.model.enums.DataSource;
import com.tracker.model.enums.TransitMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransitDataService {

    private final MtaApiClient mtaApiClient;
    private final TransitLandFeedFetcher feedFetcher;
    private final CacheService cacheService;
    private final MockDataProvider mockDataProvider;
    private final AppConfig appConfig;

    private static final String CACHE_KEY_PREFIX = "transit:vehicles:";

    /**
     * Fetches vehicles using direct dispatch based on transit mode.
     * No provider chain or ordering — mode explicitly selects the data source.
     */
    @SuppressWarnings("unchecked")
    public FetchResult fetchVehicles(String city, String routeId, TransitMode mode) {
        String modeStr = appConfig.getDataSourceMode().toUpperCase();
        String cacheKey = CACHE_KEY_PREFIX + city + ":" + routeId + ":" + mode;

        log.debug("Fetching vehicles city={} route={} mode={} dataSourceMode={}", city, routeId, mode, modeStr);

        return switch (modeStr) {
            case "MOCK" -> serveMock(city, routeId);
            case "CACHED" -> serveCachedOnly(cacheKey, city, routeId);
            case "STALE" -> serveStaleOnly(cacheKey, city, routeId);
            default -> serveLive(cacheKey, city, routeId, mode);
        };
    }

    /**
     * Backward-compatible overload — defaults to BUS mode.
     */
    public FetchResult fetchVehicles(String city, String routeId) {
        return fetchVehicles(city, routeId, TransitMode.BUS);
    }

    /**
     * LIVE mode — direct dispatch by transit mode:
     *   BUS    → TransitLandFeedFetcher (pre-indexed, instant)
     *   SUBWAY → MtaApiClient (protobuf, network call)
     *
     * Fallback chain: cache → stale cache → mock
     */
    @SuppressWarnings("unchecked")
    private FetchResult serveLive(String cacheKey, String city, String routeId, TransitMode mode) {
        // 1. Try fresh cache
        var cached = cacheService.get(cacheKey, List.class);
        if (cached.isPresent()) {
            log.info("Cache HIT city={} route={} mode={}", city, routeId, mode);
            return new FetchResult((List<VehiclePosition>) cached.get(), DataSource.CACHED, false, null);
        }

        // 2. Fetch from the correct source based on mode
        try {
            List<VehiclePosition> vehicles = switch (mode) {
                case BUS -> fetchFromTransitLand(city, routeId);
                case SUBWAY -> fetchFromMta(city, routeId);
            };

            if (!vehicles.isEmpty()) {
                cacheService.put(cacheKey, vehicles);
                log.info("LIVE success mode={} city={} route={} vehicles={}", mode, city, routeId, vehicles.size());
                return new FetchResult(vehicles, DataSource.LIVE, false, null);
            }
            log.info("Source returned empty for mode={} city={} route={}", mode, city, routeId);
        } catch (Exception ex) {
            log.warn("LIVE fetch failed mode={} city={} route={}: {}", mode, city, routeId, ex.getMessage());
        }

        // 3. Try stale cache
        var stale = cacheService.getStale(cacheKey, List.class);
        if (stale.isPresent()) {
            log.warn("Serving STALE cache city={} route={}", city, routeId);
            return new FetchResult(
                    (List<VehiclePosition>) stale.get(),
                    DataSource.STALE, false,
                    "Data may be outdated - live feed unavailable");
        }

        // 4. Mock fallback
        log.error("All sources exhausted, serving MOCK city={} route={}", city, routeId);
        return serveMock(city, routeId);
    }

    private List<VehiclePosition> fetchFromTransitLand(String city, String routeId) {
        if (!feedFetcher.hasFreshData(city)) {
            feedFetcher.fetchOnDemand(city);
        }
        return feedFetcher.getVehiclesForRoute(city, routeId);
    }

    private List<VehiclePosition> fetchFromMta(String city, String routeId) {
        return mtaApiClient.fetchVehicles(city, routeId);
    }

    @SuppressWarnings("unchecked")
    private FetchResult serveCachedOnly(String cacheKey, String city, String routeId) {
        var cached = cacheService.get(cacheKey, List.class);
        if (cached.isPresent()) {
            return new FetchResult((List<VehiclePosition>) cached.get(), DataSource.CACHED, false, null);
        }
        return serveMock(city, routeId);
    }

    @SuppressWarnings("unchecked")
    private FetchResult serveStaleOnly(String cacheKey, String city, String routeId) {
        var stale = cacheService.getStale(cacheKey, List.class);
        if (stale.isPresent()) {
            return new FetchResult(
                    (List<VehiclePosition>) stale.get(), DataSource.STALE, false,
                    "Stale mode - showing previously cached data");
        }
        return serveMock(city, routeId);
    }

    private FetchResult serveMock(String city, String routeId) {
        return new FetchResult(
                mockDataProvider.getMockVehicles(city, routeId),
                DataSource.MOCK, true,
                "Mock mode - showing sample data");
    }

    public record FetchResult(
            List<VehiclePosition> vehicles,
            DataSource dataSource,
            boolean offline,
            String warning
    ) {}
}
