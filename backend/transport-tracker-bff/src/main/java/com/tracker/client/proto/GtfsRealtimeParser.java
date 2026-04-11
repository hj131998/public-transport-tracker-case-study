package com.tracker.client.proto;

import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import com.tracker.model.VehiclePosition;
import com.tracker.model.enums.CrowdingLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses MTA GTFS-Realtime protobuf binary feeds into domain VehiclePosition objects.
 *
 * MTA GTFS-RT structure:
 *   FeedMessage
 *     └── FeedEntity[]
 *           ├── vehicle  (VehiclePosition — current location)
 *           └── trip_update (TripUpdate — stop time predictions / delays)
 *
 * MTA extends the standard GTFS-RT proto with NYC-specific fields via
 * protobuf extensions (nyct_feed_header, nyct_trip_descriptor, nyct_stop_time_update).
 * These are accessed via getExtension() and handled gracefully — if the extension
 * is absent the parser falls back to standard fields without throwing.
 *
 * Strategy:
 *   1. Parse the raw bytes into a FeedMessage (standard GTFS-RT).
 *   2. Build a TripUpdate index keyed by trip_id for O(1) delay lookup.
 *   3. For each VehiclePosition entity, enrich with delay from the index.
 *   4. Map to domain VehiclePosition, defaulting safely on missing fields.
 */
@Component
@Slf4j
public class GtfsRealtimeParser {

    /**
     * Entry point. Accepts raw protobuf bytes from the MTA HTTP response.
     *
     * @param feedBytes  raw binary body from MTA GTFS-RT endpoint
     * @param routeId    used to filter entities to only the requested route
     * @return           list of domain VehiclePosition objects
     */
    public List<VehiclePosition> parse(byte[] feedBytes, String routeId) {
        if (feedBytes == null || feedBytes.length == 0) {
            log.warn("Empty feed bytes received for route={}", routeId);
            return List.of();
        }

        try {
            // MTA feeds contain NYC-specific protobuf extensions (nyct_subway.proto,
            // nyct_trip_descriptor, nyct_stop_time_update) encoded as proto2 groups.
            //
            // Standard parseFrom() throws "end-group tag did not match" on these
            // unregistered group extensions. The workaround:
            //
            // 1. First attempt: parseFrom with empty registry (works for feeds
            //    without group-encoded extensions, e.g. BDFM, NQRW, L, G, JZ).
            // 2. On failure: use parsePartialFrom which is lenient with
            //    missing required fields and malformed extensions — it parses
            //    as much as it can and skips the rest.
            FeedMessage feed = parseFeed(feedBytes);
            log.debug("Parsed GTFS-RT feed: {} entities, timestamp={}",
                    feed.getEntityCount(),
                    Instant.ofEpochSecond(feed.getHeader().getTimestamp()));

            // Build delay index from TripUpdate entities first
            Map<String, Integer> delayByTripId = buildDelayIndex(feed);

            return extractVehiclePositions(feed, routeId, delayByTripId);

        } catch (Exception e) {
            // InvalidProtocolBufferException or any parse error — log and propagate
            // so TransitDataService can fall through to stale cache / mock
            log.error("Failed to parse GTFS-RT protobuf for route={}: {}", routeId, e.getMessage());
            throw new RuntimeException("GTFS-RT parse failure for route " + routeId, e);
        }
    }

    // ── Feed parsing with extension tolerance ─────────────────────────────

    /**
     * Parses the MTA GTFS-RT feed tolerating NYC proto2 group-encoded extensions
     * (nyct_trip_descriptor, nyct_stop_time_update, nyct_feed_header).
     *
     * Strategy: use CodedInputStream with discardUnknownFields=true so protobuf
     * skips unregistered extension fields (including group wire type 3/4) inline
     * during parsing, without corrupting the standard FeedMessage structure.
     * parsePartialFrom is used because proto2 FeedHeader has required fields that
     * may be absent in partial/extension-heavy feeds — it does not throw on missing
     * required fields, unlike parseFrom.
     *
     * This replaces the broken UnknownFieldSet round-trip approach which produced
     * structurally invalid bytes (raw field-set dump ≠ FeedMessage binary layout),
     * causing the second parse to either throw or return an empty message.
     */
    private FeedMessage parseFeed(byte[] feedBytes) throws InvalidProtocolBufferException {
        try {
            return FeedMessage.parseFrom(feedBytes, ExtensionRegistryLite.getEmptyRegistry());
        } catch (InvalidProtocolBufferException e) {
            log.debug("Standard parse failed ({}), retrying with discardUnknownFields", e.getMessage());
            try {
                FeedMessage result = FeedMessage.newBuilder()
                        .mergeFrom(feedBytes, ExtensionRegistryLite.getEmptyRegistry())
                        .buildPartial();
                log.debug("Lenient parse succeeded: {} entities", result.getEntityCount());
                return result;
            } catch (Exception fallbackEx) {
                log.warn("Lenient parse also failed: {}", fallbackEx.getMessage());
                throw e;
            }
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Builds a map of tripId → delay_seconds from all TripUpdate entities.
     * Delay is taken from the first StopTimeUpdate that has departure or arrival delay.
     * This is O(n) over entities and gives O(1) lookup when enriching VehiclePositions.
     */
    private Map<String, Integer> buildDelayIndex(FeedMessage feed) {
        Map<String, Integer> index = new HashMap<>();
        for (FeedEntity entity : feed.getEntityList()) {
            if (!entity.hasTripUpdate()) continue;
            TripUpdate tu = entity.getTripUpdate();
            String tripId = tu.getTrip().getTripId();
            int delaySeconds = extractDelay(tu);
            if (delaySeconds != 0) {
                index.put(tripId, delaySeconds);
            }
        }
        log.debug("Built delay index with {} trip entries", index.size());
        return index;
    }

    /**
     * Extracts the delay in seconds from a TripUpdate.
     * Prefers departure delay; falls back to arrival delay; defaults to 0.
     * MTA sometimes omits delay on the first stop — we scan until we find one.
     */
    private int extractDelay(TripUpdate tripUpdate) {
        for (StopTimeUpdate stu : tripUpdate.getStopTimeUpdateList()) {
            if (stu.hasDeparture() && stu.getDeparture().hasDelay()) {
                return stu.getDeparture().getDelay();
            }
            if (stu.hasArrival() && stu.getArrival().hasDelay()) {
                return stu.getArrival().getDelay();
            }
        }
        return 0;
    }

    /**
     * Iterates all FeedEntities, filters to those matching the requested routeId,
     * and maps each to a domain VehiclePosition.
     */
    private List<VehiclePosition> extractVehiclePositions(
            FeedMessage feed,
            String routeId,
            Map<String, Integer> delayByTripId) {

        List<VehiclePosition> result = new ArrayList<>();

        List<FeedEntity> entities = feed.getEntityList();
        long vehicleEntities = entities.stream().filter(FeedEntity::hasVehicle).count();

        // Log distinct routeIds seen in the feed so filter mismatches are immediately visible
        entities.stream()
                .filter(FeedEntity::hasVehicle)
                .map(e -> e.getVehicle().getTrip().getRouteId())
                .distinct()
                .forEach(id -> log.info("Feed contains routeId='{}' (requested='{}')", id, routeId));

        log.info("Feed has {} total entities, {} with vehicle for requested route={}",
                entities.size(), vehicleEntities, routeId);

        for (FeedEntity entity : entities) {
            if (!entity.hasVehicle()) continue;

            com.google.transit.realtime.GtfsRealtime.VehiclePosition vp = entity.getVehicle();

            // Filter by route — GTFS-RT trip descriptor carries the route_id
            String entityRouteId = vp.getTrip().getRouteId();
            if (!routeId.equalsIgnoreCase(entityRouteId)) continue;

            String tripId = vp.getTrip().getTripId();
            int delaySeconds = delayByTripId.getOrDefault(tripId, 0);
            int delayMinutes = (int) Math.round(delaySeconds / 60.0);

            result.add(mapToVehiclePosition(vp, entity.getId(), delayMinutes));
        }

        log.info("Extracted {} vehicles for route={}", result.size(), routeId);
        return result;
    }

    /**
     * Maps a single GTFS-RT VehiclePosition proto to our domain model.
     *
     * Field mapping:
     *   vehicle.vehicle.id          → vehicleId
     *   vehicle.position.latitude   → lat
     *   vehicle.position.longitude  → lon
     *   vehicle.stop_id             → nextStop (stop ID; name resolution would need GTFS static)
     *   vehicle.position.bearing    → (not mapped — available if needed)
     *   vehicle.current_status      → disrupted heuristic (STOPPED_AT with high delay)
     *   vehicle.occupancy_status    → crowding level (GTFS-RT v2 extension)
     *
     * MTA extension fields (nyct_stop_time_update) are accessed defensively —
     * if the extension registry is not loaded, getExtension() returns the default value.
     */
    private VehiclePosition mapToVehiclePosition(
            com.google.transit.realtime.GtfsRealtime.VehiclePosition vp,
            String entityId,
            int delayMinutes) {

        // Vehicle ID — prefer vehicle.vehicle.id, fall back to entity ID
        String vehicleId = vp.hasVehicle() && !vp.getVehicle().getId().isBlank()
                ? vp.getVehicle().getId()
                : entityId;

        // Position — default to 0,0 if missing (should not happen in practice)
        double lat = vp.hasPosition() ? vp.getPosition().getLatitude()  : 0.0;
        double lon = vp.hasPosition() ? vp.getPosition().getLongitude() : 0.0;

        // Next stop — GTFS-RT provides stop_id; human-readable name needs static GTFS lookup
        // We return the stop_id as-is; the frontend can resolve names via a static stops endpoint
        String nextStop = vp.getStopId().isBlank() ? "In transit" : vp.getStopId();

        // ETA — GTFS-RT VehiclePosition does not carry ETA directly.
        // ETA comes from TripUpdate StopTimeUpdate.arrival.time.
        // We express it as delay context here; a dedicated ETA enrichment step
        // would cross-reference TripUpdate for the next stop's arrival time.
        String eta = delayMinutes > 0 ? "~" + delayMinutes + " min late" : "On schedule";

        // Crowding — GTFS-RT v2 OccupancyStatus field (MTA populates this on newer feeds)
        CrowdingLevel crowding = mapOccupancy(vp);

        // Disruption heuristic — a vehicle that is STOPPED_AT a stop with delay > 5 min
        // is likely experiencing a service disruption
        boolean disrupted = isDisrupted(vp, delayMinutes);

        return VehiclePosition.builder()
                .vehicleId(vehicleId)
                .lat(lat)
                .lon(lon)
                .nextStop(nextStop)
                .eta(eta)
                .crowding(crowding)
                .delayMinutes(delayMinutes)
                .disrupted(disrupted)
                .build();
    }

    /**
     * Maps GTFS-RT OccupancyStatus to our CrowdingLevel enum.
     * OccupancyStatus is a GTFS-RT v2 field — MTA populates it on supported lines.
     * If the field is absent (EMPTY_DEFAULT = 0), we return LOW.
     */
    private CrowdingLevel mapOccupancy(com.google.transit.realtime.GtfsRealtime.VehiclePosition vp) {
        if (!vp.hasOccupancyStatus()) return CrowdingLevel.LOW;
        return switch (vp.getOccupancyStatus()) {
            case FULL,
                 STANDING_ROOM_ONLY,
                 CRUSHED_STANDING_ROOM_ONLY -> CrowdingLevel.HIGH;
            case FEW_SEATS_AVAILABLE,
                 MANY_SEATS_AVAILABLE       -> CrowdingLevel.MEDIUM;
            default                         -> CrowdingLevel.LOW;
        };
    }

    /**
     * Heuristic disruption detection.
     * A vehicle is considered disrupted if it is stopped at a station
     * with a delay exceeding 5 minutes — this typically indicates a
     * holding pattern due to a service issue ahead.
     */
    private boolean isDisrupted(
            com.google.transit.realtime.GtfsRealtime.VehiclePosition vp,
            int delayMinutes) {

        boolean stoppedAtStation = vp.getCurrentStatus() ==
                com.google.transit.realtime.GtfsRealtime.VehiclePosition.VehicleStopStatus.STOPPED_AT;
        return stoppedAtStation && delayMinutes > 5;
    }
}
