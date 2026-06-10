package com.example.backendapi.auth;

/**
 * Immutable authentication response sent to clients.
 *
 * <p>A Java record provides encapsulation through final components and generated accessors.
 * Returning a dedicated DTO prevents internal fields such as password hashes from leaking.</p>
 *
 * @param token signed JWT used in the Authorization header
 * @param tokenType standard bearer-token type
 * @param expiresIn token lifetime in seconds
 * @param user non-sensitive account information
 */
public record AuthResponse(
        String token,
        String tokenType,
        long expiresIn,
        UserSummary user
) {
    /** Safe, immutable projection of a user account. */
    public record UserSummary(Long id, String name, String email) {
    }
}
