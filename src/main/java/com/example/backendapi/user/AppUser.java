package com.example.backendapi.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * Persistent user entity and Spring Security identity.
 *
 * <p>This class demonstrates encapsulation by keeping fields private and exposing controlled
 * accessors. Implementing {@link UserDetails} is polymorphism: Spring Security can treat this
 * domain object as its standard user abstraction without knowing the concrete class.</p>
 */
@Entity
@Table(name = "users")
public class AppUser implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 60)
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Required by JPA when reconstructing an entity from a database row. */
    protected AppUser() {
    }

    /** Creates a new user with an already-hashed password. Plain passwords never enter the entity. */
    public AppUser(String name, String email, String passwordHash) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    /** Sets the immutable creation timestamp immediately before the first database insert. */
    @jakarta.persistence.PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    /** Returns roles used by Spring Security authorization decisions. */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    /** Returns the BCrypt hash required by Spring's password verifier. */
    @Override
    public String getPassword() {
        return passwordHash;
    }

    /** Uses email as the unique authentication username. */
    @Override
    public String getUsername() {
        return email;
    }

    /** Returns the database identifier. */
    public Long getId() { return id; }
    /** Returns the display name. */
    public String getName() { return name; }
    /** Returns the normalized unique email. */
    public String getEmail() { return email; }
    /** Returns when the account was created. */
    public Instant getCreatedAt() { return createdAt; }
}
