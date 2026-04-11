package com.decoder;

import com.decoder.client.MtaFeedClient;
import com.decoder.model.VehiclePosition;
import com.decoder.proto.GtfsProtoDecoder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.OkHttpClient;

import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class Main {

    private static final String OUTPUT_DIR = "mta_feed_output";

    public static void main(String[] args) throws Exception {
        OkHttpClient    http      = new OkHttpClient();
        MtaFeedClient   mtaClient = new MtaFeedClient(http);
        GtfsProtoDecoder decoder  = new GtfsProtoDecoder();
        ObjectMapper    mapper    = new ObjectMapper();

        // Create output directory
        new File(OUTPUT_DIR).mkdirs();

        System.out.println("=== MTA GTFS-RT Feed Decoder ===");
        System.out.println("Output directory: " + OUTPUT_DIR + "/\n");

        for (Map.Entry<String, String> feed : MtaFeedClient.FEEDS.entrySet()) {
            String feedPath = feed.getKey();
            String lines    = feed.getValue();

            System.out.printf("Fetching %-25s (lines: %s) ... ", feedPath, lines);

            try {
                byte[] bytes = mtaClient.fetchFeed(feedPath);
                List<VehiclePosition> vehicles = decoder.decode(bytes);

                // Build vehicle array
                ArrayNode vehicleArray = mapper.createArrayNode();
                for (VehiclePosition v : vehicles) {
                    ObjectNode node = mapper.createObjectNode();
                    node.put("entityId",  v.entityId());
                    node.put("routeId",   v.routeId());
                    node.put("tripId",    v.tripId());
                    node.put("latitude",  v.latitude());
                    node.put("longitude", v.longitude());
                    node.put("bearing",   v.bearing());
                    node.put("status",    v.status());
                    node.put("timestamp", v.timestamp());
                    vehicleArray.add(node);
                }

                // Build wrapper output
                ObjectNode result = mapper.createObjectNode();
                result.put("feedPath",     feedPath);
                result.put("lines",        lines);
                result.put("fetchedAtUtc", Instant.now().toString());
                result.put("rawSizeBytes", bytes.length);
                result.put("vehicleCount", vehicles.size());
                result.set("vehicles",     vehicleArray);

                // Save to file — one file per feed
                String fileName  = feedPath.replace("%2F", "_").replace("/", "_") + ".json";
                File   outFile   = new File(OUTPUT_DIR, fileName);
                mapper.writerWithDefaultPrettyPrinter().writeValue(outFile, result);

                System.out.printf("OK  %d vehicles -> %s%n", vehicles.size(), outFile.getPath());

            } catch (Exception e) {
                System.err.printf("FAILED: %s%n", e.getMessage());

                // Save error output so every feed has a file
                ObjectNode errorNode = mapper.createObjectNode();
                errorNode.put("feedPath", feedPath);
                errorNode.put("lines",    lines);
                errorNode.put("error",    e.getMessage());
                String fileName = feedPath.replace("%2F", "_").replace("/", "_") + ".json";
                mapper.writerWithDefaultPrettyPrinter()
                      .writeValue(new File(OUTPUT_DIR, fileName), errorNode);
            }
        }

        System.out.println("\nDone. Files saved to: " + new File(OUTPUT_DIR).getAbsolutePath());
    }
}
