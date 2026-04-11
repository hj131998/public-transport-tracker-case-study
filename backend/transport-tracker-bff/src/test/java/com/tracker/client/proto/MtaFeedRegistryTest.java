package com.tracker.client.proto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class MtaFeedRegistryTest {

    private MtaFeedRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new MtaFeedRegistry();
    }

    @ParameterizedTest(name = "Route {0} should map to feed name {1}")
    @CsvSource({
            "1,  gtfs",
            "2,  gtfs",
            "6,  gtfs",
            "7,  gtfs",
            "A,  gtfs-ace",
            "C,  gtfs-ace",
            "E,  gtfs-ace",
            "B,  gtfs-bdfm",
            "D,  gtfs-bdfm",
            "F,  gtfs-bdfm",
            "M,  gtfs-bdfm",
            "N,  gtfs-nqrw",
            "Q,  gtfs-nqrw",
            "R,  gtfs-nqrw",
            "W,  gtfs-nqrw",
            "L,  gtfs-l",
            "G,  gtfs-g",
            "J,  gtfs-jz",
            "Z,  gtfs-jz",
            "SIR,gtfs-si"
    })
    void shouldMapRouteToCorrectFeedName(String route, String expectedName) {
        assertThat(registry.getFeedName(route.trim())).isEqualTo(expectedName.trim());
    }

    @ParameterizedTest(name = "Route {0} full path should be nyct%2F{1}")
    @CsvSource({
            "A,  nyct%2Fgtfs-ace",
            "1,  nyct%2Fgtfs",
            "L,  nyct%2Fgtfs-l",
            "N,  nyct%2Fgtfs-nqrw"
    })
    void shouldBuildFullFeedPath(String route, String expectedPath) {
        assertThat(registry.getFeedPath(route.trim())).isEqualTo(expectedPath.trim());
    }

    @Test
    @DisplayName("Should be case-insensitive for route lookup")
    void shouldBeCaseInsensitive() {
        assertThat(registry.getFeedPath("a")).isEqualTo(registry.getFeedPath("A"));
        assertThat(registry.getFeedPath("f")).isEqualTo(registry.getFeedPath("F"));
    }

    @Test
    @DisplayName("Should fall back to feed 1 for unknown route")
    void shouldFallBackToFeed1ForUnknownRoute() {
        assertThat(registry.getFeedName("X99")).isEqualTo("gtfs");
        assertThat(registry.getFeedPath("X99")).isEqualTo("nyct%2Fgtfs");
        assertThat(registry.getFeedId("X99")).isEqualTo(1);
    }
}
