package com.example.backendapi.config;

import com.example.backendapi.audit.ApiLoggingFilter;
import com.example.backendapi.audit.ApiLogService;
import com.example.backendapi.security.JwtAuthenticationFilter;
import com.example.backendapi.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.Instant;
import java.util.Map;

/**
 * Central Spring Security configuration for the API.
 *
 * <p>The class uses dependency injection and bean configuration to separate security policy
 * from business code. Stateless JWT security scales well because any application instance can
 * verify a request without sharing an HTTP session.</p>
 */
@Configuration
public class SecurityConfig {

    /**
     * Builds the HTTP security filter chain.
     *
     * <p>Authentication endpoints and health checks are public; every other request requires
     * authentication. CSRF is disabled because credentials are sent explicitly as bearer tokens,
     * not automatically as browser cookies.</p>
     */
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, JwtAuthenticationFilter jwtFilter,
            ApiLogService apiLogService, JwtService jwtService, ObjectMapper objectMapper
    ) throws Exception {
        ApiLoggingFilter apiLoggingFilter = new ApiLoggingFilter(apiLogService, jwtService);
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/index.html", "/app.js", "/styles.css", "/favicon.ico",
                                "/api/v1/auth/**", "/actuator/health"
                        ).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> writeError(
                                response, objectMapper, HttpServletResponse.SC_UNAUTHORIZED,
                                "Unauthorized", "A valid bearer token is required", request.getRequestURI()))
                        .accessDeniedHandler((request, response, exception) -> writeError(
                                response, objectMapper, HttpServletResponse.SC_FORBIDDEN,
                                "Forbidden", "You do not have permission to access this resource",
                                request.getRequestURI())))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(apiLoggingFilter, JwtAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Provides BCrypt password hashing. BCrypt includes a salt and work factor, making stolen
     * password hashes substantially harder to crack than plain or fast hashes.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Exposes Spring's authentication coordinator for the login service.
     */
    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * Writes a stable JSON error contract for failures raised before controllers execute.
     */
    private void writeError(
            HttpServletResponse response, ObjectMapper objectMapper, int status,
            String error, String message, String path
    ) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "timestamp", Instant.now().toString(), "status", status, "error", error,
                "message", message, "path", path
        ));
    }
}
