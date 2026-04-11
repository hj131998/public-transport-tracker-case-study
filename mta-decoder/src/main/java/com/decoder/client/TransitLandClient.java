package com.decoder.client;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

/**
 * Fetches route/operator data from the Transit.land REST API.
 * No API key required for public feeds.
 * Docs: https://www.transit.land/documentation/
 */
public class TransitLandClient {

    private static final String BASE_URL = "https://transit.land/api/v2/rest";

    private final OkHttpClient http;

    public TransitLandClient(OkHttpClient http) {
        this.http = http;
    }

    /** Returns raw JSON string for NYC subway routes. */
    public String fetchNycRoutes(int limit) throws IOException {
        Request req = new Request.Builder()
                .url(BASE_URL + "/routes?operator_onestop_id=o-dr5r-nyct&limit=" + limit)
                .build();

        try (Response resp = http.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                throw new IOException("Transit.land returned HTTP " + resp.code());
            }
            return resp.body().string();
        }
    }
}
