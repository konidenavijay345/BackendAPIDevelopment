package com.example.backendapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application entry point.
 *
 * <p>{@link SpringBootApplication} combines configuration, component scanning, and
 * auto-configuration. Companies use this convention to start the HTTP server and wire
 * controllers, services, repositories, security, and database integrations consistently.</p>
 */
@SpringBootApplication
public class BackendApiApplication {

    /**
     * Starts Spring Boot and the embedded Tomcat web server.
     *
     * @param args optional command-line arguments passed to Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(BackendApiApplication.class, args);
    }
}
