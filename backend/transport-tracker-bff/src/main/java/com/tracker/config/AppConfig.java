package com.tracker.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {

    /**
     * Controls the data source strategy for transit data.
     *   LIVE   — always call external API first, fallback to cache/mock on failure
     *   CACHED — serve from cache only, never call external API
     *   STALE  — serve stale cache only (for testing degradation)
     *   MOCK   — skip API and cache entirely, always return mock data
     */
    private String dataSourceMode = "LIVE";

    private Cache cache = new Cache();
    private TransitApi transitApi = new TransitApi();
    private Cors cors = new Cors();
    private RateLimit rateLimit = new RateLimit();

    @Data
    public static class Cache {
        private int ttlSeconds = 30;
        private int staleTtlSeconds = 300;
        private int maxSize = 1000;
    }

    @Data
    public static class TransitApi {
        private ApiSource mta = new ApiSource();
        private ApiSource transitLand = new ApiSource();

        @Data
        public static class ApiSource {
            private String baseUrl;
            private String apiKey;
            private int timeoutSeconds = 5;
        }
    }

    @Data
    public static class Cors {
        private String allowedOrigins = "http://localhost:3000";
    }

    @Data
    public static class RateLimit {
        private int requestsPerMinute = 60;
    }
}
