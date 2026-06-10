package com.example.backendapi.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Immutable login input contract. Validation annotations reject malformed requests before they
 * reach authentication logic, giving users immediate and consistent feedback.
 */
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
