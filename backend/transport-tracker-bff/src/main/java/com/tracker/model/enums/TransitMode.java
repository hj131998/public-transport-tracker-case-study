package com.tracker.model.enums;

/**
 * Identifies the type of transit vehicle the user is interested in.
 * Used to route requests to the correct data source:
 *   BUS    → Transit.land feed (real GPS positions)
 *   SUBWAY → MTA GTFS-RT protobuf feed (trip updates, delays, no GPS)
 */
public enum TransitMode {
    BUS,
    SUBWAY
}
