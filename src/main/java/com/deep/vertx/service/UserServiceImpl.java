package com.deep.vertx.service;

import com.deep.vertx.dto.Result;
import com.deep.vertx.exception.DatabaseException;
import com.deep.vertx.exception.NotFoundException;
import io.vertx.core.json.JsonObject;

import java.util.concurrent.CompletableFuture;

/**
 * Implementation of UserService containing business logic for user operations.
 * Uses CompletableFuture for async operations.
 */
public class UserServiceImpl implements UserService {

    /**
     * Creates a new user with the provided name and email.
     * In a real application, this would save to a database asynchronously.
     *
     * @param name  the user's name
     * @param email the user's email
     * @return CompletableFuture that completes with Result containing the created user data
     */
    @Override
    public CompletableFuture<Result<JsonObject>> createUser(String name, String email) {
        return CompletableFuture.supplyAsync(() -> {
            // Note: Schema validation (required fields, email format) is handled by OpenAPI spec
            // This service only handles BUSINESS LOGIC
            
            // Business logic: Generate user ID and create user object
            // In a real app, this would be: databaseClient.saveUser(name, email)
            // If database operation fails, it will throw DatabaseException
            // → Exception propagates → Handler's whenComplete() catches it → rc.fail() → GlobalFailureHandler
            
            // Example: Business rule validation (not schema validation)
            // if (userService.emailExists(email)) {
            //     throw new BusinessException("Email already exists");
            // }
            
            String userId = java.util.UUID.randomUUID().toString();
            
            JsonObject userData = new JsonObject()
                    .put("id", userId)
                    .put("name", name)
                    .put("email", email);
            
            // Return success result with status 201 (Created)
            return Result.created(userData);
            
            // Note: Any exception thrown here will:
            // 1. Complete CompletableFuture exceptionally
            // 2. Handler's whenComplete() receives throwable
            // 3. Handler calls rc.fail(throwable)
            // 4. GlobalFailureHandler handles it automatically
        });
    }

    /**
     * Retrieves a user by ID.
     * In a real application, this would fetch from a database asynchronously.
     *
     * @param id the user ID
     * @return CompletableFuture that completes with Result containing the user data
     */
    @Override
    public CompletableFuture<Result<JsonObject>> getUserById(String id) {
        return CompletableFuture.supplyAsync(() -> {
            // Note: Path parameter validation (required, type) is handled by OpenAPI spec
            // This service only handles BUSINESS LOGIC
            
            // Business logic: Fetch user from database
            // In a real app, this would be:
            // User user = userRepository.findById(id)
            //     .orElseThrow(() -> new NotFoundException("User", id));
            if ("notfound".equals(id)) {
                throw new NotFoundException("User", id);
                // → Exception propagates → GlobalFailureHandler returns 404
            }
            // For demo purposes: simulate user lookup
            // In production, replace this with actual database query
            JsonObject userData = new JsonObject()
                    .put("id", id)
                    .put("name", "User-" + id)
                    .put("email", "user" + id + "@example.com");
            
            // Return success result with status 200 (OK)
            return Result.success(userData);
            
            // Note: In real implementation:
            // - If user not found → throw new NotFoundException("User", id)
            // - If database error → throw new DatabaseException("Connection failed")
            // - Any exception thrown will:
            //   1. Complete CompletableFuture exceptionally
            //   2. Handler's whenComplete() receives throwable
            //   3. Handler calls rc.fail(throwable)
            //   4. GlobalFailureHandler handles it automatically
        });
    }
}

