package com.tracker.service;

import com.tracker.model.Route;
import com.tracker.model.RoutePlan;
import com.tracker.model.Stop;
import com.tracker.model.VehiclePosition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class RoutePlannerService {

    /**
     * Builds a RoutePlan for the given origin/destination using live vehicle data
     * to determine disruptions and estimate journey time.
     */
    public RoutePlan plan(String from, String to, List<VehiclePosition> liveVehicles) {
        log.info("Planning route from={} to={}", from, to);

        boolean routeDisrupted = liveVehicles.stream().anyMatch(VehiclePosition::isDisrupted);
        int estimatedMinutes = estimateJourneyTime(liveVehicles);

        Route primary = buildRoute(from, to, "R-PRIMARY", estimatedMinutes, routeDisrupted);
        List<Route> alternatives = buildAlternatives(from, to, routeDisrupted);

        return RoutePlan.builder()
                .primaryRoute(primary)
                .alternatives(alternatives)
                .estimatedMinutes(estimatedMinutes)
                .build();
    }

    private Route buildRoute(String from, String to, String routeId, int durationMinutes, boolean disrupted) {
        Stop origin = Stop.builder()
                .stopId(routeId + "-S1")
                .name(from)
                .lat(0.0).lon(0.0)
                .eta("Now")
                .build();

        Stop midpoint = Stop.builder()
                .stopId(routeId + "-S2")
                .name("Transfer Point")
                .lat(0.0).lon(0.0)
                .eta(durationMinutes / 2 + " min")
                .build();

        Stop destination = Stop.builder()
                .stopId(routeId + "-S3")
                .name(to)
                .lat(0.0).lon(0.0)
                .eta(durationMinutes + " min")
                .build();

        return Route.builder()
                .routeId(routeId)
                .stop(origin)
                .stop(midpoint)
                .stop(destination)
                .durationMinutes(durationMinutes)
                .hasDisruption(disrupted)
                .build();
    }

    private List<Route> buildAlternatives(String from, String to, boolean primaryDisrupted) {
        List<Route> alternatives = new ArrayList<>();

        // Alt 1: slightly longer but disruption-free if primary is disrupted
        alternatives.add(buildRoute(from, to, "R-ALT-1", 25, false));

        // Alt 2: express route (fewer stops, longer walk)
        alternatives.add(buildRoute(from, to, "R-ALT-2", 20, false));

        // Alt 3: only offered when primary is disrupted
        if (primaryDisrupted) {
            alternatives.add(buildRoute(from, to, "R-ALT-3", 30, false));
        }

        return alternatives;
    }

    /**
     * Estimates journey time based on average delay across active vehicles.
     * Base time is 15 minutes; each minute of average delay adds proportionally.
     */
    private int estimateJourneyTime(List<VehiclePosition> vehicles) {
        if (vehicles.isEmpty()) return 15;
        double avgDelay = vehicles.stream()
                .mapToInt(VehiclePosition::getDelayMinutes)
                .average()
                .orElse(0);
        return 15 + (int) Math.ceil(avgDelay * 0.5);
    }
}
