package com.tracker.controller;

import com.tracker.model.TransitResponse;
import com.tracker.model.enums.DataSource;
import com.tracker.service.TransitAggregatorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransitController.class)
class TransitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransitAggregatorService aggregatorService;

    @Test
    @DisplayName("GET /api/v1/transit returns 200 with transit data")
    void shouldReturn200ForValidRequest() throws Exception {
        TransitResponse response = TransitResponse.builder()
                .city("NYC").routeId("M15")
                .dataSource(DataSource.LIVE)
                .offline(false)
                .vehicles(List.of())
                .alerts(List.of())
                .links(Map.of())
                .build();

        when(aggregatorService.aggregate("NYC", "M15")).thenReturn(response);

        mockMvc.perform(get("/api/v1/transit")
                        .param("city", "NYC")
                        .param("route", "M15")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("NYC"))
                .andExpect(jsonPath("$.routeId").value("M15"))
                .andExpect(jsonPath("$.dataSource").value("LIVE"));
    }

    @Test
    @DisplayName("GET /api/v1/transit returns 400 when city param is missing")
    void shouldReturn400WhenCityMissing() throws Exception {
        mockMvc.perform(get("/api/v1/transit")
                        .param("route", "M15")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/transit returns 400 when route param is missing")
    void shouldReturn400WhenRouteMissing() throws Exception {
        mockMvc.perform(get("/api/v1/transit")
                        .param("city", "NYC")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/health returns 200")
    void shouldReturn200ForHealth() throws Exception {
        mockMvc.perform(get("/api/v1/transit")
                        .param("city", "NYC")
                        .param("route", "M15"))
                .andExpect(status().isOk());
    }
}
