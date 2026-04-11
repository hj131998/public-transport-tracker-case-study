package com.tracker.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class RoutePlan {
    Route primaryRoute;
    @Singular
    List<Route> alternatives;
    int estimatedMinutes;
}
