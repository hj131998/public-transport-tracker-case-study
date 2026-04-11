package com.decoder.proto;

import com.decoder.model.Alert;
import com.decoder.model.TripUpdate;
import com.decoder.model.VehiclePosition;
import com.google.protobuf.ExtensionRegistryLite;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts raw GTFS-RT protobuf bytes into domain model objects.
 * Uses an empty extension registry to silently discard MTA's nyct proto2 extensions.
 */
public class GtfsProtoDecoder {

    private static final ExtensionRegistryLite REGISTRY =
            ExtensionRegistryLite.getEmptyRegistry();

    public record FeedResult(
            List<VehiclePosition> vehicles,
            List<TripUpdate>      tripUpdates,
            List<Alert>           alerts
    ) {}

    public FeedResult decode(byte[] bytes) throws IOException {
        FeedMessage message = FeedMessage.parseFrom(bytes, REGISTRY);

        List<VehiclePosition> vehicles    = new ArrayList<>();
        List<TripUpdate>      tripUpdates = new ArrayList<>();
        List<Alert>           alerts      = new ArrayList<>();

        for (FeedEntity entity : message.getEntityList()) {
            if (entity.hasVehicle())    vehicles.add(decodeVehicle(entity));
            if (entity.hasTripUpdate()) tripUpdates.add(decodeTripUpdate(entity));
            if (entity.hasAlert())      alerts.add(decodeAlert(entity));
        }

        return new FeedResult(vehicles, tripUpdates, alerts);
    }

    private VehiclePosition decodeVehicle(FeedEntity entity) {
        var v = entity.getVehicle();
        return new VehiclePosition(
                entity.getId(),
                v.getTrip().getRouteId(),
                v.getTrip().getTripId(),
                v.getPosition().getLatitude(),
                v.getPosition().getLongitude(),
                v.getPosition().getBearing(),
                v.getCurrentStatus().name(),
                v.getTimestamp()
        );
    }

    private TripUpdate decodeTripUpdate(FeedEntity entity) {
        var tu = entity.getTripUpdate();
        List<TripUpdate.StopTimeUpdate> stops = new ArrayList<>();
        for (var stu : tu.getStopTimeUpdateList()) {
            stops.add(new TripUpdate.StopTimeUpdate(
                    stu.getStopId(),
                    stu.hasArrival()   ? stu.getArrival().getTime()        : 0,
                    stu.hasArrival()   ? stu.getArrival().getDelay()       : 0,
                    stu.hasDeparture() ? stu.getDeparture().getTime()      : 0,
                    stu.hasDeparture() ? stu.getDeparture().getDelay()     : 0
            ));
        }
        return new TripUpdate(
                entity.getId(),
                tu.getTrip().getTripId(),
                tu.getTrip().getRouteId(),
                tu.getTrip().getStartDate(),
                stops
        );
    }

    private Alert decodeAlert(FeedEntity entity) {
        var a = entity.getAlert();
        List<String> informedRoutes = new ArrayList<>();
        for (var ie : a.getInformedEntityList()) {
            if (!ie.getRouteId().isEmpty()) informedRoutes.add(ie.getRouteId());
        }
        return new Alert(
                entity.getId(),
                a.getCause().name(),
                a.getEffect().name(),
                a.hasHeaderText()      && a.getHeaderText().getTranslationCount()      > 0
                        ? a.getHeaderText().getTranslation(0).getText()      : "",
                a.hasDescriptionText() && a.getDescriptionText().getTranslationCount() > 0
                        ? a.getDescriptionText().getTranslation(0).getText() : "",
                informedRoutes
        );
    }
}
