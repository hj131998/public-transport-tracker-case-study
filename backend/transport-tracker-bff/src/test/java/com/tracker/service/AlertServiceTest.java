package com.tracker.service;

import com.tracker.model.Alert;
import com.tracker.model.VehiclePosition;
import com.tracker.model.enums.AlertType;
import com.tracker.model.enums.CrowdingLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AlertServiceTest {

    private AlertService alertService;

    @BeforeEach
    void setUp() {
        alertService = new AlertService();
    }

    @Test
    @DisplayName("Should generate DELAY alert when delay exceeds 15 minutes")
    void shouldGenerateDelayAlert() {
        List<VehiclePosition> vehicles = List.of(vehicle(20, CrowdingLevel.LOW, false));
        List<Alert> alerts = alertService.evaluate(vehicles);
        assertThat(alerts).anyMatch(a -> a.getType() == AlertType.DELAY);
    }

    @Test
    @DisplayName("Should NOT generate DELAY alert when delay is exactly 15 minutes")
    void shouldNotGenerateDelayAlertAtBoundary() {
        List<VehiclePosition> vehicles = List.of(vehicle(15, CrowdingLevel.LOW, false));
        List<Alert> alerts = alertService.evaluate(vehicles);
        assertThat(alerts).noneMatch(a -> a.getType() == AlertType.DELAY);
    }

    @Test
    @DisplayName("Should generate DISRUPTION alert when any vehicle is disrupted")
    void shouldGenerateDisruptionAlert() {
        List<VehiclePosition> vehicles = List.of(vehicle(0, CrowdingLevel.LOW, true));
        List<Alert> alerts = alertService.evaluate(vehicles);
        assertThat(alerts).anyMatch(a -> a.getType() == AlertType.DISRUPTION);
    }

    @Test
    @DisplayName("Should generate CROWDING alert when any vehicle has HIGH crowding")
    void shouldGenerateCrowdingAlert() {
        List<VehiclePosition> vehicles = List.of(vehicle(0, CrowdingLevel.HIGH, false));
        List<Alert> alerts = alertService.evaluate(vehicles);
        assertThat(alerts).anyMatch(a -> a.getType() == AlertType.CROWDING);
    }

    @Test
    @DisplayName("Should generate WEATHER alert when weather impact flag is true")
    void shouldGenerateWeatherAlert() {
        List<VehiclePosition> vehicles = List.of(vehicle(0, CrowdingLevel.LOW, false));
        List<Alert> alerts = alertService.evaluate(vehicles, true);
        assertThat(alerts).anyMatch(a -> a.getType() == AlertType.WEATHER);
    }

    @Test
    @DisplayName("Should generate multiple alerts when multiple conditions are met")
    void shouldGenerateMultipleAlerts() {
        List<VehiclePosition> vehicles = List.of(vehicle(20, CrowdingLevel.HIGH, true));
        List<Alert> alerts = alertService.evaluate(vehicles, true);
        assertThat(alerts).hasSize(4);
    }

    @Test
    @DisplayName("Should return empty alerts for healthy vehicles")
    void shouldReturnNoAlertsForHealthyVehicles() {
        List<VehiclePosition> vehicles = List.of(vehicle(0, CrowdingLevel.LOW, false));
        List<Alert> alerts = alertService.evaluate(vehicles);
        assertThat(alerts).isEmpty();
    }

    @Test
    @DisplayName("Should return empty alerts for empty vehicle list")
    void shouldHandleEmptyVehicleList() {
        List<Alert> alerts = alertService.evaluate(List.of());
        assertThat(alerts).isEmpty();
    }

    private VehiclePosition vehicle(int delay, CrowdingLevel crowding, boolean disrupted) {
        return VehiclePosition.builder()
                .vehicleId("V-TEST")
                .lat(0).lon(0)
                .nextStop("Test Stop")
                .eta("5 min")
                .crowding(crowding)
                .delayMinutes(delay)
                .disrupted(disrupted)
                .build();
    }
}
