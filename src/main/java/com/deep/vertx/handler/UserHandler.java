package com.deep.vertx.handler;

import com.deep.vertx.dto.Result;
import com.deep.vertx.service.UserService;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.concurrent.CompletableFuture;

/**
 * Handler for user-related HTTP requests.
 * Delegates business logic to UserService and handles CompletableFuture results.
 */
public class UserHandler {

    private final UserService userService;

    public UserHandler(UserService userService) {
        this.userService = userService;
    }

    /**
     * Handles POST /users request to create a new user.
     *
     * @param rc the routing context
     */
    public void handleCreateUser(RoutingContext rc) {
        // Note: Schema validation (required fields, email format) is handled by OpenAPI spec
        // If validation fails, GlobalFailureHandler will catch it automatically
        
        JsonObject body = rc.getBodyAsJson();
        String name = body.getString("name");
        String email = body.getString("email");

        // Call async service method
        userService.createUser(name, email)
                .whenComplete((result, throwable) -> {
                    // Handle exceptions first - MUST call rc.fail() to trigger GlobalFailureHandler
                    if (throwable != null) {
                        rc.fail(throwable); // This triggers GlobalFailureHandler
                        return;
                    }
                    
                    // Handle the Result from service
                    if (result.isSuccess()) {
                        sendSuccessResponse(rc, result);
                    } else {
                        // For Result errors, we can either:
                        // Option 1: Send error directly (current approach)
                        sendErrorResponse(rc, result);
                        // Option 2: Or throw exception to use GlobalFailureHandler
                        // throw new BusinessException(result.getMessage());
                    }
                });
    }

    /**
     * Handles GET /users/{id} request to retrieve a user by ID.
     *
     * @param rc the routing context
     */
    public void handleGetUser(RoutingContext rc) {
        // Note: Path parameter validation (required, type) is handled by OpenAPI spec
        // If validation fails, GlobalFailureHandler will catch it automatically
        
        String id = rc.pathParam("id");

        // Call async service method
        userService.getUserById(id)
                .whenComplete((result, throwable) -> {
                    // Handle exceptions first - MUST call rc.fail() to trigger GlobalFailureHandler
                    if (throwable != null) {
                        rc.fail(throwable); // This triggers GlobalFailureHandler
                        return;
                    }
                    
                    // Handle the Result from service
                    if (result.isSuccess()) {
                        sendSuccessResponse(rc, result);
                    } else {
                        // For Result errors, we can either:
                        // Option 1: Send error directly (current approach)
                        sendErrorResponse(rc, result);
                        // Option 2: Or throw exception to use GlobalFailureHandler
                        // throw new BusinessException(result.getMessage());
                    }
                });
    }

    /**
     * Sends a successful HTTP response based on Result.
     *
     * @param rc     the routing context
     * @param result the result containing data and status code
     */
    private void sendSuccessResponse(RoutingContext rc, Result<JsonObject> result) {
        JsonObject responseBody = result.getData();
        if (responseBody == null) {
            responseBody = new JsonObject();
        }

        rc.response()
                .setStatusCode(result.getStatus())
                .putHeader("content-type", "application/json")
                .end(responseBody.encode());
    }

    /**
     * Sends an error HTTP response based on Result.
     *
     * @param rc     the routing context
     * @param result the result containing error status and message
     */
    private void sendErrorResponse(RoutingContext rc, Result<JsonObject> result) {
        JsonObject errorBody = new JsonObject()
                .put("status", result.getStatus())
                .put("message", result.getMessage() != null ? result.getMessage() : "Unknown error");

        rc.response()
                .setStatusCode(result.getStatus())
                .putHeader("content-type", "application/json")
                .end(errorBody.encode());
    }
}

