package com.decoder.proto;

import com.decoder.model.VehiclePosition;
import com.google.protobuf.ExtensionRegistryLite;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts raw GTFS-RT protobuf bytes into {@link VehiclePosition} domain objects.
 * Uses an empty extension registry to silently discard MTA's nyct proto2 extensions.
 */
public class GtfsProtoDecoder {

    private static final ExtensionRegistryLite REGISTRY =
            ExtensionRegistryLite.getEmptyRegistry();

    public List<VehiclePosition> decode(byte[] bytes) throws IOException {
        FeedMessage message = FeedMessage.parseFrom(bytes, REGISTRY);

        List<VehiclePosition> result = new ArrayList<>();
        for (FeedEntity entity : message.getEntityList()) {
            if (!entity.hasVehicle()) continue;
            var v = entity.getVehicle();
            result.add(new VehiclePosition(
                    entity.getId(),
                    v.getTrip().getRouteId(),
                    v.getTrip().getTripId(),
                    v.getPosition().getLatitude(),
                    v.getPosition().getLongitude(),
                    v.getPosition().getBearing(),
                    v.getCurrentStatus().name(),
                    v.getTimestamp()
            ));
        }
        return result;
    }
}
