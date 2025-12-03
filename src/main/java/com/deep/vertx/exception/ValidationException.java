package com.deep.vertx.exception;

/**
 * Exception thrown when request validation fails.
 */
public class ValidationException extends RuntimeException {
    
    private final int statusCode;

    public ValidationException(String message) {
        super(message);
        this.statusCode = 400;
    }

    public ValidationException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}

