package com.deep.vertx.exception;

/**
 * Exception thrown when a requested resource is not found.
 */
public class NotFoundException extends RuntimeException {
    
    private final int statusCode;

    public NotFoundException(String message) {
        super(message);
        this.statusCode = 404;
    }

    public NotFoundException(String resource, String id) {
        super(String.format("%s with id '%s' not found", resource, id));
        this.statusCode = 404;
    }

    public int getStatusCode() {
        return statusCode;
    }
}

