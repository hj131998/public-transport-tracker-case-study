package com.tracker.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class Route {
    String routeId;
    @Singular
    List<Stop> stops;
    int durationMinutes;
    boolean hasDisruption;
}
