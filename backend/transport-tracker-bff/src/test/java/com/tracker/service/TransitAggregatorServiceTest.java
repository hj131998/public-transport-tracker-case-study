package com.tracker.service;

import com.tracker.client.MockDataProvider;
import com.tracker.model.Alert;
import com.tracker.model.TransitResponse;
import com.tracker.model.VehiclePosition;
import com.tracker.model.enums.AlertType;
import com.tracker.model.enums.CrowdingLevel;
import com.tracker.model.enums.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransitAggregatorServiceTest {

    @Mock private TransitDataService transitDataService;
    @Mock private AlertService alertService;
    @Mock private RoutePlannerService routePlannerService;
    @Mock private MockDataProvider mockDataProvider;

    @InjectMocks
    private TransitAggregatorService aggregatorService;

    private List<VehiclePosition> sampleVehicles;

    @BeforeEach
    void setUp() {
        sampleVehicles = List.of(VehiclePosition.builder()
                .vehicleId("V001").lat(40.71).lon(-74.00)
                .nextStop("Times Sq").eta("3 min")
                .crowding(CrowdingLevel.LOW).delayMinutes(2).disrupted(false)
                .build());
    }

    @Test
    @DisplayName("Should return LIVE response when API succeeds")
    void shouldReturnLiveResponse() {
        when(transitDataService.fetchVehicles("NYC", "M15"))
                .thenReturn(new TransitDataService.FetchResult(sampleVehicles, DataSource.LIVE, false, null));
        when(alertService.evaluate(anyList())).thenReturn(List.of());
        when(routePlannerService.plan(anyString(), anyString(), anyList()))
                .thenReturn(null);

        TransitResponse response = aggregatorService.aggregate("NYC", "M15");

        assertThat(response.getDataSource()).isEqualTo(DataSource.LIVE);
        assertThat(response.isOffline()).isFalse();
        assertThat(response.getVehicles()).hasSize(1);
        assertThat(response.getLinks()).containsKey("self");
    }

    @Test
    @DisplayName("Should return STALE response with warning when API fails but cache has stale data")
    void shouldReturnStaleResponse() {
        when(transitDataService.fetchVehicles("NYC", "M15"))
                .thenReturn(new TransitDataService.FetchResult(
                        sampleVehicles, DataSource.STALE, false, "Data may be outdated"));
        when(alertService.evaluate(anyList())).thenReturn(List.of());
        when(routePlannerService.plan(anyString(), anyString(), anyList())).thenReturn(null);

        TransitResponse response = aggregatorService.aggregate("NYC", "M15");

        assertThat(response.getDataSource()).isEqualTo(DataSource.STALE);
        assertThat(response.getWarning()).isEqualTo("Data may be outdated");
    }

    @Test
    @DisplayName("Should return MOCK response with offline=true when all sources fail")
    void shouldReturnMockResponseWhenOffline() {
        List<Alert> mockAlerts = List.of(Alert.builder()
                .type(AlertType.DISRUPTION).message("Offline mode").build());

        when(transitDataService.fetchVehicles("NYC", "M15"))
                .thenReturn(new TransitDataService.FetchResult(
                        sampleVehicles, DataSource.MOCK, true, "Offline mode"));
        when(mockDataProvider.getMockAlerts()).thenReturn(mockAlerts);
        when(routePlannerService.plan(anyString(), anyString(), anyList())).thenReturn(null);

        TransitResponse response = aggregatorService.aggregate("NYC", "M15");

        assertThat(response.isOffline()).isTrue();
        assertThat(response.getDataSource()).isEqualTo(DataSource.MOCK);
        assertThat(response.getAlerts()).hasSize(1);
    }

    @Test
    @DisplayName("Should include HATEOAS links in response")
    void shouldIncludeHateoasLinks() {
        when(transitDataService.fetchVehicles("NYC", "M15"))
                .thenReturn(new TransitDataService.FetchResult(sampleVehicles, DataSource.LIVE, false, null));
        when(alertService.evaluate(anyList())).thenReturn(List.of());
        when(routePlannerService.plan(anyString(), anyString(), anyList())).thenReturn(null);

        TransitResponse response = aggregatorService.aggregate("NYC", "M15");

        assertThat(response.getLinks()).containsKeys("self", "vehicles", "alerts", "alternatives");
    }
}
