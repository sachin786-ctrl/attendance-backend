package com.example.demo.entities;

// ✅ Login provider track karne ke liye - LOCAL, GOOGLE, ya GITHUB
public enum Provider {
    LOCAL,    // Email/password se register kiya
    GOOGLE,   // Google OAuth2 se login kiya
    GITHUB    // GitHub OAuth2 se login kiya
}