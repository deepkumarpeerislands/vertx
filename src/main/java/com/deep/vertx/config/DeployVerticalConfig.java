package com.deep.vertx.config;

import com.deep.vertx.verticles.HttpServerVerticle;
import com.deep.vertx.verticles.OpenApiVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Deploy OpenApiVerticle first; upon success deploy HttpServerVerticle.
 * Keeps lifecycle control inside Spring so main() doesn't start servers.
 */

@Configuration
public class DeployVerticalConfig {

    private static final Logger logger = LoggerFactory.getLogger(DeployVerticalConfig.class);

    @Bean
    public CommandLineRunner deployVerticles(Vertx vertx) {
        return args -> {
            vertx.deployVerticle(new OpenApiVerticle(), openAr -> {
                if (openAr.succeeded()) {
                    logger.info("OpenApiVerticle deployed: " + openAr.result());
                    // Now deploy the HTTP server verticle which will wait for the router-ready event
                    vertx.deployVerticle(new HttpServerVerticle(), serverAr -> {
                        if (serverAr.succeeded()) {
                            logger.info("HttpServerVerticle deployed: " + serverAr.result());
                        } else {
                            logger.error("Failed to deploy HttpServerVerticle", serverAr.cause());
                        }
                    });
                } else {
                    logger.error("Failed to deploy OpenApiVerticle", openAr.cause());
                }
            });
        };
    }
}
