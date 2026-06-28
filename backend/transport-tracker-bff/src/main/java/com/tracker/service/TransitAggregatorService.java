package com.tracker.service;

import com.tracker.client.MockDataProvider;
import com.tracker.client.TransitLandFeedFetcher;
import com.tracker.model.Alert;
import com.tracker.model.RoutePlan;
import com.tracker.model.TransitResponse;
import com.tracker.model.TransitResponse.HateoasLink;
import com.tracker.model.VehiclePosition;
import com.tracker.model.enums.AlertType;
import com.tracker.model.enums.Severity;
import com.tracker.model.enums.TransitMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransitAggregatorService {

    private final TransitDataService transitDataService;
    private final AlertService alertService;
    private final RoutePlannerService routePlannerService;
    private final MockDataProvider mockDataProvider;
    private final TransitLandFeedFetcher feedFetcher;

    /** Backward-compatible overload — defaults to BUS */
    public TransitResponse aggregate(String city, String routeId) {
        return aggregate(city, routeId, TransitMode.BUS);
    }

    /**
     * Aggregates transit data, alerts, and route plan into a single response.
     * Alerts come from two sources:
     *   1. Conditional alerts (AlertService) — based on vehicle delay/disruption/crowding data
     *   2. Agency alerts (TransitLandFeedFetcher) — real service alerts from the transit agency
     */
    public TransitResponse aggregate(String city, String routeId, TransitMode mode) {
        MDC.put("city", city);
        MDC.put("routeId", routeId);
        try {
            log.info("Aggregating transit data city={} route={} mode={}", city, routeId, mode);

            TransitDataService.FetchResult result = transitDataService.fetchVehicles(city, routeId, mode);

            List<Alert> alerts;
            if (result.offline()) {
                alerts = mockDataProvider.getMockAlerts();
            } else {
                // Conditional alerts from vehicle data (delay, disruption, crowding)
                alerts = new ArrayList<>(alertService.evaluate(result.vehicles()));
                // Real agency alerts from Transit.land feed
                alerts.addAll(buildAgencyAlerts(city, routeId));
            }

            RoutePlan routePlan = routePlannerService.plan(city, routeId, result.vehicles());

            return TransitResponse.builder()
                    .city(city)
                    .routeId(routeId)
                    .dataSource(result.dataSource())
                    .offline(result.offline())
                    .warning(result.warning())
                    .vehicles(result.vehicles())
                    .alerts(alerts)
                    .routePlan(routePlan)
                    .links(buildLinks(city, routeId))
                    .build();

        } finally {
            MDC.remove("city");
            MDC.remove("routeId");
        }
    }

    /**
     * Returns only vehicles for a given route — lightweight endpoint.
     */
    public List<VehiclePosition> getVehicles(String city, String routeId, TransitMode mode) {
        return transitDataService.fetchVehicles(city, routeId, mode).vehicles();
    }

    /** Backward-compatible overload — defaults to BUS */
    public List<VehiclePosition> getVehicles(String city, String routeId) {
        return getVehicles(city, routeId, TransitMode.BUS);
    }

    /**
     * Returns only alerts for a given route (conditional + agency).
     */
    public List<Alert> getAlerts(String city, String routeId, TransitMode mode) {
        TransitDataService.FetchResult result = transitDataService.fetchVehicles(city, routeId, mode);
        if (result.offline()) {
            return mockDataProvider.getMockAlerts();
        }
        List<Alert> alerts = new ArrayList<>(alertService.evaluate(result.vehicles()));
        alerts.addAll(buildAgencyAlerts(city, routeId));
        return alerts;
    }

    /** Backward-compatible overload — defaults to BUS */
    public List<Alert> getAlerts(String city, String routeId) {
        return getAlerts(city, routeId, TransitMode.BUS);
    }

    /**
     * Plans a route between two stops using live vehicle data for delay context.
     */
    public RoutePlan planRoute(String city, String from, String to) {
        TransitDataService.FetchResult result = transitDataService.fetchVehicles(city, from);
        return result.offline()
                ? mockDataProvider.getMockRoutePlan(from, to)
                : routePlannerService.plan(from, to, result.vehicles());
    }

    /**
     * Converts real agency alerts from TransitLandFeedFetcher into our Alert model.
     * These are DISRUPTION type since they represent actual service advisories.
     */
    private List<Alert> buildAgencyAlerts(String city, String routeId) {
        List<TransitLandFeedFetcher.AgencyAlert> agencyAlerts = feedFetcher.getAlertsForRoute(city, routeId);
        return agencyAlerts.stream()
                .map(aa -> Alert.builder()
                        .type(AlertType.DISRUPTION)
                        .severity(Severity.MEDIUM)
                        .message(aa.message())
                        .generatedAt(aa.fetchedAt())
                        .build())
                .toList();
    }

    private Map<String, HateoasLink> buildLinks(String city, String routeId) {
        String base = "/api/v1";
        return Map.of(
                "self",         HateoasLink.builder().href(base + "/transit?city=" + city + "&route=" + routeId).build(),
                "vehicles",     HateoasLink.builder().href(base + "/transit/" + routeId + "/vehicles?city=" + city).build(),
                "alerts",       HateoasLink.builder().href(base + "/transit/" + routeId + "/alerts?city=" + city).build(),
                "alternatives", HateoasLink.builder().href(base + "/routes/" + routeId + "/alternatives?city=" + city).build()
        );
    }
}
