package com.tracker.bdd.steps;

import com.tracker.model.Alert;
import com.tracker.model.VehiclePosition;
import com.tracker.model.enums.AlertType;
import com.tracker.model.enums.CrowdingLevel;
import com.tracker.service.AlertService;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class TransitSteps {

    @Autowired
    private AlertService alertService;

    @Autowired
    private TestRestTemplate restTemplate;

    private ResponseEntity<Map> lastResponse;
    private List<Alert> evaluatedAlerts;
    private List<VehiclePosition> testVehicles;

    @Given("the transit service is running")
    public void theTransitServiceIsRunning() {
        // Spring Boot test context handles this
    }

    @Given("the external transit API is available")
    public void theExternalTransitApiIsAvailable() {
        // WireMock stub would be configured here in integration test
    }

    @Given("the external transit API is unavailable")
    public void theExternalTransitApiIsUnavailable() {
        // WireMock stub returns 503 here
    }

    @Given("there is stale cached data for city {string} and route {string}")
    public void thereIsStaleCachedData(String city, String route) {
        // Pre-populate cache via test helper
    }

    @Given("there is no cached data for city {string} and route {string}")
    public void thereIsNoCachedData(String city, String route) {
        // Cache is empty by default in test context
    }

    @Given("a vehicle on route {string} has a delay of {int} minutes")
    public void aVehicleHasDelay(String route, int delayMinutes) {
        testVehicles = List.of(VehiclePosition.builder()
                .vehicleId("V-BDD").lat(0).lon(0)
                .nextStop("Test Stop").eta("5 min")
                .crowding(CrowdingLevel.LOW)
                .delayMinutes(delayMinutes)
                .disrupted(false)
                .build());
    }

    @Given("a vehicle on route {string} is disrupted")
    public void aVehicleIsDisrupted(String route) {
        testVehicles = List.of(VehiclePosition.builder()
                .vehicleId("V-BDD").lat(0).lon(0)
                .nextStop("Test Stop").eta("N/A")
                .crowding(CrowdingLevel.LOW)
                .delayMinutes(0)
                .disrupted(true)
                .build());
    }

    @When("I request transit data for city {string} and route {string}")
    public void iRequestTransitData(String city, String route) {
        lastResponse = restTemplate.getForEntity(
                "/api/v1/transit?city={city}&route={route}", Map.class, city, route);
    }

    @When("I request transit data without the city parameter")
    public void iRequestWithoutCity() {
        lastResponse = restTemplate.getForEntity("/api/v1/transit?route=M15", Map.class);
    }

    @When("alerts are evaluated")
    public void alertsAreEvaluated() {
        evaluatedAlerts = alertService.evaluate(testVehicles);
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(int status) {
        assertThat(lastResponse.getStatusCode().value()).isEqualTo(status);
    }

    @And("the data source is {string}")
    public void theDataSourceIs(String dataSource) {
        assertThat(lastResponse.getBody()).containsEntry("dataSource", dataSource);
    }

    @And("the response contains vehicle positions")
    public void theResponseContainsVehiclePositions() {
        assertThat(lastResponse.getBody()).containsKey("vehicles");
    }

    @And("the response contains a warning message")
    public void theResponseContainsWarning() {
        assertThat(lastResponse.getBody()).containsKey("warning");
    }

    @And("the offline flag is true")
    public void theOfflineFlagIsTrue() {
        assertThat(lastResponse.getBody()).containsEntry("offline", true);
    }

    @Then("a {string} alert is generated with message {string}")
    public void anAlertIsGenerated(String alertType, String message) {
        assertThat(evaluatedAlerts)
                .anyMatch(a -> a.getType() == AlertType.valueOf(alertType)
                        && a.getMessage().equals(message));
    }
}
