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

import java.time.Instant;
import java.util.List;

@Component
public class MockDataProvider {

    public List<VehiclePosition> getMockVehicles(String city, String routeId) {
        return List.of(
                VehiclePosition.builder()
                        .vehicleId("MOCK-V001")
                        .lat(40.7359).lon(-73.9911)
                        .nextStop("14th St - Union Sq")
                        .eta("5 min")
                        .crowding(CrowdingLevel.MEDIUM)
                        .delayMinutes(3)
                        .disrupted(false)
                        .build(),
                VehiclePosition.builder()
                        .vehicleId("MOCK-V002")
                        .lat(40.7484).lon(-73.9879)
                        .nextStop("34th St - Herald Sq")
                        .eta("12 min")
                        .crowding(CrowdingLevel.LOW)
                        .delayMinutes(0)
                        .disrupted(false)
                        .build(),
                VehiclePosition.builder()
                        .vehicleId("MOCK-V003")
                        .lat(40.7627).lon(-73.9809)
                        .nextStop("Times Sq - 42nd St")
                        .eta("8 min")
                        .crowding(CrowdingLevel.HIGH)
                        .delayMinutes(7)
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
                        .generatedAt(Instant.now())
                        .build()
        );
    }

    public RoutePlan getMockRoutePlan(String from, String to) {
        Stop stopA = Stop.builder().stopId("S1").name(from).lat(40.7359).lon(-73.9911).eta("Now").build();
        Stop stopB = Stop.builder().stopId("S2").name("14th St - Union Sq").lat(40.7359).lon(-73.9903).eta("8 min").build();
        Stop stopC = Stop.builder().stopId("S3").name(to).lat(40.7484).lon(-73.9879).eta("15 min").build();

        Route primary = Route.builder()
                .routeId("MOCK-R1")
                .stop(stopA).stop(stopB).stop(stopC)
                .durationMinutes(15)
                .hasDisruption(false)
                .build();

        Stop altStopA = Stop.builder().stopId("S4").name(from).lat(40.7359).lon(-73.9911).eta("Now").build();
        Stop altStopB = Stop.builder().stopId("S5").name("23rd St").lat(40.7410).lon(-73.9896).eta("10 min").build();
        Stop altStopC = Stop.builder().stopId("S6").name(to).lat(40.7484).lon(-73.9879).eta("18 min").build();

        Route alternative = Route.builder()
                .routeId("MOCK-R2")
                .stop(altStopA).stop(altStopB).stop(altStopC)
                .durationMinutes(18)
                .hasDisruption(false)
                .build();

        return RoutePlan.builder()
                .primaryRoute(primary)
                .alternative(alternative)
                .estimatedMinutes(15)
                .build();
    }
}
