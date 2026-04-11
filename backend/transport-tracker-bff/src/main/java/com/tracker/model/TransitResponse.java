package com.tracker.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tracker.model.enums.DataSource;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransitResponse {
    String routeId;
    String city;
    DataSource dataSource;
    int cacheAgeSeconds;
    boolean offline;
    String warning;

    @Singular
    List<VehiclePosition> vehicles;

    @Singular
    List<Alert> alerts;

    RoutePlan routePlan;

    // HATEOAS links
    Map<String, HateoasLink> links;

    @Value
    @Builder
    public static class HateoasLink {
        String href;
    }
}
