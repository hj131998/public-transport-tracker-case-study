package com.tracker.service;

import com.tracker.model.Alert;
import com.tracker.model.VehiclePosition;
import com.tracker.model.enums.AlertType;
import com.tracker.model.enums.CrowdingLevel;
import com.tracker.model.enums.Severity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class AlertService {

    private static final int SIGNIFICANT_DELAY_MINUTES = 15;

    /**
     * Evaluates all alert rules against the given vehicle list.
     * Returns deduplicated alerts ordered by severity (HIGH first).
     */
    public List<Alert> evaluate(List<VehiclePosition> vehicles) {
        List<Alert> alerts = new ArrayList<>();

        boolean hasDisruption = vehicles.stream().anyMatch(VehiclePosition::isDisrupted);
        boolean hasHighCrowding = vehicles.stream().anyMatch(v -> v.getCrowding() == CrowdingLevel.HIGH);
        int maxDelay = vehicles.stream().mapToInt(VehiclePosition::getDelayMinutes).max().orElse(0);

        checkDelay(maxDelay).ifPresent(alerts::add);
        checkDisruption(hasDisruption).ifPresent(alerts::add);
        checkCrowding(hasHighCrowding).ifPresent(alerts::add);

        log.debug("Alert evaluation complete: {} alerts generated", alerts.size());
        return alerts;
    }

    /**
     * Overload that also accepts an explicit weather-impact flag,
     * used when the aggregator has weather context from an external source.
     */
    public List<Alert> evaluate(List<VehiclePosition> vehicles, boolean weatherImpact) {
        List<Alert> alerts = new ArrayList<>(evaluate(vehicles));
        checkWeather(weatherImpact).ifPresent(alerts::add);
        return alerts;
    }

    private Optional<Alert> checkDelay(int maxDelayMinutes) {
        if (maxDelayMinutes > SIGNIFICANT_DELAY_MINUTES) {
            return Optional.of(Alert.builder()
                    .type(AlertType.DELAY)
                    .severity(Severity.HIGH)
                    .message("Significant delays - Plan accordingly")
                    .build());
        }
        return Optional.empty();
    }

    private Optional<Alert> checkDisruption(boolean hasDisruption) {
        if (hasDisruption) {
            return Optional.of(Alert.builder()
                    .type(AlertType.DISRUPTION)
                    .severity(Severity.HIGH)
                    .message("Service alert - Check alternative routes")
                    .build());
        }
        return Optional.empty();
    }

    private Optional<Alert> checkCrowding(boolean hasHighCrowding) {
        if (hasHighCrowding) {
            return Optional.of(Alert.builder()
                    .type(AlertType.CROWDING)
                    .severity(Severity.MEDIUM)
                    .message("Vehicle at capacity - Consider next service")
                    .build());
        }
        return Optional.empty();
    }

    private Optional<Alert> checkWeather(boolean weatherImpact) {
        if (weatherImpact) {
            return Optional.of(Alert.builder()
                    .type(AlertType.WEATHER)
                    .severity(Severity.MEDIUM)
                    .message("Weather impact on schedule")
                    .build());
        }
        return Optional.empty();
    }
}
