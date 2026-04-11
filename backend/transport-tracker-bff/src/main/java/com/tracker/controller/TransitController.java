package com.tracker.controller;

import com.tracker.model.Alert;
import com.tracker.model.RoutePlan;
import com.tracker.model.TransitResponse;
import com.tracker.model.VehiclePosition;
import com.tracker.service.TransitAggregatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@Tag(name = "Transit", description = "Real-time public transport data")
public class TransitController {

    private final TransitAggregatorService aggregatorService;

    @GetMapping("/transit")
    @Operation(summary = "Get aggregated transit data for a city and route")
    public ResponseEntity<TransitResponse> getTransitData(
            @Parameter(description = "City code e.g. NYC") @RequestParam @NotBlank String city,
            @Parameter(description = "Route identifier e.g. M15") @RequestParam @NotBlank String route) {
        return ResponseEntity.ok(aggregatorService.aggregate(city, route));
    }

    @GetMapping("/transit/{routeId}/vehicles")
    @Operation(summary = "Get vehicle positions for a specific route")
    public ResponseEntity<List<VehiclePosition>> getVehicles(
            @PathVariable @NotBlank String routeId,
            @RequestParam @NotBlank String city) {
        return ResponseEntity.ok(aggregatorService.getVehicles(city, routeId));
    }

    @GetMapping("/transit/{routeId}/alerts")
    @Operation(summary = "Get active alerts for a specific route")
    public ResponseEntity<List<Alert>> getAlerts(
            @PathVariable @NotBlank String routeId,
            @RequestParam @NotBlank String city) {
        return ResponseEntity.ok(aggregatorService.getAlerts(city, routeId));
    }

    @GetMapping("/routes/plan")
    @Operation(summary = "Plan a route between two stops")
    public ResponseEntity<RoutePlan> planRoute(
            @RequestParam @NotBlank String city,
            @RequestParam @NotBlank String from,
            @RequestParam @NotBlank String to) {
        return ResponseEntity.ok(aggregatorService.planRoute(city, from, to));
    }

    @GetMapping("/routes/{routeId}/alternatives")
    @Operation(summary = "Get alternative routes for a given route")
    public ResponseEntity<RoutePlan> getAlternatives(
            @PathVariable @NotBlank String routeId,
            @RequestParam @NotBlank String city) {
        return ResponseEntity.ok(aggregatorService.planRoute(city, routeId, routeId + "-ALT"));
    }
}
