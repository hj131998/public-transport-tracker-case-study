package com.tracker.client.proto;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * MTA publishes separate GTFS-RT feeds per subway line group.
 * This registry maps a route ID to the correct feed URL path.
 *
 * Free endpoint (no API key required):
 *   GET https://api-endpoint.mta.info/Dataservice/mtagtfsfeeds/nyct%2F{feedName}
 *
 * Feed reference:
 *   nyct%2Fgtfs       → lines 1,2,3,4,5,6,7,S
 *   nyct%2Fgtfs-ace   → lines A,C,E
 *   nyct%2Fgtfs-bdfm  → lines B,D,F,M
 *   nyct%2Fgtfs-nqrw  → lines N,Q,R,W
 *   nyct%2Fgtfs-l     → line L
 *   nyct%2Fgtfs-g     → line G
 *   nyct%2Fgtfs-jz    → lines J,Z
 *   nyct%2Fgtfs-si    → Staten Island Railway
 */
@Component
public class MtaFeedRegistry {

    private static final Map<String, Integer> ROUTE_TO_FEED = Map.ofEntries(
            Map.entry("1", 1),  Map.entry("2", 1),  Map.entry("3", 1),
            Map.entry("4", 1),  Map.entry("5", 1),  Map.entry("6", 1),
            Map.entry("7", 1),  Map.entry("S", 1),
            Map.entry("A", 16), Map.entry("C", 16), Map.entry("E", 16),
            Map.entry("B", 21), Map.entry("D", 21), Map.entry("F", 21), Map.entry("M", 21),
            Map.entry("N", 26), Map.entry("Q", 26), Map.entry("R", 26), Map.entry("W", 26),
            Map.entry("L", 31),
            Map.entry("G", 36),
            Map.entry("J", 51), Map.entry("Z", 51),
            Map.entry("SIR", 71)
    );

    /**
     * Returns the feed name segment for the given route ID.
     * This is the part after "nyct%2F" in the URL.
     *
     * @param routeId e.g. "A", "1", "F", "L"
     * @return feed name e.g. "gtfs-ace", "gtfs", "gtfs-l"
     */
    public String getFeedName(String routeId) {
        int feedId = Optional.ofNullable(ROUTE_TO_FEED.get(routeId.toUpperCase()))
                .orElse(1);
        return buildFeedName(feedId);
    }

    /**
     * Returns the full URL path segment for the MTA Dataservice endpoint.
     * Uses %2F (URL-encoded slash) as MTA expects it in the path.
     * Example: "nyct%2Fgtfs-ace"
     */
    public String getFeedPath(String routeId) {
        return "nyct%2F" + getFeedName(routeId);
    }

    /**
     * Returns the feed ID integer for a given route — useful for logging.
     */
    public int getFeedId(String routeId) {
        return Optional.ofNullable(ROUTE_TO_FEED.get(routeId.toUpperCase())).orElse(1);
    }

    /**
     * Returns true if the given route ID is a known NYC subway route.
     */
    public boolean isSubwayRoute(String routeId) {
        return ROUTE_TO_FEED.containsKey(routeId.toUpperCase());
    }

    private String buildFeedName(int feedId) {
        return switch (feedId) {
            case 1  -> "gtfs";
            case 16 -> "gtfs-ace";
            case 21 -> "gtfs-bdfm";
            case 26 -> "gtfs-nqrw";
            case 31 -> "gtfs-l";
            case 36 -> "gtfs-g";
            case 51 -> "gtfs-jz";
            case 71 -> "gtfs-si";
            default -> "gtfs";
        };
    }
}
