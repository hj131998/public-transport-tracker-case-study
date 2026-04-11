package com.decoder.client;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fetches raw GTFS-RT protobuf bytes from MTA's free endpoint.
 * No API key required.
 */
public class MtaFeedClient {

    public static final Map<String, String> FEEDS = new LinkedHashMap<>();

    static {
        FEEDS.put("nyct%2Fgtfs",      "1,2,3,4,5,6,7,S");
        FEEDS.put("nyct%2Fgtfs-ace",  "A,C,E");
        FEEDS.put("nyct%2Fgtfs-bdfm", "B,D,F,M");
        FEEDS.put("nyct%2Fgtfs-nqrw", "N,Q,R,W");
        FEEDS.put("nyct%2Fgtfs-l",    "L");
        FEEDS.put("nyct%2Fgtfs-g",    "G");
        FEEDS.put("nyct%2Fgtfs-jz",   "J,Z");
        FEEDS.put("nyct%2Fgtfs-si",   "SIR");
    }

    private static final String BASE_URL = "https://api-endpoint.mta.info/Dataservice/mtagtfsfeeds/";

    private final OkHttpClient http;

    public MtaFeedClient(OkHttpClient http) {
        this.http = http;
    }

    /** Returns raw protobuf bytes for the given feed path (e.g. {@code nyct%2Fgtfs-l}). */
    public byte[] fetchFeed(String feedPath) throws IOException {
        Request req = new Request.Builder()
                .url(BASE_URL + feedPath)
                .build();

        try (Response resp = http.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("MTA feed " + feedPath + " returned HTTP " + resp.code());
            }
            return resp.body().bytes();
        }
    }
}
