package com.example.demo.dtos;

// ✅ Login/Register ke baad client ko yahi response milega
// Refresh token cookie mein jayega, response body mein nahi (security best practice)
public record TokenResponse(
        String accessToken,    // JWT access token (short-lived, 1 hour)
        String refreshToken,    // Refresh token (long-lived)
        long expiresIn,        // Seconds mein expiry time
        String tokenType,      // Always "Bearer"
        UserDto user           // Logged in user ki basic info
) {
    // ✅ Static factory method - clean way to create response
    public static TokenResponse of(String accessToken,String refreshToken, long expiresIn, UserDto user) {
        return new TokenResponse(accessToken,refreshToken, expiresIn, "Bearer", user);
    }
}