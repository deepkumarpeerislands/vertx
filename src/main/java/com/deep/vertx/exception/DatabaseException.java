package com.deep.vertx.exception;

/**
 * Exception thrown when database operations fail.
 */
public class DatabaseException extends RuntimeException {
    
    private final int statusCode;

    public DatabaseException(String message) {
        super(message);
        this.statusCode = 500; // Internal Server Error
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 500;
    }

    public DatabaseException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}

