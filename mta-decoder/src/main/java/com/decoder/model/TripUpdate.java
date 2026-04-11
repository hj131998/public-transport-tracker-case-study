package com.decoder.model;

import java.util.List;

public record TripUpdate(
        String entityId,
        String tripId,
        String routeId,
        String startDate,
        List<StopTimeUpdate> stopTimeUpdates
) {
    public record StopTimeUpdate(
            String stopId,
            long   arrivalTime,
            int    arrivalDelay,
            long   departureTime,
            int    departureDelay
    ) {}
}
