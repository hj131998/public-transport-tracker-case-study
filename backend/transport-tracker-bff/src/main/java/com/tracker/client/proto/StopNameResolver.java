package com.tracker.client.proto;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolves MTA subway stop IDs to human-readable station names.
 *
 * Loads from the static GTFS stops.txt file (MTA NYC subway) at startup.
 * The file maps IDs like "L06N" to names like "Union Sq - 14 St".
 *
 * MTA stop ID format:
 *   - "101"  = parent station (location_type=1)
 *   - "101N" = northbound platform (child of 101)
 *   - "101S" = southbound platform (child of 101)
 *
 * Source: http://web.mta.info/developers/data/nyct/subway/google_transit.zip
 */
@Slf4j
@Component
public class StopNameResolver {

    private final Map<String, String> stopNames = new HashMap<>();
    private final Map<String, double[]> stopCoords = new HashMap<>();

    @PostConstruct
    public void init() {
        loadStops("/gtfs-static/stops.txt"); // Subway stops
        // Bus stops — all 5 NYC boroughs
        loadStops("/gtfs-static/bus/stops_manhattan.txt");
        loadStops("/gtfs-static/bus/stops_bronx.txt");
        loadStops("/gtfs-static/bus/stops_brooklyn.txt");
        loadStops("/gtfs-static/bus/stops_queens.txt");
        loadStops("/gtfs-static/bus/stops_staten_island.txt");
        log.info("StopNameResolver ready: {} total stops loaded", stopNames.size());
    }

    /**
     * Resolves a stop ID to its human-readable name.
     * Falls back to the raw ID if not found.
     */
    public String resolve(String stopId) {
        if (stopId == null || stopId.isBlank()) return "Unknown";
        String name = stopNames.get(stopId);
        if (name != null) return name;

        // Try without direction suffix (L06N → L06)
        if (stopId.length() > 1) {
            String base = stopId.substring(0, stopId.length() - 1);
            name = stopNames.get(base);
            if (name != null) return name;
        }
        return stopId; // fallback to raw ID
    }

    /**
     * Returns lat/lon for a stop ID, or null if not found.
     */
    public double[] getCoordinates(String stopId) {
        double[] coords = stopCoords.get(stopId);
        if (coords != null) return coords;

        // Try without direction suffix
        if (stopId != null && stopId.length() > 1) {
            return stopCoords.get(stopId.substring(0, stopId.length() - 1));
        }
        return null;
    }

    /**
     * Returns the direction label based on stop ID suffix.
     */
    public String getDirection(String stopId) {
        if (stopId == null || stopId.isEmpty()) return "";
        char last = stopId.charAt(stopId.length() - 1);
        return switch (last) {
            case 'N' -> "Uptown / Bronx-bound";
            case 'S' -> "Downtown / Brooklyn-bound";
            default -> "";
        };
    }

    public int getStopCount() {
        return stopNames.size();
    }

    private void loadStops(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                log.warn("{} not found on classpath — skipping", resourcePath);
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                // Parse CSV header to find column indices
                String header = reader.readLine();
                if (header == null) return;

                String[] columns = header.split(",");
                int idIdx = -1, nameIdx = -1, latIdx = -1, lonIdx = -1;
                for (int i = 0; i < columns.length; i++) {
                    switch (columns[i].trim()) {
                        case "stop_id" -> idIdx = i;
                        case "stop_name" -> nameIdx = i;
                        case "stop_lat" -> latIdx = i;
                        case "stop_lon" -> lonIdx = i;
                    }
                }

                if (idIdx == -1 || nameIdx == -1) {
                    log.warn("stops.txt missing required columns (stop_id, stop_name)");
                    return;
                }

                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",", -1);
                    if (parts.length <= Math.max(idIdx, nameIdx)) continue;

                    String stopId = parts[idIdx].trim();
                    String stopName = parts[nameIdx].trim();

                    if (!stopId.isEmpty() && !stopName.isEmpty()) {
                        stopNames.put(stopId, stopName);

                        if (latIdx >= 0 && lonIdx >= 0 && parts.length > Math.max(latIdx, lonIdx)) {
                            try {
                                double lat = Double.parseDouble(parts[latIdx].trim());
                                double lon = Double.parseDouble(parts[lonIdx].trim());
                                if (lat != 0.0 && lon != 0.0) {
                                    stopCoords.put(stopId, new double[]{lat, lon});
                                }
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }

            log.info("Loaded stops from {}: {} names, {} coords (total: {})",
                    resourcePath,
                    stopNames.size() - stopNames.size() + stopNames.size(), // just show total
                    stopCoords.size(),
                    stopNames.size());

        } catch (Exception e) {
            log.error("Failed to load {}: {}", resourcePath, e.getMessage());
        }
    }
}
