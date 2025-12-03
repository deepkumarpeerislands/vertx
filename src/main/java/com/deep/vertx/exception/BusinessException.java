package com.deep.vertx.exception;

/**
 * Exception thrown when a business rule is violated.
 */
public class BusinessException extends RuntimeException {
    
    private final int statusCode;

    public BusinessException(String message) {
        super(message);
        this.statusCode = 400; // Bad Request by default
    }

    public BusinessException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}

