package com.decoder;

import com.decoder.client.MtaFeedClient;
import com.decoder.model.Alert;
import com.decoder.model.TripUpdate;
import com.decoder.model.VehiclePosition;
import com.decoder.proto.GtfsProtoDecoder;
import com.decoder.proto.GtfsProtoDecoder.FeedResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.OkHttpClient;

import java.io.File;
import java.time.Instant;
import java.util.Map;

public class Main {

    private static final String OUTPUT_DIR = "mta_feed_output";

    public static void main(String[] args) throws Exception {
        OkHttpClient     http      = new OkHttpClient();
        MtaFeedClient    mtaClient = new MtaFeedClient(http);
        GtfsProtoDecoder decoder   = new GtfsProtoDecoder();
        ObjectMapper     mapper    = new ObjectMapper();

        new File(OUTPUT_DIR).mkdirs();
        System.out.println("=== MTA GTFS-RT Feed Decoder ===");
        System.out.println("Output directory: " + OUTPUT_DIR + "/\n");

        for (Map.Entry<String, String> feed : MtaFeedClient.FEEDS.entrySet()) {
            String feedPath = feed.getKey();
            String lines    = feed.getValue();

            System.out.printf("Fetching %-25s (lines: %-15s) ... ", feedPath, lines);

            try {
                byte[]     bytes  = mtaClient.fetchFeed(feedPath);
                FeedResult result = decoder.decode(bytes);

                ObjectNode root = mapper.createObjectNode();
                root.put("feedPath",     feedPath);
                root.put("lines",        lines);
                root.put("fetchedAtUtc", Instant.now().toString());
                root.put("rawSizeBytes", bytes.length);
                root.put("vehicleCount",    result.vehicles().size());
                root.put("tripUpdateCount", result.tripUpdates().size());
                root.put("alertCount",      result.alerts().size());
                root.set("vehicles",     buildVehicles(mapper, result));
                root.set("tripUpdates",  buildTripUpdates(mapper, result));
                root.set("alerts",       buildAlerts(mapper, result));

                String fileName = feedPath.replace("%2F", "_").replace("/", "_") + ".json";
                File   outFile  = new File(OUTPUT_DIR, fileName);
                mapper.writerWithDefaultPrettyPrinter().writeValue(outFile, root);

                System.out.printf("OK  vehicles=%d  tripUpdates=%d  alerts=%d  -> %s%n",
                        result.vehicles().size(), result.tripUpdates().size(),
                        result.alerts().size(), outFile.getPath());

            } catch (Exception e) {
                System.err.printf("FAILED: %s%n", e.getMessage());
            }
        }

        System.out.println("\nDone. Files saved to: " + new File(OUTPUT_DIR).getAbsolutePath());
    }

    private static ArrayNode buildVehicles(ObjectMapper mapper, FeedResult result) {
        ArrayNode arr = mapper.createArrayNode();
        for (VehiclePosition v : result.vehicles()) {
            ObjectNode n = mapper.createObjectNode();
            n.put("entityId",  v.entityId());
            n.put("routeId",   v.routeId());
            n.put("tripId",    v.tripId());
            n.put("latitude",  v.latitude());
            n.put("longitude", v.longitude());
            n.put("bearing",   v.bearing());
            n.put("status",    v.status());
            n.put("timestamp", v.timestamp());
            arr.add(n);
        }
        return arr;
    }

    private static ArrayNode buildTripUpdates(ObjectMapper mapper, FeedResult result) {
        ArrayNode arr = mapper.createArrayNode();
        for (TripUpdate tu : result.tripUpdates()) {
            ObjectNode n = mapper.createObjectNode();
            n.put("entityId", tu.entityId());
            n.put("tripId",   tu.tripId());
            n.put("routeId",  tu.routeId());
            n.put("startDate", tu.startDate());
            ArrayNode stops = mapper.createArrayNode();
            for (TripUpdate.StopTimeUpdate stu : tu.stopTimeUpdates()) {
                ObjectNode s = mapper.createObjectNode();
                s.put("stopId",          stu.stopId());
                s.put("arrivalTime",     stu.arrivalTime());
                s.put("arrivalDelay",    stu.arrivalDelay());
                s.put("departureTime",   stu.departureTime());
                s.put("departureDelay",  stu.departureDelay());
                stops.add(s);
            }
            n.set("stopTimeUpdates", stops);
            arr.add(n);
        }
        return arr;
    }

    private static ArrayNode buildAlerts(ObjectMapper mapper, FeedResult result) {
        ArrayNode arr = mapper.createArrayNode();
        for (Alert a : result.alerts()) {
            ObjectNode n = mapper.createObjectNode();
            n.put("entityId",    a.entityId());
            n.put("cause",       a.cause());
            n.put("effect",      a.effect());
            n.put("header",      a.header());
            n.put("description", a.description());
            ArrayNode routes = mapper.createArrayNode();
            a.informedRoutes().forEach(routes::add);
            n.set("informedRoutes", routes);
            arr.add(n);
        }
        return arr;
    }
}
