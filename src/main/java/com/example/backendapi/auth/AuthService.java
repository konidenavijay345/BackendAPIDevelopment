package com.example.backendapi.auth;

import com.example.backendapi.security.JwtService;
import com.example.backendapi.shared.ConflictException;
import com.example.backendapi.user.AppUser;
import com.example.backendapi.user.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Implements account registration and login use cases.
 *
 * <p>This service encapsulates authentication business rules and collaborates with abstractions
 * such as {@link PasswordEncoder} and {@link AuthenticationManager}. That dependency inversion
 * allows companies to replace hashing or authentication providers without rewriting controllers.</p>
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /** Injects required collaborators through the constructor. */
    public AuthService(
            UserRepository userRepository, PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager, JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        AppUser user = userRepository.save(new AppUser(
                request.name().trim(), email, passwordEncoder.encode(request.password())
        ));
        return response(user);
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password())
        );
        return response((AppUser) authentication.getPrincipal());
    }

    /** Builds one consistent response for both registration and login. */
    private AuthResponse response(AppUser user) {
        return new AuthResponse(
                jwtService.generateToken(user), "Bearer", jwtService.expiresInSeconds(),
                new AuthResponse.UserSummary(user.getId(), user.getName(), user.getEmail())
        );
    }

    /** Normalizes email identity so casing and surrounding spaces cannot create duplicate users. */
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
    /**
     * Registers a unique email address, hashes the password, and returns a signed token.
     * The transaction ensures partial accounts are not persisted if the operation fails.
     */
    /**
     * Delegates credential verification to Spring Security and issues a fresh JWT on success.
     */
