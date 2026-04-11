package com.decoder.model;

public record VehiclePosition(
        String entityId,
        String routeId,
        String tripId,
        float  latitude,
        float  longitude,
        float  bearing,
        String status,
        long   timestamp
) {}
