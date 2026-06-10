package com.example.backendapi.auth;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresIn,
        UserSummary user
) {
    public record UserSummary(Long id, String name, String email) {
    }
}
