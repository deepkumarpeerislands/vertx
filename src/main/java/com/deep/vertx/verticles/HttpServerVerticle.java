package com.deep.vertx.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServer;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.ext.web.Router;

/**
 * Responsible for starting the HttpServer. It listens to the event bus address published by OpenApiVerticle
 * and then retrieves the Router from the in-process RouterRegistry and binds the server.
 *
 * This avoids SharedData and uses the event bus for service-style signaling.
 */

public class HttpServerVerticle extends AbstractVerticle {

    /*public void start(Promise<Void> startPromise){
        HttpServer server = vertx.createHttpServer();
        server.requestHandler(router).listen(8080, http -> {
            if (http.succeeded()) {
                System.out.println("HTTP server started on port 8080");
                startPromise.complete();
            } else {
                startPromise.fail(http.cause());
            }
        });
    }*/



    private static final Logger logger = LoggerFactory.getLogger(HttpServerVerticle.class);
    private static final int DEFAULT_PORT = 8080;
    private Promise<Void> startPromise;

    @Override
    public void start(Promise<Void> startPromise) {
        this.startPromise = startPromise;
        // Listen for the router-ready event; if it has already been published, we might still receive it
        // depending on subscription timing; to be resilient, also check the registry immediately.
        vertx.eventBus().consumer(OpenApiVerticle.ROUTER_READY_ADDRESS, this::onRouterReady);

        // Check registry immediately in case OpenApiVerticle already set it before we subscribed
        Router router = RouterRegistry.getRouter();
        if (router != null) {
            bindServer(router, startPromise);
        } else {
            // Wait for the event bus message; keep startPromise pending until server is started.
            // To avoid leaving startPromise unresolved forever in pathological cases, set a timeout.
            vertx.setTimer(30_000L, id -> {
                if (!startPromise.future().isComplete()) {
                    logger.error("Timeout waiting for router readiness event; failing start");
                    startPromise.fail("Timeout waiting for router readiness");
                }
            });
            // startPromise will be completed in bindServer when event arrives
        }
    }

    private void onRouterReady(Message<Object> msg) {
        logger.info("Received router readiness event");
        Router router = RouterRegistry.getRouter();
        if (router == null) {
            logger.error("RouterRegistry has no router even after readiness event");
            if (startPromise != null && !startPromise.future().isComplete()) {
                startPromise.fail("Router not found in registry");
            }
            return;
        }
        // Bind the HTTP server using the original startPromise
        if (startPromise != null && !startPromise.future().isComplete()) {
            bindServer(router, startPromise);
        }
    }

    private void bindServer(Router router, Promise<Void> startPromise) {
        int port = config().getInteger("http.port", DEFAULT_PORT);
        HttpServer server = vertx.createHttpServer();
        server.requestHandler(router).listen(port, ar -> {
            if (ar.succeeded()) {
                logger.info("HTTP server started on port " + port);
                startPromise.tryComplete();
            } else {
                logger.error("Failed to start HTTP server", ar.cause());
                startPromise.tryFail(ar.cause());
            }
        });
    }

}
