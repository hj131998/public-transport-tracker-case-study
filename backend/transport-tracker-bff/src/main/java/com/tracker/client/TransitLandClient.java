package com.tracker.client;

import com.tracker.model.VehiclePosition;
import com.tracker.model.enums.CrowdingLevel;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class TransitLandClient implements TransitDataProvider {

    private final WebClient webClient;

    public TransitLandClient(
            ReactorClientHttpConnector connector,
            @Value("${app.transit-api.transit-land.base-url}") String baseUrl,
            @Value("${app.transit-api.transit-land.api-key}") String apiKey,
            @Value("${app.transit-api.transit-land.timeout-seconds:5}") int timeoutSeconds) {
        this.webClient = WebClient.builder()
                .clientConnector(connector)
                .baseUrl(baseUrl)
                .defaultHeader("apikey", apiKey)
                .build();
    }

    @Override
    public boolean supports(String city) {
        // Transit.land supports all cities as fallback
        return true;
    }

    @Override
    @CircuitBreaker(name = "transitApi")
    @Retry(name = "transitApi")
    public List<VehiclePosition> fetchVehicles(String city, String routeId) {
        log.info("Fetching vehicles from Transit.land city={} route={}", city, routeId);

        Map<String, Object> response = webClient.get()
                .uri("/vehicles?route_id={routeId}", routeId)
                .retrieve()
                .bodyToMono(Map.class)
                .cast(Map.class)
                .map(m -> (Map<String, Object>) m)
                .block(Duration.ofSeconds(5));

        return normalize(response);
    }

    @SuppressWarnings("unchecked")
    private List<VehiclePosition> normalize(Map<String, Object> response) {
        if (response == null) return List.of();
        List<Map<String, Object>> vehicles = (List<Map<String, Object>>) response.getOrDefault("vehicles", List.of());
        return vehicles.stream()
                .map(v -> {
                    Map<String, Object> position = (Map<String, Object>) v.getOrDefault("position", Map.of());
                    return VehiclePosition.builder()
                            .vehicleId(String.valueOf(v.getOrDefault("id", "UNKNOWN")))
                            .lat(toDouble(position.get("lat")))
                            .lon(toDouble(position.get("lon")))
                            .nextStop(String.valueOf(v.getOrDefault("stop_name", "")))
                            .eta(String.valueOf(v.getOrDefault("arrival_time", "N/A")))
                            .crowding(CrowdingLevel.LOW)
                            .delayMinutes(toInt(v.get("delay")))
                            .disrupted(false)
                            .build();
                })
                .toList();
    }

    private double toDouble(Object val) {
        if (val == null) return 0.0;
        return val instanceof Number n ? n.doubleValue() : Double.parseDouble(val.toString());
    }

    private int toInt(Object val) {
        if (val == null) return 0;
        return val instanceof Number n ? n.intValue() : Integer.parseInt(val.toString());
    }
}
