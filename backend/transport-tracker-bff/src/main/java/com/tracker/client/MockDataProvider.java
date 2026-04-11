package com.tracker.client;

import com.tracker.model.Alert;
import com.tracker.model.Route;
import com.tracker.model.RoutePlan;
import com.tracker.model.Stop;
import com.tracker.model.VehiclePosition;
import com.tracker.model.enums.AlertType;
import com.tracker.model.enums.CrowdingLevel;
import com.tracker.model.enums.Severity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MockDataProvider {

    public List<VehiclePosition> getMockVehicles(String city, String routeId) {
        return List.of(
                VehiclePosition.builder()
                        .vehicleId("MOCK-V001")
                        .lat(40.7128).lon(-74.0060)
                        .nextStop("Central Station")
                        .eta("5 min")
                        .crowding(CrowdingLevel.MEDIUM)
                        .delayMinutes(3)
                        .disrupted(false)
                        .build(),
                VehiclePosition.builder()
                        .vehicleId("MOCK-V002")
                        .lat(40.7580).lon(-73.9855)
                        .nextStop("North Terminal")
                        .eta("12 min")
                        .crowding(CrowdingLevel.LOW)
                        .delayMinutes(0)
                        .disrupted(false)
                        .build()
        );
    }

    public List<Alert> getMockAlerts() {
        return List.of(
                Alert.builder()
                        .type(AlertType.DISRUPTION)
                        .severity(Severity.MEDIUM)
                        .message("Offline mode - showing cached schedule data")
                        .build()
        );
    }

    public RoutePlan getMockRoutePlan(String from, String to) {
        Stop stopA = Stop.builder().stopId("S1").name(from).lat(40.71).lon(-74.00).eta("Now").build();
        Stop stopB = Stop.builder().stopId("S2").name("Mid Point").lat(40.73).lon(-73.99).eta("8 min").build();
        Stop stopC = Stop.builder().stopId("S3").name(to).lat(40.75).lon(-73.98).eta("15 min").build();

        Route primary = Route.builder()
                .routeId("MOCK-R1")
                .stop(stopA).stop(stopB).stop(stopC)
                .durationMinutes(15)
                .hasDisruption(false)
                .build();

        return RoutePlan.builder()
                .primaryRoute(primary)
                .estimatedMinutes(15)
                .build();
    }
}
