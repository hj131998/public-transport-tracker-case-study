Feature: Public Transport Tracker - Transit Data

  Background:
    Given the transit service is running

  Scenario: Fetch live transit data successfully
    Given the external transit API is available
    When I request transit data for city "NYC" and route "M15"
    Then the response status is 200
    And the response contains vehicle positions

  Scenario: Serve mock data when live API is unavailable in test environment
    Given the external transit API is unavailable
    When I request transit data for city "NYC" and route "M15"
    Then the response status is 200
    And the response contains vehicle positions

  Scenario: Non-subway route gracefully returns data
    Given the external transit API is unavailable
    And there is no cached data for city "NYC" and route "M15"
    When I request transit data for city "NYC" and route "M15"
    Then the response status is 200
    And the response contains vehicle positions

  Scenario: Generate delay alert when delay exceeds 15 minutes
    Given a vehicle on route "M15" has a delay of 20 minutes
    When alerts are evaluated
    Then a "DELAY" alert is generated with message "Significant delays - Plan accordingly"

  Scenario: Generate disruption alert when service is disrupted
    Given a vehicle on route "M15" is disrupted
    When alerts are evaluated
    Then a "DISRUPTION" alert is generated with message "Service alert - Check alternative routes"

  Scenario: Return 400 when required parameters are missing
    When I request transit data without the city parameter
    Then the response status is 400
