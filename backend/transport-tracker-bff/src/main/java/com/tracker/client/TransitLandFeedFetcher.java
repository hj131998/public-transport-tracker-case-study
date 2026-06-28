package com.tracker.client;

import com.tracker.model.VehiclePosition;
import com.tracker.model.enums.CrowdingLevel;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Background feed fetcher for Transit.land GTFS-RT data.
 *
 * Fetches ALL THREE feed types every 60 seconds:
 *   1. vehicle_positions — real GPS coordinates for active vehicles
 *   2. trip_updates     — per-stop arrival delays (enriches vehicles with delayMinutes)
 *   3. alerts           — agency-published service alerts (disruptions, detours)
 *
 * Data flow:
 *   - trip_updates are joined to vehicles by tripId → populates delayMinutes
 *   - Vehicles with delay > 5min at a stop are marked disrupted
 *   - Agency alerts are indexed by routeId for direct lookup
 *   - AlertService.evaluate() then fires conditional alerts based on enriched vehicle data
 */
@Slf4j
@Component
public class TransitLandFeedFetcher {

    private final WebClient webClient;
    private final int timeoutSeconds;
    private final com.tracker.client.proto.StopNameResolver stopNameResolver;

    /** Route-indexed vehicle positions: city → (routeId → vehicles) */
    private final ConcurrentHashMap<String, Map<String, List<VehiclePosition>>> cityVehicleIndex =
            new ConcurrentHashMap<>();

    /** Route-indexed agency alerts: city → (routeId → alert messages) */
    private final ConcurrentHashMap<String, Map<String, List<AgencyAlert>>> cityAlertIndex =
            new ConcurrentHashMap<>();

    /** Last fetch timestamps per city */
    private final ConcurrentHashMap<String, Instant> lastFetchTime = new ConcurrentHashMap<>();

    /** Maps city codes to Transit.land feed Onestop IDs */
    private static final Map<String, String> CITY_FEED_KEYS = Map.ofEntries(
            Map.entry("NYC", "f-mta~nyc~rt~bustime"),
            Map.entry("CHICAGO", "f-dp3-cta~rt"),
            Map.entry("PORTLAND", "f-c20-trimet~rt"),
            Map.entry("SF", "f-9q8y-sfmta~rt"),
            Map.entry("BOSTON", "f-drt-mbta~rt"),
            Map.entry("PHILLY", "f-dr4-septa~rt"),
            Map.entry("DC", "f-dqc-wmata~rt"),
            Map.entry("LA", "f-9q5-lacmta~rt")
    );

    /** NYC has a separate alerts feed */
    private static final Map<String, String> CITY_ALERT_FEED_KEYS = Map.ofEntries(
            Map.entry("NYC", "f-mta~nyc~rt~alerts")
    );

    private static final long FEED_MAX_AGE_SECONDS = 90;

    public TransitLandFeedFetcher(
            ReactorClientHttpConnector connector,
            @Value("${app.transit-api.transit-land.base-url}") String baseUrl,
            @Value("${app.transit-api.transit-land.api-key}") String apiKey,
            @Value("${app.transit-api.transit-land.timeout-seconds:10}") int timeoutSeconds,
            com.tracker.client.proto.StopNameResolver stopNameResolver) {
        this.timeoutSeconds = timeoutSeconds;
        this.stopNameResolver = stopNameResolver;
        this.webClient = WebClient.builder()
                .clientConnector(connector)
                .baseUrl(baseUrl)
                .defaultHeader("apikey", apiKey)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PUBLIC API — called by TransitLandClient (no network, instant lookup)
    // ═══════════════════════════════════════════════════════════════════════

    /** Get vehicles for a specific route (O(1) lookup) */
    public List<VehiclePosition> getVehiclesForRoute(String city, String routeId) {
        Map<String, List<VehiclePosition>> index = cityVehicleIndex.get(city.toUpperCase());
        if (index == null) return List.of();

        // Exact match
        List<VehiclePosition> vehicles = index.get(routeId);
        if (vehicles != null) return vehicles;

        // Case-insensitive
        vehicles = index.get(routeId.toUpperCase());
        if (vehicles != null) return vehicles;

        // Partial/prefix match (routes like "M15+" should match "M15")
        for (Map.Entry<String, List<VehiclePosition>> entry : index.entrySet()) {
            if (entry.getKey().toUpperCase().startsWith(routeId.toUpperCase())) {
                return entry.getValue();
            }
        }
        return List.of();
    }

    /** Get agency-published alerts for a route */
    public List<AgencyAlert> getAlertsForRoute(String city, String routeId) {
        Map<String, List<AgencyAlert>> index = cityAlertIndex.get(city.toUpperCase());
        if (index == null) return List.of();

        List<AgencyAlert> alerts = index.getOrDefault(routeId, List.of());
        if (!alerts.isEmpty()) return alerts;

        // Case-insensitive fallback
        for (Map.Entry<String, List<AgencyAlert>> entry : index.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(routeId)) return entry.getValue();
        }
        return List.of();
    }

    /** Get all available routes for a city */
    public List<String> getAvailableRoutes(String city) {
        Map<String, List<VehiclePosition>> index = cityVehicleIndex.get(city.toUpperCase());
        return index != null ? new ArrayList<>(index.keySet()) : List.of();
    }

    public boolean hasFreshData(String city) {
        Instant fetched = lastFetchTime.get(city.toUpperCase());
        return fetched != null && Duration.between(fetched, Instant.now()).getSeconds() < FEED_MAX_AGE_SECONDS;
    }

    public boolean supportsCity(String city) {
        return CITY_FEED_KEYS.containsKey(city.toUpperCase());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SCHEDULED BACKGROUND FETCH — runs every 60 seconds
    // ═══════════════════════════════════════════════════════════════════════

    @Scheduled(fixedDelay = 60_000, initialDelay = 5_000)
    public void fetchNycFeeds() {
        fetchAllFeeds("NYC");
    }

    /** On-demand fetch for cities not pre-scheduled */
    public void fetchOnDemand(String city) {
        if (!CITY_FEED_KEYS.containsKey(city.toUpperCase())) return;
        if (hasFreshData(city)) return;
        fetchAllFeeds(city);
    }

    /**
     * Fetches all three feeds for a city, joins the data, and indexes it.
     * Order: trip_updates first (for delay data), then vehicle_positions (enriched), then alerts.
     */
    @CircuitBreaker(name = "transitApi")
    @SuppressWarnings("unchecked")
    private void fetchAllFeeds(String city) {
        String feedKey = CITY_FEED_KEYS.get(city.toUpperCase());
        if (feedKey == null) return;

        long start = System.currentTimeMillis();
        log.info("Background fetch ALL feeds: city={} feedKey={}", city, feedKey);

        // 1. Fetch trip updates → build delay index (tripId → delaySeconds)
        Map<String, Integer> delayByTripId = fetchTripUpdates(feedKey, city);

        // 2. Fetch vehicle positions → enrich with delays → index by route
        Map<String, List<VehiclePosition>> vehicleIndex = fetchAndIndexVehicles(feedKey, city, delayByTripId);

        // 3. Fetch alerts → index by route
        String alertFeedKey = CITY_ALERT_FEED_KEYS.getOrDefault(city.toUpperCase(), feedKey);
        Map<String, List<AgencyAlert>> alertIndex = fetchAndIndexAlerts(alertFeedKey, city);

        // Store indexes
        if (vehicleIndex != null && !vehicleIndex.isEmpty()) {
            cityVehicleIndex.put(city.toUpperCase(), vehicleIndex);
        }
        if (alertIndex != null) {
            cityAlertIndex.put(city.toUpperCase(), alertIndex);
        }
        lastFetchTime.put(city.toUpperCase(), Instant.now());

        long elapsed = System.currentTimeMillis() - start;
        int totalVehicles = vehicleIndex != null ? vehicleIndex.values().stream().mapToInt(List::size).sum() : 0;
        int totalAlerts = alertIndex != null ? alertIndex.values().stream().mapToInt(List::size).sum() : 0;
        log.info("Background fetch complete: city={} routes={} vehicles={} delays={} alerts={} elapsed={}ms",
                city,
                vehicleIndex != null ? vehicleIndex.size() : 0,
                totalVehicles,
                delayByTripId.size(),
                totalAlerts,
                elapsed);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TRIP UPDATES — extracts delay per trip
    // ═══════════════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private Map<String, Integer> fetchTripUpdates(String feedKey, String city) {
        Map<String, Integer> delayByTripId = new HashMap<>();
        try {
            Map<String, Object> response = webClient.get()
                    .uri("/feeds/{feedKey}/download_latest_rt/trip_updates.json", feedKey)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();

            if (response == null) return delayByTripId;

            List<Map<String, Object>> entities =
                    (List<Map<String, Object>>) response.getOrDefault("entity", List.of());

            for (Map<String, Object> entity : entities) {
                Map<String, Object> tripUpdate = (Map<String, Object>) entity.get("tripUpdate");
                if (tripUpdate == null) tripUpdate = (Map<String, Object>) entity.get("trip_update");
                if (tripUpdate == null) continue;

                Map<String, Object> trip = (Map<String, Object>) tripUpdate.getOrDefault("trip", Map.of());
                String tripId = String.valueOf(trip.getOrDefault("tripId", trip.getOrDefault("trip_id", "")));
                if (tripId.isEmpty()) continue;

                // Extract delay from stop time updates
                int delaySec = extractDelayFromStopTimeUpdates(tripUpdate);
                if (delaySec != 0) {
                    delayByTripId.put(tripId, delaySec);
                }
            }
            log.debug("Trip updates: city={} trips_with_delay={}", city, delayByTripId.size());

        } catch (Exception e) {
            log.warn("Failed to fetch trip_updates for city={}: {}", city, e.getMessage());
        }
        return delayByTripId;
    }

    @SuppressWarnings("unchecked")
    private int extractDelayFromStopTimeUpdates(Map<String, Object> tripUpdate) {
        List<Map<String, Object>> stopTimeUpdates = (List<Map<String, Object>>)
                tripUpdate.getOrDefault("stopTimeUpdate",
                        tripUpdate.getOrDefault("stop_time_update", List.of()));

        // Take the delay from the first stop update that has one
        for (Map<String, Object> stu : stopTimeUpdates) {
            Map<String, Object> departure = (Map<String, Object>) stu.getOrDefault("departure", Map.of());
            Map<String, Object> arrival = (Map<String, Object>) stu.getOrDefault("arrival", Map.of());

            Object depDelay = departure.get("delay");
            if (depDelay != null) return toInt(depDelay);

            Object arrDelay = arrival.get("delay");
            if (arrDelay != null) return toInt(arrDelay);
        }
        return 0;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // VEHICLE POSITIONS — enriched with delay from trip updates
    // ═══════════════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private Map<String, List<VehiclePosition>> fetchAndIndexVehicles(
            String feedKey, String city, Map<String, Integer> delayByTripId) {

        Map<String, List<VehiclePosition>> index = new ConcurrentHashMap<>();
        try {
            Map<String, Object> response = webClient.get()
                    .uri("/feeds/{feedKey}/download_latest_rt/vehicle_positions.json", feedKey)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();

            if (response == null) return index;

            List<Map<String, Object>> entities =
                    (List<Map<String, Object>>) response.getOrDefault("entity", List.of());

            for (Map<String, Object> entity : entities) {
                Map<String, Object> vehicle = (Map<String, Object>) entity.get("vehicle");
                if (vehicle == null) continue;

                Map<String, Object> trip = (Map<String, Object>) vehicle.getOrDefault("trip", Map.of());
                String routeId = String.valueOf(trip.getOrDefault("routeId",
                        trip.getOrDefault("route_id", "")));
                if (routeId.isEmpty()) continue;

                Map<String, Object> position = (Map<String, Object>) vehicle.getOrDefault("position", Map.of());
                double lat = toDouble(position.get("latitude"));
                double lon = toDouble(position.get("longitude"));
                if (lat == 0.0 && lon == 0.0) continue;

                // Join delay from trip updates
                String tripId = String.valueOf(trip.getOrDefault("tripId",
                        trip.getOrDefault("trip_id", "")));
                int delaySec = delayByTripId.getOrDefault(tripId, 0);
                int delayMinutes = (int) Math.round(delaySec / 60.0);

                // Vehicle ID
                Map<String, Object> vehicleDesc = (Map<String, Object>) vehicle.getOrDefault("vehicle", Map.of());
                String vehicleId = String.valueOf(vehicleDesc.getOrDefault("id",
                        vehicleDesc.getOrDefault("label", entity.getOrDefault("id", "UNKNOWN"))));

                // Next stop
                String stopId = String.valueOf(vehicle.getOrDefault("stopId",
                        vehicle.getOrDefault("stop_id", "")));
                String nextStop = stopId.isEmpty() ? "In transit" : stopNameResolver.resolve(stopId);

                // ETA based on delay
                String eta = delayMinutes > 0
                        ? "~" + delayMinutes + " min late"
                        : buildEtaFromTimestamp(vehicle);

                // Crowding
                CrowdingLevel crowding = mapOccupancy(vehicle);

                // Disruption: stopped at a station with significant delay
                String status = String.valueOf(vehicle.getOrDefault("currentStatus",
                        vehicle.getOrDefault("current_status", "")));
                boolean disrupted = "STOPPED_AT".equalsIgnoreCase(status) && delayMinutes > 5;

                // Resolve next stop coordinates for map highlight
                double[] nextStopCoords = stopNameResolver.getCoordinates(stopId);
                double nextStopLat = nextStopCoords != null ? nextStopCoords[0] : 0.0;
                double nextStopLon = nextStopCoords != null ? nextStopCoords[1] : 0.0;

                VehiclePosition vp = VehiclePosition.builder()
                        .vehicleId(vehicleId)
                        .lat(lat)
                        .lon(lon)
                        .nextStop(nextStop)
                        .eta(eta)
                        .crowding(crowding)
                        .delayMinutes(delayMinutes)
                        .disrupted(disrupted)
                        .nextStopLat(nextStopLat)
                        .nextStopLon(nextStopLon)
                        .build();

                index.computeIfAbsent(routeId, k -> Collections.synchronizedList(new ArrayList<>()))
                        .add(vp);
            }

        } catch (Exception e) {
            log.warn("Failed to fetch vehicle_positions for city={}: {}", city, e.getMessage());
        }
        return index;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ALERTS — agency-published service disruptions
    // ═══════════════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private Map<String, List<AgencyAlert>> fetchAndIndexAlerts(String feedKey, String city) {
        Map<String, List<AgencyAlert>> index = new ConcurrentHashMap<>();
        try {
            Map<String, Object> response = webClient.get()
                    .uri("/feeds/{feedKey}/download_latest_rt/alerts.json", feedKey)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();

            if (response == null) return index;

            List<Map<String, Object>> entities =
                    (List<Map<String, Object>>) response.getOrDefault("entity", List.of());

            for (Map<String, Object> entity : entities) {
                Map<String, Object> alert = (Map<String, Object>) entity.get("alert");
                if (alert == null) continue;

                // Extract alert text
                String headerText = extractTranslationText(alert, "headerText", "header_text");
                String descText = extractTranslationText(alert, "descriptionText", "description_text");
                String message = !descText.isEmpty() ? descText : headerText;
                if (message.isEmpty()) continue;

                // Extract affected routes
                List<Map<String, Object>> informedEntities = (List<Map<String, Object>>)
                        alert.getOrDefault("informedEntity", alert.getOrDefault("informed_entity", List.of()));

                Set<String> affectedRoutes = new HashSet<>();
                for (Map<String, Object> ie : informedEntities) {
                    String routeId = String.valueOf(ie.getOrDefault("routeId",
                            ie.getOrDefault("route_id", "")));
                    if (!routeId.isEmpty()) affectedRoutes.add(routeId);
                }

                // Build alert record
                AgencyAlert agencyAlert = new AgencyAlert(
                        message,
                        headerText,
                        Instant.now()
                );

                // Index by each affected route
                for (String routeId : affectedRoutes) {
                    index.computeIfAbsent(routeId, k -> Collections.synchronizedList(new ArrayList<>()))
                            .add(agencyAlert);
                }

                // If no specific routes, store under "__ALL__" key
                if (affectedRoutes.isEmpty()) {
                    index.computeIfAbsent("__ALL__", k -> Collections.synchronizedList(new ArrayList<>()))
                            .add(agencyAlert);
                }
            }
            log.debug("Alerts: city={} routes_with_alerts={} total_alerts={}",
                    city, index.size(), index.values().stream().mapToInt(List::size).sum());

        } catch (Exception e) {
            log.warn("Failed to fetch alerts for city={}: {}", city, e.getMessage());
        }
        return index;
    }

    @SuppressWarnings("unchecked")
    private String extractTranslationText(Map<String, Object> alert, String... keys) {
        for (String key : keys) {
            Object textObj = alert.get(key);
            if (textObj instanceof Map) {
                List<Map<String, Object>> translations =
                        (List<Map<String, Object>>) ((Map<String, Object>) textObj).getOrDefault("translation", List.of());
                if (!translations.isEmpty()) {
                    String text = String.valueOf(translations.get(0).getOrDefault("text", ""));
                    if (!text.isEmpty()) return text;
                }
            }
        }
        return "";
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private String buildEtaFromTimestamp(Map<String, Object> vehicle) {
        Object tsObj = vehicle.getOrDefault("timestamp", null);
        if (tsObj != null) {
            try {
                long epoch = Long.parseLong(String.valueOf(tsObj));
                long age = Instant.now().getEpochSecond() - epoch;
                String status = String.valueOf(vehicle.getOrDefault("currentStatus",
                        vehicle.getOrDefault("current_status", "IN_TRANSIT_TO")));
                if (age < 120) return "Live (" + status.replace("_", " ").toLowerCase() + ")";
                return "~" + (age / 60) + " min ago";
            } catch (NumberFormatException ignored) {}
        }
        return "On schedule";
    }

    private CrowdingLevel mapOccupancy(Map<String, Object> vehicle) {
        String occ = String.valueOf(vehicle.getOrDefault("occupancyStatus",
                vehicle.getOrDefault("occupancy_status", ""))).toUpperCase();
        return switch (occ) {
            case "FULL", "STANDING_ROOM_ONLY", "CRUSHED_STANDING_ROOM_ONLY" -> CrowdingLevel.HIGH;
            case "FEW_SEATS_AVAILABLE" -> CrowdingLevel.MEDIUM;
            default -> CrowdingLevel.LOW;
        };
    }

    private double toDouble(Object val) {
        if (val == null) return 0.0;
        return val instanceof Number n ? n.doubleValue() : Double.parseDouble(String.valueOf(val));
    }

    private int toInt(Object val) {
        if (val == null) return 0;
        return val instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(val));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // INNER TYPES
    // ═══════════════════════════════════════════════════════════════════════

    /** Represents a real service alert from the transit agency */
    public record AgencyAlert(
            String message,
            String header,
            Instant fetchedAt
    ) {}
}
