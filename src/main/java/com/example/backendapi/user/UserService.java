package com.example.backendapi.user;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides user lookup to business code and Spring Security.
 *
 * <p>Implementing {@link UserDetailsService} adapts the application's user model to Spring's
 * authentication contract. One lookup implementation is therefore shared by login and JWT flows.</p>
 */
@Service
public class UserService implements UserDetailsService {

    private final UserRepository repository;

    /** Creates the service with its persistence abstraction. */
    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    /** Loads the user identity required by Spring Security authentication providers. */
    @Override
    /** Finds a user by normalized email inside a read-only transaction. */
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return findByEmail(email);
    }

    @Transactional(readOnly = true)
    public AppUser findByEmail(String email) {
        return repository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new UsernameNotFoundException("User was not found"));
    }
}
