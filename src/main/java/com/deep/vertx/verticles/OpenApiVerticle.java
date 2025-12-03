package com.deep.vertx.verticles;

import com.deep.vertx.handler.GlobalFailureHandler;
import com.deep.vertx.handler.UserHandler;
import com.deep.vertx.service.UserService;
import com.deep.vertx.service.UserServiceImpl;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.openapi.RouterBuilder;



/**
 * Builds the OpenAPI router from classpath openapi.yaml, attaches handlers by operationId,
 * and publishes a readiness event on the event bus (address: "router.openapi.ready").
 *
 * The actual Router instance is stored in the in-process RouterRegistry (package-private helper),
 * so other verticles can retrieve the Router by reference (no serialization).
 *
 * This verticle DOES NOT start an HTTP server.
 */

public class OpenApiVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(OpenApiVerticle.class);
    public static final String ROUTER_READY_ADDRESS = "router.openapi.ready";

    private final UserService userService;
    private final UserHandler userHandler;
    private final GlobalFailureHandler globalFailureHandler;

    public OpenApiVerticle() {
        // Initialize service and handlers
        this.userService = new UserServiceImpl();
        this.userHandler = new UserHandler(userService);
        this.globalFailureHandler = new GlobalFailureHandler();
    }

    @Override
    public void start(Promise<Void> startPromise) {
        RouterBuilder.create(vertx, "openapi/openApiSpec.yml")
            .onSuccess(routerBuilder -> {
                // Add body handler first to parse JSON request bodies
                routerBuilder.rootHandler(BodyHandler.create());

                // Add handlers by operationId
                routerBuilder.operation("createUser").handler(userHandler::handleCreateUser);
                routerBuilder.operation("getUser").handler(userHandler::handleGetUser);

                // Build router
                Router router = routerBuilder.createRouter();

                // Add global failure handler to the router
                router.route().failureHandler(globalFailureHandler::handle);

                // Store router in registry (in-process) for direct retrieval by other verticles
                RouterRegistry.setRouter(router);

                // Publish readiness on event bus (service-style)
                // Use publish so multiple consumers could listen; include a minimal payload if needed.
                JsonObject payload = new JsonObject().put("status", "ready");
                vertx.eventBus().publish(ROUTER_READY_ADDRESS, payload, new DeliveryOptions());

                logger.info("OpenAPI router created and published readiness on '" + ROUTER_READY_ADDRESS + "'");

                startPromise.complete();
            })
            .onFailure(throwable -> {
                logger.error("Failed to create RouterBuilder", throwable);
                startPromise.fail(throwable);
            });
    }
}

/**
 * Package-private in-process registry for Router instance.
 *
 * We keep this simple and JVM-local. The event bus is used to publish readiness; the registry
 * provides direct object reference retrieval without serialization.
 *
 * NOTE: This is intentionally package-private and simple for the sample. For production you may
 * want a proper service interface or use Vert.x service proxies.
 */
class RouterRegistry {
    private static volatile Router router;

    static void setRouter(Router r) {
        router = r;
    }

    static Router getRouter() {
        return router;
    }
}
