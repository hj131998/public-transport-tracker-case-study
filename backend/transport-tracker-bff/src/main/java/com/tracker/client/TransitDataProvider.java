package com.tracker.client;

import com.tracker.model.VehiclePosition;

import java.util.List;

public interface TransitDataProvider {
    List<VehiclePosition> fetchVehicles(String city, String routeId);
    boolean supports(String city);
}
