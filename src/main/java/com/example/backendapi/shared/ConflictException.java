package com.example.backendapi.shared;

/** Signals a duplicate email, SKU, or other conflict with existing state. */
public class ConflictException extends RuntimeException {
    /** Creates a conflict carrying a diagnostic message. */
    public ConflictException(String message) {
        super(message);
    }
}
