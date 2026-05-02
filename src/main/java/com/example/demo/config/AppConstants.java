package com.example.demo.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;

// ✅ Security configuration ke liye constants
// Ek jagah change karo - puri app mein reflect hoga
public final class AppConstants {

    private AppConstants() {
        // Utility class - instantiate mat karo
    }

    // ✅ Publicly accessible endpoints (no token required)
    public static final String[] AUTH_PUBLIC_URLS = {
            "/api/v1/auth/**",       // Login, Register, Refresh, Logout
            "/api/v1/users/**",      // User management (CRUD)
            "/api/v1/attendance/**", //  Attendance management
            "/v3/api-docs/**",       // OpenAPI docs
            "/swagger-ui.html",      // Swagger UI
            "/swagger-ui/**",        // Swagger resources
            "/oauth2/**",            // OAuth2 redirect URLs
            "/login/oauth2/**"       // OAuth2 callback
    };

    // ✅ Admin-only endpoints
    public static final String[] AUTH_ADMIN_URLS = {
            "/api/v1/users/**"       // User management (CRUD)
    };

    // ✅ Guest role endpoints (currently empty, add as needed)
    public static final String[] AUTH_GUEST_URLS = {
            // e.g. "/api/v1/guest/**"
    };

    // ✅ Role names (Spring Security "ROLE_" prefix automatically add hota hai)
    public static final String ADMIN_ROLE = "ADMIN";
    public static final String GUEST_ROLE = "GUEST";
    public static final String USER_ROLE = "USER";
}