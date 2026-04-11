package com.decoder.model;

import java.util.List;

public record Alert(
        String entityId,
        String cause,
        String effect,
        String header,
        String description,
        List<String> informedRoutes
) {}
