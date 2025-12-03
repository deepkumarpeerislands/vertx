package com.deep.vertx.service;

import com.deep.vertx.dto.Result;
import io.vertx.core.json.JsonObject;

import java.util.concurrent.CompletableFuture;

/**
 * Service interface for user-related business operations.
 * Uses CompletableFuture for async operations and Result wrapper for responses.
 */
public interface UserService {

    /**
     * Creates a new user with the provided name and email.
     *
     * @param name  the user's name
     * @param email the user's email
     * @return CompletableFuture that completes with Result containing the created user data
     */
    CompletableFuture<Result<JsonObject>> createUser(String name, String email);

    /**
     * Retrieves a user by ID.
     *
     * @param id the user ID
     * @return CompletableFuture that completes with Result containing the user data
     */
    CompletableFuture<Result<JsonObject>> getUserById(String id);
}
