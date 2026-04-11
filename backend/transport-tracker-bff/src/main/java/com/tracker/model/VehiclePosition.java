package com.tracker.model;

import com.tracker.model.enums.CrowdingLevel;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VehiclePosition {
    String vehicleId;
    double lat;
    double lon;
    String nextStop;
    String eta;
    CrowdingLevel crowding;
    int delayMinutes;
    boolean disrupted;
}
