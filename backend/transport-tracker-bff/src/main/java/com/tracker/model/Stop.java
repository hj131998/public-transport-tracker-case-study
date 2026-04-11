package com.tracker.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Stop {
    String stopId;
    String name;
    double lat;
    double lon;
    String eta;
}
