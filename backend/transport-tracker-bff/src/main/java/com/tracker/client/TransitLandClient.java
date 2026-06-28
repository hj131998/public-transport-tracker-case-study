package com.tracker.client;

import com.tracker.model.VehiclePosition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Transit.land data provider that reads from the pre-built route index
 * maintained by {@link TransitLandFeedFetcher}.
 *
 * This client does NOT make network calls on user requests.
 * Instead, it reads from the in-memory index that the background fetcher
 * populates every 60 seconds.
 *
 * Flow:
 *   1. TransitLandFeedFetcher fetches full city feed every 60s (background)
 *   2. Feed is parsed and indexed by routeId in memory
 *   3. This client reads from that index — O(1) per request, no network I/O
 *
 * This means:
 *   - First request after startup may return empty (feed not yet fetched)
 *   - Subsequent requests are instant (sub-millisecond)
 *   - Data is at most ~60-90 seconds old (Transit.land refresh rate)
 *   - 1000 concurrent users requesting different routes = ZERO additional API calls
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class TransitLandClient implements TransitDataProvider {

    private final TransitLandFeedFetcher feedFetcher;

    @Override
    public boolean supports(String city) {
        return feedFetcher.supportsCity(city);
    }

    @Override
    public List<VehiclePosition> fetchVehicles(String city, String routeId) {
        // Ensure feed is available (triggers on-demand fetch if stale/missing)
        if (!feedFetcher.hasFreshData(city)) {
            log.info("No fresh data for city={}, triggering on-demand fetch", city);
            feedFetcher.fetchOnDemand(city);
        }

        List<VehiclePosition> vehicles = feedFetcher.getVehiclesForRoute(city, routeId);
        log.info("TransitLandClient: city={} route={} → {} vehicles from index",
                city, routeId, vehicles.size());
        return vehicles;
    }
}
