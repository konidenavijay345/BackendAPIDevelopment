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

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

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

    private AuthResponse response(AppUser user) {
        return new AuthResponse(
                jwtService.generateToken(user), "Bearer", jwtService.expiresInSeconds(),
                new AuthResponse.UserSummary(user.getId(), user.getName(), user.getEmail())
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
