package com.decoder;

import com.decoder.client.MtaFeedClient;
import com.decoder.client.TransitLandClient;
import com.decoder.model.VehiclePosition;
import com.decoder.proto.GtfsProtoDecoder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.OkHttpClient;

import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws Exception {
        OkHttpClient http       = new OkHttpClient();
        MtaFeedClient mtaClient = new MtaFeedClient(http);
        TransitLandClient tlClient = new TransitLandClient(http);
        GtfsProtoDecoder decoder = new GtfsProtoDecoder();
        ObjectMapper mapper      = new ObjectMapper();

        // ── MTA GTFS-RT ───────────────────────────────────────────────────────
        System.out.println("=== MTA GTFS-RT Vehicle Positions ===");
        for (Map.Entry<String, String> feed : MtaFeedClient.FEEDS.entrySet()) {
            String feedPath = feed.getKey();
            String lines    = feed.getValue();
            try {
                byte[] bytes = mtaClient.fetchFeed(feedPath);
                List<VehiclePosition> vehicles = decoder.decode(bytes);

                System.out.println("Total Vehicles: " + vehicles.size());
                System.out.printf("feedPath=%-20s lines=%-15s vehicleCount=%d%n",
                        feedPath, lines, vehicles.size());

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

                ObjectNode result = mapper.createObjectNode();
                result.put("feed", feedPath);
                result.put("lines", lines);
                result.put("vehicleCount", vehicles.size());
                result.set("vehicles", vehicleArray);

//                System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
            } catch (Exception e) {
                System.err.printf("Failed feed %s: %s%n", feedPath, e.getMessage());
            }
        }

        /*// ── Transit.land ──────────────────────────────────────────────────────
        System.out.println("\n=== Transit.land Routes (NYC Subway) ===");
        try {
            String json = tlClient.fetchNycRoutes(20);
            Object parsed = mapper.readValue(json, Object.class);
            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsed));
        } catch (Exception e) {
            System.err.println("Transit.land failed: " + e.getMessage());
        }*/
    }
}
