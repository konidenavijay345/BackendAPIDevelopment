package com.example.backendapi.shared;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converts application exceptions into one consistent HTTP error contract.
 * Central handling keeps controllers focused and prevents internal stack traces reaching users.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Maps missing or non-owned resources to HTTP 404. */
    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), request.getRequestURI(), Map.of());
    }

    /** Maps duplicate and database-constraint conflicts to HTTP 409. */
    @ExceptionHandler({ConflictException.class, DataIntegrityViolationException.class})
    ResponseEntity<ApiError> handleConflict(RuntimeException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "The request conflicts with existing data", request.getRequestURI(), Map.of());
    }

    /** Collects field validation failures into a frontend-friendly map. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(fieldError -> fields.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "Request validation failed", request.getRequestURI(), fields);
    }

    /** Maps invalid login credentials to 401 without revealing which credential failed. */
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> handleAuthentication(AuthenticationException exception, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "Invalid email or password", request.getRequestURI(), Map.of());
    }

    /** Logs unexpected failures internally and returns a generic HTTP 500 response. */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unhandled exception for {}", request.getRequestURI(), exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request.getRequestURI(), Map.of());
    }

    /** Constructs the shared error DTO and matching HTTP response. */
    private ResponseEntity<ApiError> error(
            HttpStatus status, String message, String path, Map<String, String> validationErrors
    ) {
        ApiError body = new ApiError(
                Instant.now(), status.value(), status.getReasonPhrase(), message, path, validationErrors
        );
        return ResponseEntity.status(status).body(body);
    }
}
