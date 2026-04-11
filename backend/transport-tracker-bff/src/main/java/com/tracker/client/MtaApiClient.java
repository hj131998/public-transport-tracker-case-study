package com.tracker.client;

import com.tracker.client.proto.GtfsRealtimeParser;
import com.tracker.client.proto.MtaFeedRegistry;
import com.tracker.model.VehiclePosition;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URI;
import java.time.Duration;
import java.util.List;

/**
 * HTTP client for the MTA GTFS-Realtime API (free endpoint, no API key required).
 *
 * Endpoint pattern:
 *   GET https://api-endpoint.mta.info/Dataservice/mtagtfsfeeds/{feedPath}
 *   Response: application/x-protobuf (binary GTFS-RT FeedMessage)
 *
 * Example:
 *   GET https://api-endpoint.mta.info/Dataservice/mtagtfsfeeds/nyct%2Fgtfs-l
 *
 * Each subway line group has its own feed (see MtaFeedRegistry).
 * The response is raw protobuf bytes — GtfsRealtimeParser handles
 * binary → domain model conversion.
 */
@Slf4j
@Component
public class MtaApiClient implements TransitDataProvider {

    private final WebClient webClient;
    private final MtaFeedRegistry feedRegistry;
    private final GtfsRealtimeParser parser;
    private final int timeoutSeconds;

    public MtaApiClient(
            ReactorClientHttpConnector connector,
            @Value("${app.transit-api.mta.base-url}") String baseUrl,
            @Value("${app.transit-api.mta.timeout-seconds:10}") int timeoutSeconds,
            MtaFeedRegistry feedRegistry,
            GtfsRealtimeParser parser) {

        this.feedRegistry = feedRegistry;
        this.parser = parser;
        this.timeoutSeconds = timeoutSeconds;
        this.webClient = WebClient.builder()
                .clientConnector(connector)
                .baseUrl(baseUrl)
                .defaultHeader("Accept", "application/x-protobuf, application/octet-stream, */*")
                .build();
    }

    @Override
    public boolean supports(String city) {
        return "NYC".equalsIgnoreCase(city);
    }

    /**
     * Fetches the GTFS-RT feed for the route's line group, parses the protobuf
     * binary, and returns only the vehicles matching the requested routeId.
     */
    @Override
    @CircuitBreaker(name = "transitApi")
    @Retry(name = "transitApi")
    public List<VehiclePosition> fetchVehicles(String city, String routeId) {
        // MTA subway feeds only contain subway data — skip non-subway routes
        if (!feedRegistry.isSubwayRoute(routeId)) {
            log.info("Route {} is not a subway route — skipping MTA feed", routeId);
            return List.of();
        }

        String feedPath = feedRegistry.getFeedPath(routeId);
        int feedId = feedRegistry.getFeedId(routeId);

        log.info("Fetching MTA GTFS-RT feed={} feedPath={} route={}", feedId, feedPath, routeId);

        // URI.create() with a pre-encoded path preserves %2F as-is.
        // WebClient's .uri(String) would double-encode %2F to %252F.
        // MTA expects: /Dataservice/mtagtfsfeeds/nyct%2Fgtfs-l
        //URI feedUri = URI.create("/Dataservice/mtagtfsfeeds/" + feedPath);


        byte[] feedBytes;
        try {
            feedBytes = webClient.get()
                    .uri(b -> b.path("/Dataservice/mtagtfsfeeds/" + feedPath).build(true))
                    .accept(MediaType.APPLICATION_OCTET_STREAM)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();
        } catch (WebClientResponseException e) {
            log.error("MTA API HTTP error: status={} route={} feed={}",
                    e.getStatusCode(), routeId, feedId);
            throw new RuntimeException("MTA API returned " + e.getStatusCode() + " for route " + routeId, e);
        }

        // Parse errors are caught here and NOT re-thrown so they do not count
        // as circuit breaker failures — a parse issue is a data quality problem,
        // not an API availability problem. Return empty list so TransitDataService
        // falls through to stale cache rather than tripping the circuit.
        try {
            List<VehiclePosition> vehicles = parser.parse(feedBytes, routeId);
            log.info("MTA feed={} returned {} vehicles for route={}", feedId, vehicles.size(), routeId);
            return vehicles;
        } catch (Exception e) {
            log.error("GTFS-RT parse error for route={} feed={}: {}", routeId, feedId, e.getMessage());
            return List.of();
        }
    }
}
