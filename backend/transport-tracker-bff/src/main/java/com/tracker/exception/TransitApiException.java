package com.tracker.exception;

public class TransitApiException extends RuntimeException {

    public TransitApiException(String message) {
        super(message);
    }

    public TransitApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
