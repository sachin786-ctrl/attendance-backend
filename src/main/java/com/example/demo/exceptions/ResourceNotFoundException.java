package com.example.demo.exceptions;

// ✅ Jab koi resource DB mein nahi milta tab throw karo (User, Token etc.)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}