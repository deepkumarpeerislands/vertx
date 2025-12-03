package com.deep.vertx.dto;

/**
 * Generic result wrapper for service responses.
 * Similar to Spring Boot's ResponseEntity but simpler.
 * 
 * @param <T> the type of data being returned
 */
public class Result<T> {
    
    private T data;
    private int status;
    private String message;

    private Result(T data, int status, String message) {
        this.data = data;
        this.status = status;
        this.message = message;
    }

    /**
     * Creates a successful result with data and status code.
     */
    public static <T> Result<T> success(T data, int status) {
        return new Result<>(data, status, null);
    }

    /**
     * Creates a successful result with default 200 status.
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(data, 200, null);
    }

    /**
     * Creates a successful result with status 201 (Created).
     */
    public static <T> Result<T> created(T data) {
        return new Result<>(data, 201, null);
    }

    /**
     * Creates an error result with status code and message.
     */
    public static <T> Result<T> error(int status, String message) {
        return new Result<>(null, status, message);
    }

    /**
     * Creates a bad request error (400).
     */
    public static <T> Result<T> badRequest(String message) {
        return new Result<>(null, 400, message);
    }

    /**
     * Creates a not found error (404).
     */
    public static <T> Result<T> notFound(String message) {
        return new Result<>(null, 404, message);
    }

    /**
     * Creates an internal server error (500).
     */
    public static <T> Result<T> internalError(String message) {
        return new Result<>(null, 500, message);
    }

    public T getData() {
        return data;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccess() {
        return status >= 200 && status < 300;
    }

    public boolean isError() {
        return !isSuccess();
    }
}

