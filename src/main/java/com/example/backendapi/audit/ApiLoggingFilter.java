package com.example.backendapi.audit;

import com.example.backendapi.security.JwtService;
import com.example.backendapi.user.AppUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Captures one database audit record after each HTTP request completes. */
public class ApiLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiLoggingFilter.class);
    private static final Pattern PRODUCT_RECORD_PATH = Pattern.compile("^/api/v1/products/(\\d+)$");

    private final ApiLogService apiLogService;
    private final JwtService jwtService;

    public ApiLoggingFilter(ApiLogService apiLogService, JwtService jwtService) {
        this.apiLogService = apiLogService;
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain
    ) throws ServletException, IOException {
        long startedAt = System.nanoTime();
        log.info("REQUEST RECEIVED method={} path={} clientIp={}",
                request.getMethod(), requestPath(request), request.getRemoteAddr());
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            AppUser authenticatedUser = authenticatedUser();
            JwtAuditDetails jwt = jwtAuditDetails(request);
            DbAuditDetails db = dbAuditDetails(request);
            apiLogService.record(
                    authenticatedUser == null ? null : authenticatedUser.getId(),
                    authenticatedUser == null ? jwt.subject() : authenticatedUser.getEmail(),
                    authenticatedUser == null ? null : authenticatedUser.getName(),
                    request.getMethod(), request.getRequestURI(), request.getQueryString(),
                    response.getStatus(), durationMs, request.getRemoteAddr(),
                    request.getHeader("User-Agent"), jwt.present(), jwt.valid(),
                    jwt.expiresAt(), jwt.secondsRemaining(), db.tableName(),
                    db.recordId(), db.operation()
            );
        }
    }

    private String requestPath(HttpServletRequest request) {
        if (request.getQueryString() == null) {
            return request.getRequestURI();
        }
        return request.getRequestURI() + "?" + request.getQueryString();
    }

    private AppUser authenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof AppUser user) {
            return user;
        }
        return null;
    }

    private JwtAuditDetails jwtAuditDetails(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return JwtAuditDetails.notPresent();
        }

        try {
            String token = header.substring(7);
            return new JwtAuditDetails(
                    true, true, jwtService.extractUsername(token),
                    jwtService.extractExpiration(token), jwtService.secondsUntilExpiration(token)
            );
        } catch (RuntimeException ignored) {
            return JwtAuditDetails.invalid();
        }
    }

    private DbAuditDetails dbAuditDetails(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        if (path.equals("/api/v1/products")) {
            return new DbAuditDetails("products", null, operation(method));
        }

        Matcher productMatcher = PRODUCT_RECORD_PATH.matcher(path);
        if (productMatcher.matches()) {
            return new DbAuditDetails("products", Long.valueOf(productMatcher.group(1)), operation(method));
        }

        if (path.startsWith("/api/v1/auth/")) {
            return new DbAuditDetails("users", null, operation(method));
        }
        return new DbAuditDetails(null, null, operation(method));
    }

    private String operation(String method) {
        return switch (method) {
            case "POST" -> "CREATE";
            case "GET" -> "READ";
            case "PUT", "PATCH" -> "UPDATE";
            case "DELETE" -> "DELETE";
            default -> method;
        };
    }

    private record JwtAuditDetails(
            boolean present, boolean valid, String subject, Instant expiresAt, Long secondsRemaining
    ) {
        static JwtAuditDetails notPresent() {
            return new JwtAuditDetails(false, false, null, null, null);
        }

        static JwtAuditDetails invalid() {
            return new JwtAuditDetails(true, false, null, null, null);
        }
    }

    private record DbAuditDetails(String tableName, Long recordId, String operation) {
    }
}
