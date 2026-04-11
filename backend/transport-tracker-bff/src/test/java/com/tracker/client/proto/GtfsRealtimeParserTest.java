package com.tracker.client.proto;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedHeader;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.Position;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.VehicleDescriptor;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition.OccupancyStatus;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition.VehicleStopStatus;
import com.tracker.model.enums.CrowdingLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

class GtfsRealtimeParserTest {

    private GtfsRealtimeParser parser;

    @BeforeEach
    void setUp() {
        parser = new GtfsRealtimeParser();
    }

    // ── Feed builder helper ──────────────────────────────────────────────────

    private byte[] buildFeed(String routeId, String vehicleId,
                             float lat, float lon, String stopId,
                             int delaySeconds, OccupancyStatus occupancy,
                             VehicleStopStatus stopStatus) {

        TripDescriptor trip = TripDescriptor.newBuilder()
                .setTripId("TRIP-" + vehicleId)
                .setRouteId(routeId)
                .build();

        VehiclePosition vp = VehiclePosition.newBuilder()
                .setTrip(trip)
                .setPosition(Position.newBuilder().setLatitude(lat).setLongitude(lon).build())
                .setVehicle(VehicleDescriptor.newBuilder().setId(vehicleId).build())
                .setStopId(stopId)
                .setCurrentStatus(stopStatus)
                .setOccupancyStatus(occupancy)
                .build();

        FeedEntity vehicleEntity = FeedEntity.newBuilder()
                .setId("entity-" + vehicleId)
                .setVehicle(vp)
                .build();

        TripUpdate tripUpdate = TripUpdate.newBuilder()
                .setTrip(trip)
                .addStopTimeUpdate(TripUpdate.StopTimeUpdate.newBuilder()
                        .setStopId(stopId)
                        .setDeparture(TripUpdate.StopTimeEvent.newBuilder().setDelay(delaySeconds).build())
                        .build())
                .build();

        FeedEntity tripEntity = FeedEntity.newBuilder()
                .setId("trip-" + vehicleId)
                .setTripUpdate(tripUpdate)
                .build();

        return FeedMessage.newBuilder()
                .setHeader(FeedHeader.newBuilder()
                        .setGtfsRealtimeVersion("2.0")
                        .setTimestamp(System.currentTimeMillis() / 1000)
                        .build())
                .addEntity(vehicleEntity)
                .addEntity(tripEntity)
                .build()
                .toByteArray();
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should parse vehicle position from valid protobuf feed")
    void shouldParseVehiclePosition() {
        byte[] feed = buildFeed("A", "V001", 40.71f, -74.00f, "A27",
                0, OccupancyStatus.MANY_SEATS_AVAILABLE, VehicleStopStatus.IN_TRANSIT_TO);

        List<com.tracker.model.VehiclePosition> result = parser.parse(feed, "A");

        assertThat(result).hasSize(1);
        com.tracker.model.VehiclePosition v = result.get(0);
        assertThat(v.getVehicleId()).isEqualTo("V001");
        assertThat(v.getLat()).isEqualTo(40.71, offset(0.01));
        assertThat(v.getLon()).isEqualTo(-74.00, offset(0.01));
        assertThat(v.getNextStop()).isEqualTo("A27");
    }

    @Test
    @DisplayName("Should filter out vehicles not matching the requested routeId")
    void shouldFilterByRouteId() {
        byte[] feed = buildFeed("A", "V001", 40.71f, -74.00f, "A27",
                0, OccupancyStatus.EMPTY, VehicleStopStatus.IN_TRANSIT_TO);

        List<com.tracker.model.VehiclePosition> result = parser.parse(feed, "C");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should convert delay seconds to delay minutes correctly")
    void shouldConvertDelaySecondsToMinutes() {
        byte[] feed = buildFeed("1", "V002", 40.75f, -73.98f, "101",
                900, OccupancyStatus.EMPTY, VehicleStopStatus.IN_TRANSIT_TO);

        List<com.tracker.model.VehiclePosition> result = parser.parse(feed, "1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDelayMinutes()).isEqualTo(15);
    }

    @Test
    @DisplayName("Should map FULL occupancy to HIGH crowding level")
    void shouldMapFullOccupancyToHighCrowding() {
        byte[] feed = buildFeed("F", "V003", 40.72f, -73.99f, "F15",
                0, OccupancyStatus.FULL, VehicleStopStatus.IN_TRANSIT_TO);

        List<com.tracker.model.VehiclePosition> result = parser.parse(feed, "F");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCrowding()).isEqualTo(CrowdingLevel.HIGH);
    }

    @Test
    @DisplayName("Should map STANDING_ROOM_ONLY to HIGH crowding level")
    void shouldMapStandingRoomToHighCrowding() {
        byte[] feed = buildFeed("N", "V004", 40.73f, -73.97f, "N10",
                0, OccupancyStatus.STANDING_ROOM_ONLY, VehicleStopStatus.IN_TRANSIT_TO);

        assertThat(parser.parse(feed, "N").get(0).getCrowding()).isEqualTo(CrowdingLevel.HIGH);
    }

    @Test
    @DisplayName("Should map MANY_SEATS_AVAILABLE to MEDIUM crowding level")
    void shouldMapManySeatsToMediumCrowding() {
        byte[] feed = buildFeed("L", "V005", 40.74f, -73.96f, "L05",
                0, OccupancyStatus.MANY_SEATS_AVAILABLE, VehicleStopStatus.IN_TRANSIT_TO);

        assertThat(parser.parse(feed, "L").get(0).getCrowding()).isEqualTo(CrowdingLevel.MEDIUM);
    }

    @Test
    @DisplayName("Should mark vehicle as disrupted when stopped with delay > 5 minutes")
    void shouldMarkDisruptedWhenStoppedWithHighDelay() {
        byte[] feed = buildFeed("G", "V006", 40.68f, -73.95f, "G22",
                360, OccupancyStatus.EMPTY, VehicleStopStatus.STOPPED_AT);

        assertThat(parser.parse(feed, "G").get(0).isDisrupted()).isTrue();
    }

    @Test
    @DisplayName("Should NOT mark disrupted when stopped with delay <= 5 minutes")
    void shouldNotMarkDisruptedWithSmallDelay() {
        byte[] feed = buildFeed("G", "V007", 40.68f, -73.95f, "G22",
                240, OccupancyStatus.EMPTY, VehicleStopStatus.STOPPED_AT);

        assertThat(parser.parse(feed, "G").get(0).isDisrupted()).isFalse();
    }

    @Test
    @DisplayName("Should return empty list for empty feed bytes")
    void shouldReturnEmptyForEmptyBytes() {
        assertThat(parser.parse(new byte[0], "A")).isEmpty();
    }

    @Test
    @DisplayName("Should throw RuntimeException for corrupt protobuf bytes")
    void shouldThrowForCorruptBytes() {
        byte[] corrupt = {0x01, 0x02, 0x03, 0x04};
        assertThatThrownBy(() -> parser.parse(corrupt, "A"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("GTFS-RT parse failure");
    }

    @Test
    @DisplayName("Should set eta to On schedule when delay is zero")
    void shouldSetEtaOnScheduleForZeroDelay() {
        byte[] feed = buildFeed("J", "V008", 40.70f, -73.93f, "J12",
                0, OccupancyStatus.EMPTY, VehicleStopStatus.IN_TRANSIT_TO);

        com.tracker.model.VehiclePosition v = parser.parse(feed, "J").get(0);
        assertThat(v.getDelayMinutes()).isZero();
        assertThat(v.getEta()).isEqualTo("On schedule");
    }

    @Test
    @DisplayName("Should set eta with delay minutes when delayed")
    void shouldSetEtaWithDelayWhenDelayed() {
        byte[] feed = buildFeed("Z", "V009", 40.69f, -73.92f, "Z05",
                600, OccupancyStatus.EMPTY, VehicleStopStatus.IN_TRANSIT_TO);

        com.tracker.model.VehiclePosition v = parser.parse(feed, "Z").get(0);
        assertThat(v.getDelayMinutes()).isEqualTo(10);
        assertThat(v.getEta()).contains("10 min late");
    }
}
