package com.tracker.exception;

public class RouteNotFoundException extends RuntimeException {

    public RouteNotFoundException(String city, String routeId) {
        super("No route found for city=" + city + " routeId=" + routeId);
    }
}
