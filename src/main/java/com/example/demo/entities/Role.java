package com.example.demo.entities;

// User roles - Spring Security "ROLE_" prefix automatically add karta hai getAuthorities() mein
public enum Role {
    USER,   // Normal user
    ADMIN,  // Admin privileges
    GUEST   // Limited access
}