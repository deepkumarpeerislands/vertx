package com.deep.vertx.handler;

import com.deep.vertx.dto.Result;
import com.deep.vertx.exception.BusinessException;
import com.deep.vertx.exception.DatabaseException;
import com.deep.vertx.exception.NotFoundException;
import com.deep.vertx.exception.ValidationException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.concurrent.CompletionException;

/**
 * Comprehensive global failure handler that handles all types of exceptions
 * and supports Result<T> pattern.
 * 
 * Handles:
 * - ValidationException (custom)
 * - NotFoundException (custom)
 * - BusinessException (custom)
 * - DatabaseException (custom)
 * - Vert.x ValidationException (from RouterBuilder)
 * - RequestPredicateException (from Vert.x validation)
 * - General Exception
 */
public class GlobalFailureHandler {

    /**
     * Handles failures/errors that occur during request processing.
     *
     * @param rc the routing context containing error information
     */
    public void handle(RoutingContext rc) {
        Throwable failure = rc.failure();
        int statusCode = rc.statusCode() > 0 ? rc.statusCode() : 500;
        String errorMessage = "Unknown error";

        // Handle different exception types
        if (failure != null) {
            ErrorResponse errorResponse = mapExceptionToErrorResponse(failure, statusCode, rc);
            statusCode = errorResponse.getStatusCode();
            errorMessage = errorResponse.getMessage();
        }

        // Create error response JSON
        JsonObject errorBody = createErrorResponse(statusCode, errorMessage, failure);

        // Send response
        rc.response()
                .setStatusCode(statusCode)
                .putHeader("content-type", "application/json")
                .end(errorBody.encode());
    }

    /**
     * Maps exceptions to appropriate error responses.
     *
     * @param throwable the exception
     * @param defaultStatusCode default status code if not found in exception
     * @param rc the routing context (for extracting request body to identify fields)
     * @return ErrorResponse with status code and message
     */
    private ErrorResponse mapExceptionToErrorResponse(Throwable throwable, int defaultStatusCode, RoutingContext rc) {
        // Unwrap CompletionException (exceptions from CompletableFuture are wrapped)
        Throwable actualException = unwrapException(throwable);
        
        // Handle custom exceptions
        if (actualException instanceof com.deep.vertx.exception.ValidationException) {
            com.deep.vertx.exception.ValidationException ex = 
                    (com.deep.vertx.exception.ValidationException) actualException;
            return new ErrorResponse(ex.getStatusCode(), ex.getMessage());
        }

        if (actualException instanceof NotFoundException) {
            NotFoundException ex = (NotFoundException) actualException;
            return new ErrorResponse(ex.getStatusCode(), ex.getMessage());
        }

        if (actualException instanceof BusinessException) {
            BusinessException ex = (BusinessException) actualException;
            return new ErrorResponse(ex.getStatusCode(), ex.getMessage());
        }

        if (actualException instanceof DatabaseException) {
            DatabaseException ex = (DatabaseException) actualException;
            return new ErrorResponse(ex.getStatusCode(), ex.getMessage());
        }

        // Handle Vert.x validation exceptions (from RouterBuilder/OpenAPI validation)
        String exceptionClassName = actualException.getClass().getName();
        String exceptionMessage = actualException.getMessage();
        
        if (exceptionClassName.contains("ValidationException") || 
            exceptionClassName.contains("RequestPredicateException") ||
            exceptionClassName.contains("BodyProcessorException") ||
            (exceptionMessage != null && exceptionMessage.contains("Validation error"))) {
            
            // Parse and format validation error message to be user-friendly
            String userFriendlyMessage = formatValidationErrorMessage(exceptionMessage, rc);
            return new ErrorResponse(400, userFriendlyMessage);
        }

        // Handle general exceptions
        if (actualException instanceof IllegalArgumentException) {
            return new ErrorResponse(400, "Invalid argument: " + actualException.getMessage());
        }

        if (actualException instanceof IllegalStateException) {
            return new ErrorResponse(400, "Invalid state: " + actualException.getMessage());
        }

        // Default: internal server error
        return new ErrorResponse(defaultStatusCode, actualException.getMessage());
    }

    /**
     * Formats validation error messages to be user-friendly.
     * Extracts meaningful information from technical validation errors.
     *
     * @param technicalMessage the technical error message from Vert.x
     * @param rc the routing context (for extracting request body to identify fields)
     * @return user-friendly error message
     */
    private String formatValidationErrorMessage(String technicalMessage, RoutingContext rc) {
        if (technicalMessage == null || technicalMessage.isEmpty()) {
            return "Request validation failed";
        }
        
        // Remove technical prefixes
        String message = technicalMessage
                .replaceAll("\\[Bad Request\\]\\s*", "")
                .replaceAll("Validation error for body application/json:\\s*", "")
                .replaceAll("Validation error:\\s*", "")
                .trim();
        
        // Extract field name from message or request body
        String fieldName = extractFieldName(message, rc);
        
        // Handle type mismatch errors
        if (message.contains("don't match type") || message.contains("does not match type")) {
            String typeError = extractTypeError(message);
            // Always include "Field" prefix, use field name if available, otherwise generic
            if (fieldName != null) {
                return String.format("Field '%s' should be %s", fieldName, typeError);
            }
            return String.format("Field should be %s", typeError);
        }
        
        // Handle missing required fields
        if (message.contains("required") || message.contains("missing")) {
            if (fieldName != null) {
                return String.format("Field '%s' is required", fieldName);
            }
            return "Required field is missing";
        }
        
        // Handle format validation (email, etc.)
        if (message.contains("format") || message.contains("email") || message.contains("pattern")) {
            if (fieldName != null && message.contains("email")) {
                return String.format("Field '%s' must be a valid email address", fieldName);
            }
            if (fieldName != null) {
                return String.format("Field '%s' format is invalid", fieldName);
            }
            return "Invalid field format";
        }
        
        // Handle pattern validation
        if (message.contains("don't match pattern") || message.contains("does not match pattern")) {
            if (fieldName != null) {
                return String.format("Field '%s' does not match required format", fieldName);
            }
            return "Field value does not match required format";
        }
        
        // Handle minimum/maximum validation
        if (message.contains("minimum") || message.contains("maximum")) {
            if (fieldName != null) {
                return String.format("Field '%s' value is out of allowed range", fieldName);
            }
            return "Field value is out of allowed range";
        }
        
        // Generic validation error - try to make it more readable
        return "Request validation failed: " + message;
    }
    
    /**
     * Extracts field name from validation error message or request body.
     *
     * @param message the error message
     * @param rc the routing context (to check request body)
     * @return field name if found, null otherwise
     */
    private String extractFieldName(String message, RoutingContext rc) {
        // Try to extract from request body first (check which field has wrong type)
        if (rc != null) {
            try {
                JsonObject body = rc.getBodyAsJson();
                if (body != null) {
                    // Check each field in the body to find the one with wrong type
                    // Common fields from OpenAPI spec
                    String[] commonFields = {"name", "email", "id", "password", "username"};
                    for (String field : commonFields) {
                        if (body.containsKey(field)) {
                            Object value = body.getValue(field);
                            // If value is not a string but should be (based on error message)
                            if (message.contains("STRING") && !(value instanceof String)) {
                                return field;
                            }
                            // If value is not a number but should be
                            if ((message.contains("NUMBER") || message.contains("INTEGER")) 
                                && !(value instanceof Number)) {
                                return field;
                            }
                            // If value is not a boolean but should be
                            if (message.contains("BOOLEAN") && !(value instanceof Boolean)) {
                                return field;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore - fall back to message parsing
            }
        }
        
        // Try to extract from error message
        String lowerMessage = message.toLowerCase();
        String[] commonFields = {"name", "email", "id", "password", "username"};
        
        for (String field : commonFields) {
            if (lowerMessage.contains(field)) {
                return field;
            }
        }
        
        // Try to extract from patterns like "field 'name'" or "property 'email'"
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("['\"](\\w+)['\"]");
        java.util.regex.Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    /**
     * Extracts type error message from validation error.
     *
     * @param message the error message
     * @return user-friendly type error message (without "Field" prefix)
     */
    private String extractTypeError(String message) {
        String upperMessage = message.toUpperCase();
        if (upperMessage.contains("STRING")) {
            return "a string";
        } else if (upperMessage.contains("NUMBER") || upperMessage.contains("INTEGER")) {
            return "a number";
        } else if (upperMessage.contains("BOOLEAN")) {
            return "a boolean";
        } else if (upperMessage.contains("OBJECT")) {
            return "an object";
        } else if (upperMessage.contains("ARRAY")) {
            return "an array";
        }
        return "of valid type";
    }

    /**
     * Unwraps CompletionException to get the actual exception.
     * CompletableFuture wraps exceptions in CompletionException.
     *
     * @param throwable the exception (may be wrapped)
     * @return the actual exception
     */
    private Throwable unwrapException(Throwable throwable) {
        if (throwable instanceof CompletionException && throwable.getCause() != null) {
            return throwable.getCause();
        }
        return throwable;
    }

    /**
     * Creates a standardized error response JSON object.
     *
     * @param statusCode HTTP status code
     * @param message error message
     * @param throwable the exception (for logging/debugging)
     * @return JsonObject containing error details
     */
    private JsonObject createErrorResponse(int statusCode, String message, Throwable throwable) {
        JsonObject errorBody = new JsonObject()
                .put("status", statusCode)
                .put("message", message != null ? message : "Unknown error");

        // Add error type for better debugging (optional, can be removed in production)
        // Show the actual exception type, not the wrapper
        if (throwable != null) {
            Throwable actualException = unwrapException(throwable);
            errorBody.put("errorType", actualException.getClass().getSimpleName());
        }

        // Add timestamp
        errorBody.put("timestamp", java.time.Instant.now().toString());

        return errorBody;
    }

    /**
     * Helper method to send error response from Result<T>.
     * This allows handlers to use Result.error() and have it handled consistently.
     *
     * @param rc the routing context
     * @param result the Result containing error information
     */
    public static void sendErrorFromResult(RoutingContext rc, Result<?> result) {
        JsonObject errorBody = new JsonObject()
                .put("status", result.getStatus())
                .put("message", result.getMessage() != null ? result.getMessage() : "Unknown error")
                .put("timestamp", java.time.Instant.now().toString());

        rc.response()
                .setStatusCode(result.getStatus())
                .putHeader("content-type", "application/json")
                .end(errorBody.encode());
    }

    /**
     * Inner class to hold error response information.
     */
    private static class ErrorResponse {
        private final int statusCode;
        private final String message;

        ErrorResponse(int statusCode, String message) {
            this.statusCode = statusCode;
            this.message = message;
        }

        int getStatusCode() {
            return statusCode;
        }

        String getMessage() {
            return message;
        }
    }
}

