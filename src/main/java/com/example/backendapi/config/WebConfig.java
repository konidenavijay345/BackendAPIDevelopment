package com.example.backendapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * Configures Cross-Origin Resource Sharing (CORS) for browser clients.
 *
 * <p>This class demonstrates interface implementation: {@link WebMvcConfigurer} defines
 * extension points and this class supplies the application-specific behavior. CORS lets
 * approved websites call the API while browsers block unapproved origins.</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String[] allowedOriginPatterns;

    /**
     * Converts the comma-separated configuration value into origins understood by Spring.
     * Constructor injection keeps configuration explicit and makes the class testable.
     */
    public WebConfig(@Value("${app.cors.allowed-origin-patterns}") String allowedOriginPatterns) {
        this.allowedOriginPatterns = Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toArray(String[]::new);
    }

    /**
     * Applies CORS rules to API routes and caches browser preflight decisions for one hour.
     *
     * @param registry Spring's registry used to define browser access rules
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOriginPatterns)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
