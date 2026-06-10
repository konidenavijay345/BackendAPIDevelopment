package com.example.backendapi.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Immutable registration input contract.
 *
 * <p>The 72-character password maximum matches BCrypt's effective input limit and avoids giving
 * users a false impression that additional characters improve the stored hash.</p>
 */
public record RegisterRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) String password
) {
}
