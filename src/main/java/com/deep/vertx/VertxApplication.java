package com.deep.vertx;


import io.vertx.core.Vertx;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class VertxApplication {

	public static void main(String[] args) {
//		SpringApplication.run(VertxApplication.class, args);
		// Disable Spring's web environment to ensure no Tomcat/Jetty/Netty server from classpath runs.
		new SpringApplicationBuilder(VertxApplication.class)
				.web(WebApplicationType.NONE)
				.run(args);
	}


//	@Bean
//	public ApplicationRunner deployHttpServerVerticle() {
//		return args -> {
//			Vertx vertx = Vertx.vertx();
//			vertx.deployVerticle(new HttpServerVerticle());
//		};
//	}
}
