package com.tracker.model;

import com.tracker.model.enums.AlertType;
import com.tracker.model.enums.Severity;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class Alert {
    AlertType type;
    Severity severity;
    String message;
    @Builder.Default
    Instant generatedAt = Instant.now();
}
