package com.example.backendapi.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP adapter for account registration and login.
 *
 * <p>The controller follows the Single Responsibility Principle: it translates HTTP requests
 * into service calls but does not contain persistence or token-generation logic. This keeps the
 * API contract easy to change and the business logic reusable.</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService service;

    /** Creates the controller with its authentication use-case service. */
    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return service.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }
}
    /**
     * Creates an account and immediately returns a JWT so the user can begin using the API.
     *
     * @param request validated registration data
     * @return token and safe user summary
     */
    /**
     * Verifies credentials and returns a new 10-minute JWT.
     *
     * @param request validated login credentials
     * @return token and safe user summary
     */
