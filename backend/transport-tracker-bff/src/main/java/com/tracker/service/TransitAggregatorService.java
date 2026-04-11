package com.tracker.service;

import com.tracker.client.MockDataProvider;
import com.tracker.model.Alert;
import com.tracker.model.RoutePlan;
import com.tracker.model.TransitResponse;
import com.tracker.model.TransitResponse.HateoasLink;
import com.tracker.model.VehiclePosition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

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

    /**
     * Aggregates transit data, alerts, and route plan into a single response.
     * Handles full degradation chain internally via TransitDataService.
     */
    public TransitResponse aggregate(String city, String routeId) {
        MDC.put("city", city);
        MDC.put("routeId", routeId);
        try {
            log.info("Aggregating transit data city={} route={}", city, routeId);

            TransitDataService.FetchResult result = transitDataService.fetchVehicles(city, routeId);

            List<Alert> alerts = result.offline()
                    ? mockDataProvider.getMockAlerts()
                    : alertService.evaluate(result.vehicles());

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
    public List<VehiclePosition> getVehicles(String city, String routeId) {
        return transitDataService.fetchVehicles(city, routeId).vehicles();
    }

    /**
     * Returns only alerts for a given route.
     */
    public List<Alert> getAlerts(String city, String routeId) {
        TransitDataService.FetchResult result = transitDataService.fetchVehicles(city, routeId);
        return result.offline()
                ? mockDataProvider.getMockAlerts()
                : alertService.evaluate(result.vehicles());
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
