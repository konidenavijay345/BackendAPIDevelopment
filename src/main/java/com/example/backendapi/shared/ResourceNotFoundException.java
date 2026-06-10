package com.example.backendapi.shared;

/** Signals that a resource is missing or unavailable to the authenticated user. */
public class ResourceNotFoundException extends RuntimeException {
    /** Creates a not-found failure with a client-safe explanation. */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
