package com.example.backendapi.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Persistence abstraction for users.
 *
 * <p>Extending {@link JpaRepository} provides standard CRUD behavior while Spring derives the
 * custom SQL queries from method names. Services depend on this interface instead of MySQL code,
 * reducing boilerplate and improving testability.</p>
 */
public interface UserRepository extends JpaRepository<AppUser, Long> {
    /** Finds an account without treating email casing as a different identity. */
    Optional<AppUser> findByEmailIgnoreCase(String email);
    /** Efficiently checks uniqueness without loading the full user entity. */
    boolean existsByEmailIgnoreCase(String email);
}
